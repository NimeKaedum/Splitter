package com.example.livesplitlike

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.livesplitlike.data.ButtonMappingStore
import com.example.livesplitlike.ui.navigation.NavRoutes
import com.example.livesplitlike.ui.screens.groups.CreateGroupScreen
import com.example.livesplitlike.ui.screens.groups.GroupsListScreen
import com.example.livesplitlike.ui.screens.groups.ViewRunsScreen
import com.example.livesplitlike.ui.screens.help.HelpScreen
import com.example.livesplitlike.ui.screens.settings.SettingsScreen
import com.example.livesplitlike.ui.screens.timer.TimerScreen
import com.example.livesplitlike.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // ViewModels (Hilt)
    private val settingsVm: com.example.livesplitlike.ui.screens.settings.SettingsViewModel by viewModels()
    private val timerVm: com.example.livesplitlike.ui.screens.timer.TimerViewModel by viewModels()

    // cached vars updated from flows to avoid blocking reads inside dispatchKeyEvent
    @Volatile
    private var isAssigningCached: Boolean = false

    @Volatile
    private var mappedKeyCached: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start collecting flows to update cached state used in dispatchKeyEvent
        lifecycleScope.launch {
            // isAssigningFlow: StateFlow<Boolean> (must be provided by SettingsViewModel)
            try {
                settingsVm.isAssigningFlow.collectLatest { assigning ->
                    isAssigningCached = assigning
                }
            } catch (t: Throwable) {
                Timber.e(t, "Error collecting isAssigningFlow")
            }
        }

        lifecycleScope.launch {
            // mappedKeyFlow: StateFlow<Int?> (must be provided by SettingsViewModel)
            try {
                settingsVm.mappedKeyFlow.collectLatest { key ->
                    mappedKeyCached = key
                }
            } catch (t: Throwable) {
                Timber.e(t, "Error collecting mappedKeyFlow")
            }
        }

        // also try to initialize mappedKeyCached from DataStore once (in case VM didn't expose flow yet)
        lifecycleScope.launch {
            try {
                val persisted = ButtonMappingStore.getMapping(applicationContext)
                if (persisted != null && mappedKeyCached == null) {
                    mappedKeyCached = persisted
                }
            } catch (e: Exception) {
                Timber.e(e, "Error reading mapping from DataStore on startup")
            }
        }

        setContent {
            val isDark by settingsVm.isDarkThemeFlow.collectAsState()
            AppTheme(darkTheme = isDark) {
                Surface {
                    AppNavHost(settingsVm = settingsVm, timerVm = timerVm)
                    Log.d("SettingsGm", "MainActivity timerVm hash=${timerVm.hashCode()}")
                }
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // DEBUG: log completo de cualquier key event
        if (event != null) {
            val actionStr = when (event.action) {
                KeyEvent.ACTION_DOWN -> "DOWN"
                KeyEvent.ACTION_UP -> "UP"
                else -> event.action.toString()
            }
            val deviceName = try { event.device?.name ?: "unknown" } catch (t: Throwable) { "deviceError" }
            Log.d("SettingsGm", "GamepadDbg EVENT action=$actionStr keyCode=${event.keyCode} scanCode=${event.scanCode} deviceId=${event.deviceId} deviceName=$deviceName sources=${event.source} event=$event")
        }

        if (event.action == KeyEvent.ACTION_DOWN) {
            val keyCode = event.keyCode
            //Toast.makeText(this, "KeyEvent keyCode=${keyCode} (${keyCodeToName(keyCode)})", Toast.LENGTH_SHORT).show()

            // Mostrar valores cacheados para debug
            Log.d("SettingsGm", "GamepadDbg isAssigningCached=$isAssigningCached mappedKeyCached=$mappedKeyCached")

            if (isAssigningCached) {
                Log.d("SettingsGm", "Entrando a if isAssigningCached")
                // consume event and process mapping asynchronously
                lifecycleScope.launch {
                    try {
                        ButtonMappingStore.saveMapping(applicationContext, keyCode)
                        settingsVm.onAssigningFinished(keyCode)
                        Log.d("SettingsGm", "Assigned button saved and VM notified: keyCode=$keyCode")
                    } catch (e: Exception) {
                        Log.e("SettingsGm", "Error saving mapping to DataStore", e)
                    }
                }
                Log.d("SettingsGm", "GamepadDbg Assigned button pressed -> keyCode=$keyCode")
                return true
            }

            val mapped = mappedKeyCached
            if (mapped != null && mapped == keyCode) {
                try {
                    timerVm.onTimerClicked()
                    Log.d("SettingsGm", "GamepadDbg Mapped button pressed -> timerVm.onTimerClicked() keyCode=$keyCode")
                } catch (e: Exception) {
                    Log.e("SettingsGm", "Error invoking timer action from mapped button press", e)
                }
                return true
            }
        }

        return super.dispatchKeyEvent(event)
    }
}

@Composable
fun AppNavHost(
    settingsVm: com.example.livesplitlike.ui.screens.settings.SettingsViewModel,
    timerVm: com.example.livesplitlike.ui.screens.timer.TimerViewModel
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = NavRoutes.TIMER) {
        composable(NavRoutes.TIMER) {
            TimerScreen(
                vm = timerVm,
                onOpenGroups = { navController.navigate(NavRoutes.GROUPS) },
                onOpenSettings = { navController.navigate(NavRoutes.SETTINGS) },
                navController = navController
            )
        }
        composable(NavRoutes.GROUPS) {
            GroupsListScreen(
                onCreateGroup = { navController.navigate(NavRoutes.CREATE_GROUP) },
                onViewRuns = { id -> navController.navigate("${NavRoutes.VIEW_RUNS}/$id") },
                onSelectGroup = { id ->
                    navController.previousBackStackEntry?.savedStateHandle?.set("selectedGroupId", id)
                    navController.popBackStack()
                }
            )
        }
        composable(NavRoutes.CREATE_GROUP) {
            CreateGroupScreen(
                onCreated = { id ->
                    navController.popBackStack()
                },
                onCancel = { navController.popBackStack() }
            )
        }
        composable(
            route = "${NavRoutes.VIEW_RUNS}/{groupId}",
            arguments = listOf(navArgument("groupId") { type = NavType.LongType })
        ) { backStackEntry ->
            val gid = backStackEntry.arguments?.getLong("groupId") ?: 0L
            ViewRunsScreen(groupId = gid, onBack = { navController.popBackStack() })
        }
        composable(NavRoutes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                vm = settingsVm // recuerda pasar la instancia de settings también
            )
        }
        // dentro del NavHost builder:
        composable(NavRoutes.HELP) {
            HelpScreen(navController = navController)
        }
    }
}
