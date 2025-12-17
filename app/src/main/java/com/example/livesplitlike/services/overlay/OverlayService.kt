package com.example.livesplitlike.services.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.savedstate.SavedStateRegistry
import com.example.livesplitlike.R
import com.example.livesplitlike.timer.TimerEngine
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import android.content.Context
import android.content.IntentFilter
import androidx.core.content.edit
import android.view.KeyEvent
import androidx.lifecycle.lifecycleScope
import com.example.livesplitlike.AppActions
import com.example.livesplitlike.data.ButtonMappingStore
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OverlayService : LifecycleService() {

    @Inject lateinit var timerEngine: TimerEngine

    private val TAG = "OverlayService"
    private lateinit var windowManager: WindowManager
    private var composeView: ComposeView? = null

    // ViewModelStore para el overlay (limpiamos en onDestroy)
    private val vmStore = ViewModelStore()

    // SavedState owner referencia para limpiar/avanzar lifecycle
    private var savedStateOwner: OverlaySavedStateOwner? = null

    // cached mapping observed from DataStore
    private var mappedKeyCached: Int? = null

    // Job/flag opcional para saber si ya estamos observando (no obligatorio)
    private var observingMapping = false

    // Clase interna que crea un SavedStateRegistryOwner correctamente inicializado
    private class OverlaySavedStateOwner : SavedStateRegistryOwner {
        private val _lifecycle = LifecycleRegistry(this).apply {
            currentState = Lifecycle.State.INITIALIZED
        }
        private val controller: SavedStateRegistryController

        init {
            controller = SavedStateRegistryController.create(this)
            controller.performRestore(null)
        }

        override val lifecycle: Lifecycle get() = _lifecycle
        override val savedStateRegistry = controller.savedStateRegistry

        fun moveToStarted() {
            _lifecycle.currentState = Lifecycle.State.STARTED
        }

        fun moveToDestroyed() {
            _lifecycle.currentState = Lifecycle.State.DESTROYED
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // Observa el mapping guardado en DataStore para mantener mappedKeyCached actualizado
        if (!observingMapping) {
            observingMapping = true
            lifecycleScope.launch(Dispatchers.Main) {
                try {
                    // observeMapping devuelve Flow<Int?> (ver ButtonMappingStore)
                    ButtonMappingStore.observeMapping(applicationContext).collectLatest { value ->
                        mappedKeyCached = value
                        Log.d(TAG, "OverlayService mapping updated mappedKeyCached=$mappedKeyCached")
                    }
                } catch (t: Throwable) {
                    Log.e(TAG, "Error observing mapping in OverlayService", t)
                }
            }

            // También intenta leer el valor actual una vez (por si no hay emisiones inmediatas)
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val persisted = ButtonMappingStore.getMapping(applicationContext)
                    if (persisted != null) {
                        mappedKeyCached = persisted
                        Log.d(TAG, "OverlayService loaded persisted mapping=$persisted")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading mapping from DataStore on startup", e)
                }
            }
        }

        if (!Settings.canDrawOverlays(this)) {
            Log.e(TAG, "No permission to draw overlays. Stopping service.")
            stopSelf()
            return
        }

        try {
            startForegroundIfNeeded()
        } catch (e: Exception) {
            Log.e(TAG, "startForeground failed", e)
            stopSelf()
            return
        }

        val filter = IntentFilter(AppActions.ACTION_TIMER_CLICK)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // API 33+: especificar explícitamente exportado/no-exportado
            registerReceiver(timerClickReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            Log.d(TAG, "timerClickReceiver registered (RECEIVER_NOT_EXPORTED)")
        } else {
            // versiones antiguas: registro normal
            registerReceiver(timerClickReceiver, filter)
            Log.d(TAG, "timerClickReceiver registered (legacy)")
        }

        Log.d(TAG, "timerClickReceiver registered")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        val isTransparent = intent?.getBooleanExtra("transparent", false) ?: false

        if (composeView == null) {
            Handler(Looper.getMainLooper()).post {
                try {
                    addOverlayView(isTransparent)
                    (lifecycle as? LifecycleRegistry)?.currentState = Lifecycle.State.STARTED
                } catch (e: Exception) {
                    Log.e(TAG, "Error adding overlay view", e)
                    stopSelf()
                }
            }
        }
        setOverlayActive(this, true)
        return START_STICKY
    }

    private fun startForegroundIfNeeded() {
        val channelId = "overlay_channel"
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Overlay", NotificationManager.IMPORTANCE_LOW)
            nm?.createNotificationChannel(channel)
        }
        val notif: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Overlay activo")
            .setContentText("El overlay del timer está activo")
            .setSmallIcon(R.mipmap.ic_timer)
            .setOngoing(true)
            .build()

        startForeground(101, notif)
    }

    companion object {
        private const val PREFS_NAME = "overlay_prefs"
        private const val KEY_ACTIVE = "overlay_active"

        fun setOverlayActive(context: Context, active: Boolean) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit {
                    putBoolean(KEY_ACTIVE, active)
                }
        }

        fun isOverlayActive(context: Context): Boolean {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_ACTIVE, false)
        }
    }

    // dentro de OverlayService (propiedades de clase)
    private val timerClickReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "timerClickReceiver onReceive action=${intent?.action}")
            if (intent?.action == AppActions.ACTION_TIMER_CLICK) {
                try {
                    // Asegúrate de que timerEngine está inicializado (log)
                    Log.d(TAG, "Invoking timerEngine.onTimerClicked() from receiver; timerEngine=${timerEngine}")
                    timerEngine.onTimerClicked()
                    Log.d(TAG, "timerEngine.onTimerClicked() invoked successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Error invoking timer action from broadcast", e)
                }
            }
        }
    }
    private fun addOverlayView(transparent: Boolean) {
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            WindowManager.LayoutParams.TYPE_PHONE

        // Layout params: esquina superior derecha, clicable y no bloqueante
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 16
            y = 16
        }


        // Crear y asignar savedStateOwner antes de setContent
        savedStateOwner = OverlaySavedStateOwner()

        composeView = ComposeView(this).apply {
            // Lifecycle y ViewModelStore
            this.setViewTreeLifecycleOwner(this@OverlayService)
            this.setViewTreeViewModelStoreOwner(object : ViewModelStoreOwner {
                override val viewModelStore: ViewModelStore = vmStore
            })

            // Registrar SavedStateOwner para que Compose y rememberSaveable funcionen
            this.setViewTreeSavedStateRegistryOwner(savedStateOwner)

            setContent {
                OverlayContent(
                    timerEngine = timerEngine,
                    onClick = { timerEngine.onTimerClicked() },
                    transparentBackground = transparent
                )
            }

            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            // Registrar KeyListener para gamepad/teclas
//            setOnKeyListener { _, keyCode, event ->
//                if (event.action == KeyEvent.ACTION_DOWN) {
//                    // Si hay mapping y coincide, invocar la acción del timer
//                    val mapped = mappedKeyCached
//                    if (mapped != null && mapped == keyCode) {
//                        try {
//                            timerEngine.onTimerClicked()
//                            Log.d(TAG, "OverlayService: mapped button pressed -> timerEngine.onTimerClicked() keyCode=$keyCode")
//                        } catch (e: Exception) {
//                            Log.e(TAG, "OverlayService: error invoking timer action", e)
//                        }
//                        true
//                    } else {
//                        // No es la tecla mapeada: no consumir para permitir otras acciones
//                        false
//                    }
//                } else {
//                    false
//                }
//            }
        }

        // Añadir la vista
        windowManager.addView(composeView, params)
        Log.i(TAG, "Overlay view added")

        // Avanzar lifecycle del savedStateOwner ahora que la vista está añadida
        Handler(Looper.getMainLooper()).post {
            try {
                savedStateOwner?.moveToStarted()
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo avanzar lifecycle del SavedStateOwner", e)
            }
        }

        // Forzar foco en el hilo principal tras añadir la vista
        Handler(Looper.getMainLooper()).post {
            try {
                composeView?.requestFocus()
            } catch (e: Exception) {
                Log.w(TAG, "Could not request focus for overlay composeView", e)
            }
        }
    }

    override fun onDestroy() {
        try {
            composeView?.let { windowManager.removeView(it) }
        } catch (e: Exception) {
            Log.w(TAG, "Error removing overlay view", e)
        }

        try {
            vmStore.clear()
        } catch (e: Exception) {
            Log.w(TAG, "Error clearing ViewModelStore", e)
        }

        // destruir savedStateOwner lifecycle
        try {
            savedStateOwner?.moveToDestroyed()
        } catch (e: Exception) {
            Log.w(TAG, "Error destroying savedStateOwner", e)
        }
        setOverlayActive(this, false)
        try { unregisterReceiver(timerClickReceiver); Log.d(TAG, "timerClickReceiver unregistered") } catch (e: Exception) { /*...*/ }
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return super.onBind(intent)
    }
}