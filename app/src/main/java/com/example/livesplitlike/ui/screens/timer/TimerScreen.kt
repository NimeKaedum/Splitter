package com.example.livesplitlike.ui.screens.timer

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.livesplitlike.timer.ComparisonStatus
import com.example.livesplitlike.ui.navigation.NavRoutes
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.text.font.FontWeight

private fun statusToColor(status: ComparisonStatus): androidx.compose.ui.graphics.Color {
    return when (status) {
        ComparisonStatus.GOLD -> androidx.compose.ui.graphics.Color(0xFFFFD700)
        ComparisonStatus.LOSS_LOSING -> androidx.compose.ui.graphics.Color(0xFFF44336)
        ComparisonStatus.LOSS_GAINING -> androidx.compose.ui.graphics.Color(0xFFB71C1C)
        ComparisonStatus.GAIN_LOSING -> androidx.compose.ui.graphics.Color(0xFF2E7D32)
        ComparisonStatus.GAIN_GAINING -> androidx.compose.ui.graphics.Color(0xFF4CAF50)
        ComparisonStatus.NONE -> androidx.compose.ui.graphics.Color.Gray
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(
    vm: com.example.livesplitlike.ui.screens.timer.TimerViewModel, // recibe la instancia desde MainActivity/AppNavHost
    navController: NavController,
    onOpenGroups: () -> Unit,
    onOpenSettings: () -> Unit
) {
    // Estado del ViewModel (sin cambios de lógica)
    val elapsed by vm.elapsedMillis.collectAsState()
    val items by vm.comparisonItemsFlow.collectAsState(initial = emptyList())
    val isRunning by vm.isRunning.collectAsState()
    val isPaused by vm.isPaused.collectAsState()
    val templates by vm.templates.collectAsState()
    val selectedGroupName by vm.selectedGroupName.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val interactionSource = remember { MutableInteractionSource() }

    // Observe selection result from Groups screen via savedStateHandle
    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collectLatest { entry ->
            val saved = entry.savedStateHandle.get<Long?>("selectedGroupId")
            if (saved != null) {
                if (saved > 0L) {
                    vm.selectGroup(saved)
                } else {
                    scope.launch { snackbarHostState.showSnackbar("Grupo inválido seleccionado") }
                }
                entry.savedStateHandle.remove<Long>("selectedGroupId")
            }
        }
    }

    val colors = MaterialTheme.colorScheme

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = colors.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)           // <- volver al comportamiento original
                .background(colors.background)
                .padding(horizontal = 12.dp)
        ){
            Spacer(modifier = Modifier.height(12.dp))

            // Header: titulo grupo + iconos (Help, Settings)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedGroupName.ifBlank { "Sin grupo" },
                    color = colors.onBackground,
                    style = MaterialTheme.typography.titleMedium
                )

                Row {
                    IconButton(
                        onClick = { navController.navigate(NavRoutes.HELP) },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Help,
                            contentDescription = "Ayuda"
                        )
                    }

                    IconButton(
                        onClick = { onOpenSettings() },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Configuración"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Clickable area que engloba Splits + Timer (IMPORTANTE: vm.onTimerClicked() se llama aquí)
            val clickableEnabled = templates.isNotEmpty()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clickable(
                        enabled = clickableEnabled,
                        interactionSource = interactionSource,
                        indication = null
                    ) {
                        if (!clickableEnabled) {
                            scope.launch { snackbarHostState.showSnackbar("Selecciona o crea un grupo con splits primero") }
                        } else {
                            vm.onTimerClicked()
                        }
                    }
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Splits list (scrollable)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 8.dp)
                    ) {
                        if (items.isEmpty()) {
                            item {
                                Text(
                                    text = "No hay splits",
                                    color = colors.onBackground.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        } else {
                            itemsIndexed(items) { _, item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Nombre
                                    Text(
                                        text = item.name,
                                        color = colors.onBackground,
                                        modifier = Modifier.weight(0.45f)
                                    )

                                    // Diff (acumulado)
                                    val diffText = vm.formatDiffMillis(item.diffMillis)
                                    Box(modifier = Modifier.weight(0.25f), contentAlignment = Alignment.CenterStart) {
                                        if (diffText != null) {
                                            Text(text = diffText, color = statusToColor(item.status))
                                        } else {
                                            Spacer(modifier = Modifier.height(1.dp))
                                        }
                                    }

                                    // Current / Best
                                    Box(modifier = Modifier.weight(0.30f), contentAlignment = Alignment.CenterEnd) {
                                        val current = item.currentMillis
                                        val best = item.bestMillis
                                        if (current != null) {
                                            Text(text = vm.formatMillis(current), color = colors.onBackground)
                                        } else {
                                            Text(text = vm.formatMillis(best), color = colors.onBackground.copy(alpha = 0.6f))
                                        }
                                    }
                                }
                                Divider(color = colors.surfaceVariant)
                            }
                        }
                    }

                    // Timer area (debajo de splits) — sigue dentro del clickable
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .background(colors.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = vm.formatMillis(elapsed),
                            fontSize = 75.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Botones inferiores: rectángulos grandes, juntos, ocupando todo el ancho.
            // Importante: están FUERA del Box clickeable (no afectamos vm.onTimerClicked() )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Reset (replay icon)
                ElevatedButton(
                    onClick = { vm.onReset() },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                ) {
                    Icon(imageVector = Icons.Filled.Replay, contentDescription = "Reset")
                }

                // Pause / Resume (icono dinámico)
                ElevatedButton(
                    onClick = { vm.onPauseToggle() },
                    enabled = isRunning,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                ) {
                    if (isPaused) {
                        Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = "Resume")
                    } else {
                        Icon(imageVector = Icons.Filled.Pause, contentDescription = "Pause")
                    }
                }

                // Grupos (menu icon) - abre groups
                ElevatedButton(
                    onClick = { onOpenGroups() },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                ) {
                    Icon(imageVector = Icons.Filled.Menu, contentDescription = "Grupos")
                }
            }
        }
    }

    // Debug: confirmar hash de la instancia usada
    LaunchedEffect(Unit) {
        Log.d("SettingsGm", "TimerScreen using vm hash=${vm.hashCode()}")
    }
}
