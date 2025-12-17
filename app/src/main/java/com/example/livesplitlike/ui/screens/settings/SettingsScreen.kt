package com.example.livesplitlike.ui.screens.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil3.compose.AsyncImage
import com.example.livesplitlike.data.keyCodeToName
import com.example.livesplitlike.services.overlay.OverlayService
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import kotlin.jvm.java
import androidx.compose.material3.Button
import androidx.compose.material3.Switch

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    vm: com.example.livesplitlike.ui.screens.settings.SettingsViewModel
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val userEmail by vm.userEmail.collectAsState()
    val photoUrl by vm.photoUrl.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val message by vm.message.collectAsState()
    val defaultWebClientId = stringResource(com.example.livesplitlike.R.string.default_web_client_id)

    // Configura GoogleSignInClient
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        // Reemplaza este resource por el que genera Firebase: R.string.default_web_client_id
        .requestIdToken(defaultWebClientId) // <--- ya no usa stringResource()
        .requestEmail()
        .build()

    val googleSignInClient = GoogleSignIn.getClient(context, gso)

    // Launcher para el intent de Google Sign-In
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                val idToken = account?.idToken
                // Pasa el idToken al ViewModel para autenticar en Firebase
                vm.signInWithGoogleIdToken(idToken)
            } catch (e: Exception) {
                // Manejo de error simple (puedes mejorar con Snackbar)
            }
        } else {
            // cancelado o fallido
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Ajustes", style = MaterialTheme.typography.titleLarge)
                Button(onClick = onBack) { Text("Regresar") }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Encabezado cuenta: foto + logout (visible si hay sesión)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (userEmail != null) {
                    if (photoUrl != null) {
                        AsyncImage(
                            model = photoUrl,
                            contentDescription = "Foto de perfil",
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Avatar",
                            modifier = Modifier.size(56.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Vinculado como:")
                        Text(text = userEmail ?: "", style = MaterialTheme.typography.bodyMedium)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Botón de cerrar sesión (cierra GoogleSignInClient y el ViewModel)
                    Button(onClick = {
                        googleSignInClient.signOut().addOnCompleteListener {
                            vm.signOut()
                        }
                    }) {
                        Icon(Icons.Default.Logout, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cerrar sesión")
                    }
                } else {
                    // Si NO hay sesión, muestra botón Google Sign-In
                    Button(
                        onClick = {
                            val signInIntent = googleSignInClient.signInIntent
                            launcher.launch(signInIntent)
                        },
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Iniciar con Google")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Sincronización (Subir / Descargar)
            Text(text = "Sincronización", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { vm.uploadAllData() },
                    enabled = userEmail != null && !isLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Subir datos a nube")
                }

                Button(
                    onClick = { vm.downloadAllData() },
                    enabled = userEmail != null && !isLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Descargar datos desde nube")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
            }

            message?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }

            AssignButtonSection(vm)
            SettingsOverlayControls()
            AccessibilitySetupHint()
        }
    }
}

@Composable
fun AssignButtonSection(vm: com.example.livesplitlike.ui.screens.settings.SettingsViewModel) {
    val isAssigning by vm.isAssigningFlow.collectAsState()
    val mappedKey by vm.mappedKeyFlow.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = {
                Log.d("SettingsGm", "startAssigning() called from SettingsScreen")
                vm.startAssigning()
                },
            enabled = !isAssigning,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = if (isAssigning) "Presiona el botón en el control..." else "Asignar botón de Xbox")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = mappedKey?.let { "Asignado: ${keyCodeToName(it)}" }
                ?: "No hay botón asignado",
        )
    }
}

@Composable
fun SettingsOverlayControls() {
    val context = LocalContext.current
    val activity = context as? Activity
    var overlayActive by remember { mutableStateOf(OverlayService.isOverlayActive(context)) }

    var overlayTransparent by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        overlayActive = OverlayService.isOverlayActive(context)
    }

    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        // Al volver del permiso, comprobamos y arrancamos si está concedido
        if (Settings.canDrawOverlays(context)) {
            startOverlayService(context, overlayTransparent)
            activity?.moveTaskToBack(true)
        } else {
            // Mostrar mensaje al usuario (Snackbar/Toast)
        }
    }

    Row {
        Text("Overlay transparente")
        Spacer(modifier = Modifier.width(8.dp))
        Switch(checked = overlayTransparent, onCheckedChange = { overlayTransparent = it })
    }

    Spacer(modifier = Modifier.height(12.dp))

    Button(onClick = {
        if (Settings.canDrawOverlays(context)) {
            startOverlayService(context, overlayTransparent)
            activity?.moveTaskToBack(true)
        } else {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            overlayPermissionLauncher.launch(intent)
        }
    }) {
        Text("Abrir overlay (minimizar app)")
    }

    Button(
        onClick = {
            // Detener el servicio que muestra el overlay
            val intent = Intent(context, OverlayService::class.java)
            context.stopService(intent)
            // actualizar UI inmediatamente
            overlayActive = false
        },
        enabled = overlayActive
    ) {
        Text(text = "Eliminar overlay")
    }

}

@Composable
fun AccessibilitySetupHint() {
    val context = LocalContext.current
    Column {
        Text("Para que el mando controle el timer aunque el juego esté en primer plano, habilita el servicio de accesibilidad.")
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            // abrir pantalla de accesibilidad
            val intent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
            context.startActivity(intent)
        }) {
            Text("Abrir ajustes de accesibilidad")
        }
    }
}
private fun startOverlayService(context: Context, transparent: Boolean) {
    val svc = Intent(context, com.example.livesplitlike.services.overlay.OverlayService::class.java).apply {
        putExtra("transparent", transparent)
    }
    ContextCompat.startForegroundService(context, svc)
}

