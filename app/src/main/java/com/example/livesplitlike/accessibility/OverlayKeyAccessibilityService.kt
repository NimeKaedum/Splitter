package com.example.livesplitlike.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import com.example.livesplitlike.AppActions
import com.example.livesplitlike.data.ButtonMappingStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.*
import android.view.accessibility.AccessibilityEvent

class OverlayKeyAccessibilityService : AccessibilityService() {
    private val TAG = "OverlayKeyA11y"

    // Coroutine scope for collecting DataStore flows
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    // Cached mapping
    @Volatile
    private var mappedKeyCached: Int? = null

    override fun onServiceConnected() {
        super.onServiceConnected()

        // Configurar serviceInfo para recibir key events
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK // Correct
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
            notificationTimeout = 100
        }
        serviceInfo = info
        Log.i(TAG, "Accessibility service connected")

        // Observar mapping desde DataStore y mantener cache en memoria
        scope.launch {
            try {
                ButtonMappingStore.observeMapping(applicationContext).collectLatest { value ->
                    // observeMapping devuelve Flow<Int?>; puede ser null
                    mappedKeyCached = value
                    Log.d(TAG, "OverlayKeyA11y mapping updated mappedKeyCached=$mappedKeyCached")
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Error observing mapping in AccessibilityService", t)
            }
        }

        // También leer el valor actual una vez (opcional)
        scope.launch {
            try {
                val persisted = ButtonMappingStore.getMapping(applicationContext)
                if (persisted != null) {
                    mappedKeyCached = persisted
                    Log.d(TAG, "OverlayKeyA11y loaded persisted mapping=$persisted")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading mapping from DataStore on startup", e)
            }
        }
    }

    override fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent?) {
        // No se usa para key events en este caso
    }

    override fun onInterrupt() {
        // No-op
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        // Recibir eventos hardware incluso si la app no está en foco
        if (event.action == KeyEvent.ACTION_DOWN) {
            val keyCode = event.keyCode
            Log.d(TAG, "onKeyEvent keyCode=$keyCode")

            val mapped = mappedKeyCached
            if (mapped != null && mapped == keyCode) {
                try {
                    // Enviar broadcast para que la app ejecute la acción del timer
                    val intent = Intent(AppActions.ACTION_TIMER_CLICK).apply {
                        `package` = applicationContext.packageName // asegura entrega solo a tu app
                    }
                    // usar sendBroadcast en hilo principal
                    Handler(Looper.getMainLooper()).post {
                        try {
                            sendBroadcast(intent)
                            Log.d(TAG, "OverlayKeyA11y broadcast sent ACTION_TIMER_CLICK")
                        } catch (t: Throwable) {
                            Log.e(TAG, "Error sending broadcast from AccessibilityService", t)
                        }
                    }
                } catch (t: Throwable) {
                    Log.e(TAG, "Error handling mapped key in AccessibilityService", t)
                }
                // Consumir el evento para que no llegue a la app/juego si así lo deseas
                return true
            }
        }
        // No consumir otros eventos
        return super.onKeyEvent(event)
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}