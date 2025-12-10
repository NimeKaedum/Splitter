package com.example.livesplitlike.ui.screens.timer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues

@Composable
fun TimerScreen(viewModel: TimerViewModel = hiltViewModel()) {
    val elapsed = viewModel.elapsedMillis.collectAsState()
    val items = viewModel.comparisonItemsFlow.collectAsState(initial = emptyList())
    val isRunning = viewModel.isRunning.collectAsState()
    val isPaused = viewModel.isPaused.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(WindowInsets.systemBars.asPaddingValues()) // respeta status/navigation bars
    ) {
        Spacer(modifier = Modifier.height(8.dp)) // espacio superior extra

        // Area clicable que cubre Splits + Cronometro
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clickable(
                    interactionSource = MutableInteractionSource(),
                    indication = null
                ) { viewModel.onTimerClicked() }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Splits (arriba)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    items(items.value) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.name,
                                color = Color.White,
                                modifier = Modifier.weight(0.5f)
                            )
                            val diffText = viewModel.formatDiffMillis(item.diffMillis)
                            if (diffText != null) {
                                val color = when (item.status) {
                                    ComparisonStatus.GOLD -> Color(0xFFFFD700)
                                    ComparisonStatus.LOSS_LOSING -> Color(0xFFF44336)   // rojo
                                    ComparisonStatus.LOSS_GAINING -> Color(0xFF3B0B0B)  // rojo oscuro
                                    ComparisonStatus.GAIN_LOSING -> Color(0xFF0C330B)   // verde oscuro
                                    ComparisonStatus.GAIN_GAINING -> Color(0xFF4CAF50)  // verde
                                    ComparisonStatus.NONE -> Color.Gray
                                }
                                Text(text = diffText, color = color, modifier = Modifier.weight(0.25f))
                            } else {
                                Spacer(modifier = Modifier.weight(0.25f))
                            }
                            val current = item.currentMillis
                            val best = item.bestMillis
                            if (current != null) {
                                Text(text = viewModel.formatMillis(current), color = Color.White, modifier = Modifier.weight(0.25f))
                            } else {
                                Text(text = viewModel.formatMillis(best), color = Color.Gray, modifier = Modifier.weight(0.25f))
                            }
                        }
                        Divider(color = Color.DarkGray)
                    }
                }

                // Cronómetro (debajo de splits, dentro del área clicable)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(Color.DarkGray),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = viewModel.formatMillis(elapsed.value),
                        fontSize = 36.sp,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp)) // espacio entre area clicable y botones

        // Botones (fuera del área clicable)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = { viewModel.onPauseToggle() }, enabled = isRunning.value) {
                Text(if (isPaused.value) "Resume" else "Pause")
            }
            Button(onClick = { viewModel.onReset() }) {
                Text("Reset")
            }
        }

        Spacer(modifier = Modifier.height(8.dp)) // espacio inferior extra
    }
}