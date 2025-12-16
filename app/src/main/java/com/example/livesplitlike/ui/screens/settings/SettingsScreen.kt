package com.example.livesplitlike.ui.screens.settings

import android.app.Activity
import android.content.Intent
import android.util.Log
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
import coil3.compose.AsyncImage
import com.example.livesplitlike.utils.keyCodeToName
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import timber.log.Timber

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
    val defaultWebClientId = context.getString(com.example.livesplitlike.R.string.default_web_client_id)

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
            text = mappedKey?.let { "Asignado: ${com.example.livesplitlike.utils.keyCodeToName(it)}" }
                ?: "No hay botón asignado",
        )
    }
}