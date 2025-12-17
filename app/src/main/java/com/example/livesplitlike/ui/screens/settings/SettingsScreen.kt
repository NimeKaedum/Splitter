package com.example.livesplitlike.ui.screens.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
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
import kotlinx.coroutines.launch
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll


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

    // Configura GoogleSignInClient (lógica intacta)
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(defaultWebClientId)
        .requestEmail()
        .build()

    val googleSignInClient = GoogleSignIn.getClient(context, gso)

    // Launcher para el intent de Google Sign-In (lógica intacta)
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                val idToken = account?.idToken
                vm.signInWithGoogleIdToken(idToken)
            } catch (e: Exception) {
                // no se toca la lógica de error aquí
            }
        }
    }

    val colors = MaterialTheme.colorScheme

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()             // padding seguro top/bottom
                .padding(horizontal = 16.dp)
                .background(colors.background)
                .padding(top = 12.dp, bottom = 12.dp)
                .verticalScroll(rememberScrollState()) // <-- añade scroll vertical
        ) {
            // Top bar simple: título + back icon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                    Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Atrás")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Ajustes",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- Cuenta (Card) ---
            Card(modifier = Modifier.fillMaxWidth(),     elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (userEmail != null) {
                        // Avatar
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
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Vinculado como", style = MaterialTheme.typography.bodySmall)
                            Text(text = userEmail ?: "", style = MaterialTheme.typography.bodyMedium)
                        }

                        // Logout botón compacto (no cambia la lógica)
                        IconButton(onClick = {
                            googleSignInClient.signOut().addOnCompleteListener {
                                vm.signOut()
                            }
                        }) {
                            Icon(imageVector = Icons.Default.Logout, contentDescription = "Cerrar sesión")
                        }
                    } else {
                        // Si NO hay sesión, botón grande para Google SignIn
                        ElevatedButton(
                            onClick = {
                                val signInIntent = googleSignInClient.signInIntent
                                launcher.launch(signInIntent)
                            },
                            enabled = !isLoading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Person, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Iniciar sesión con Google")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mensaje de estado o progreso
            if (isLoading) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Sincronizando...", style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            message?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 6.dp))
            }

            // --- Sección: Sincronización ---
            SectionTitle(title = "Sincronización")
            Divider()
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { vm.uploadAllData() },
                    enabled = userEmail != null && !isLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Subir a la nube")
                }

                OutlinedButton(
                    onClick = { vm.downloadAllData() },
                    enabled = userEmail != null && !isLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Descargar")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- Sección: Asignar botón ---
            SectionTitle(title = "Asignación de botón")
            Divider()
            AssignButtonSection(vm = vm)

            // --- Sección: Overlay / Accesibilidad ---
            Spacer(modifier = Modifier.height(8.dp))
            SectionTitle(title = "Overlay y accesibilidad")
            Divider()
            SettingsOverlayControls()

            Spacer(modifier = Modifier.height(12.dp))

            // dentro de SettingsScreen composable (usa el vm que ya pasas)
            val isDarkTheme by vm.isDarkThemeFlow.collectAsState()

            // sección rápida en el layout:
            SectionTitle(title = "Apariencia")
            Divider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Tema oscuro", style = MaterialTheme.typography.bodyMedium)
                    Text("Alterna entre tema claro y oscuro", style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = isDarkTheme,
                    onCheckedChange = { checked -> vm.setDarkTheme(checked) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- Sección: Ayuda / Accesibilidad ---
            Divider()
            AccessibilitySetupHint()

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    )
}

@Composable
fun AssignButtonSection(vm: com.example.livesplitlike.ui.screens.settings.SettingsViewModel) {
    val isAssigning by vm.isAssigningFlow.collectAsState()
    val mappedKey by vm.mappedKeyFlow.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        // Botón ocupa todo el ancho
        Button(
            onClick = {
                vm.startAssigning()
            },
            enabled = !isAssigning,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = if (isAssigning) "Presiona el botón en el control..." else "Asignar botón de Xbox")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = mappedKey?.let { "Asignado: ${keyCodeToName(it)}" } ?: "No hay botón asignado",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun SettingsOverlayControls() {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    var overlayActive by remember { mutableStateOf(OverlayService.isOverlayActive(context)) }
    var overlayTransparent by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        overlayActive = OverlayService.isOverlayActive(context)
    }

    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (Settings.canDrawOverlays(context)) {
            startOverlayService(context, overlayTransparent)
            activity?.moveTaskToBack(true)
            overlayActive = true
        } else {
            // no se toca la lógica
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        // Switch row (full width, title + switch trailing)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Overlay transparente", style = MaterialTheme.typography.bodyMedium)
                Text("Hace el overlay parcialmente transparente", style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = overlayTransparent, onCheckedChange = { overlayTransparent = it })
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Abrir overlay (minimizar app)
        Button(
            onClick = {
                if (Settings.canDrawOverlays(context)) {
                    startOverlayService(context, overlayTransparent)
                    activity?.moveTaskToBack(true)
                    overlayActive = true
                } else {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    overlayPermissionLauncher.launch(intent)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Abrir overlay (minimizar app)")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Eliminar overlay
        OutlinedButton(
            onClick = {
                val intent = Intent(context, OverlayService::class.java)
                context.stopService(intent)
                overlayActive = false
            },
            enabled = overlayActive,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Eliminar overlay")
        }
    }
}

@Composable
fun AccessibilitySetupHint() {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Text("Para que el mando controle el timer aunque el juego esté en primer plano, habilita el servicio de accesibilidad.")
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                val intent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
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
