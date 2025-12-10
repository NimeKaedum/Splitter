package com.example.livesplitlike

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.livesplitlike.ui.navigation.NavRoutes
import com.example.livesplitlike.ui.screens.groups.CreateGroupScreen
import com.example.livesplitlike.ui.screens.groups.GroupsListScreen
import com.example.livesplitlike.ui.screens.groups.ViewRunsScreen
import com.example.livesplitlike.ui.screens.timer.TimerScreen
import com.example.livesplitlike.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                Surface {
                    AppNavHost()
                }
            }
        }
    }
}

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    // dentro de AppNavHost() en MainActivity (reemplaza o integra con tu NavHost existente)
    NavHost(navController = navController, startDestination = NavRoutes.TIMER) {
        composable(NavRoutes.TIMER) {
            TimerScreen(
                onOpenGroups = { navController.navigate(NavRoutes.GROUPS) },
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
                    // after creation, go back to groups list (it will refresh automatically)
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
    }
}