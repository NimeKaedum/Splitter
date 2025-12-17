package com.example.livesplitlike.ui.screens.timer

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.livesplitlike.timer.ComparisonStatus
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


private fun statusToColor(status: ComparisonStatus): Color {
    return when (status) {
        ComparisonStatus.GOLD -> Color(0xFFFFD700)
        ComparisonStatus.LOSS_LOSING -> Color(0xFFF44336)
        ComparisonStatus.LOSS_GAINING -> Color(0xFFB71C1C)
        ComparisonStatus.GAIN_LOSING -> Color(0xFF2E7D32)
        ComparisonStatus.GAIN_GAINING -> Color(0xFF4CAF50)
        ComparisonStatus.NONE -> Color.Gray
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
    // Usar la única instancia "vm" en toda la pantalla
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

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = Color.Black
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Black)
                .padding(horizontal = 12.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Header: show selected group name (not id) and button to open groups
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = selectedGroupName.ifBlank { "Sin grupo" }, color = Color.White)
                Row {
                    Button(onClick = { onOpenGroups() }) { Text("H") }
                    Button(onClick = { onOpenSettings() }) { Text("C") }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Clickable area covering Splits + Timer (enabled only if there are templates)
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
                                    color = Color.Gray,
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
                                    // Name
                                    Text(
                                        text = item.name,
                                        color = Color.White,
                                        modifier = Modifier.weight(0.45f)
                                    )

                                    // Diff (acumulado)
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
                                            Text(text = vm.formatMillis(current), color = Color.White)
                                        } else {
                                            Text(text = vm.formatMillis(best), color = Color.Gray)
                                        }
                                    }
                                }
                                Divider(color = Color.DarkGray)
                            }
                        }
                    }

                    // Timer area (below splits)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .background(Color.DarkGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = vm.formatMillis(elapsed),
                            fontSize = 36.sp,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Buttons row (outside clickable area)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = { vm.onPauseToggle() }, enabled = isRunning) {
                    Text(if (isPaused) "Resume" else "Pause")
                }
                Button(onClick = { vm.onReset() }) { Text("Reset") }

                Button(onClick = { onOpenGroups() }) { Text("Grupos") }

            }
        }
    }

    // Debug: confirmar hash de la instancia usada
    LaunchedEffect(Unit) {
        Log.d("SettingsGm", "TimerScreen using vm hash=${vm.hashCode()}")
    }
}
