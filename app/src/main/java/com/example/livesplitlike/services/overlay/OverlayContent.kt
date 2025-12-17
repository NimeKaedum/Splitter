// OverlayContent.kt
package com.example.livesplitlike.services.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.livesplitlike.timer.ComparisonStatus
import com.example.livesplitlike.timer.TimerEngine

@Composable
fun OverlayContent(
    timerEngine: TimerEngine,
    onClick: () -> Unit,
    transparentBackground: Boolean = false
) {
    // Observables desde tu TimerEngine (ajusta nombres si difieren)
    val elapsed by timerEngine.elapsedMillis.collectAsState()
    val current by timerEngine.currentCumulative.collectAsState()
    val diffs by timerEngine.diffs.collectAsState()
    val statuses by timerEngine.statuses.collectAsState() // si tienes estados por split

    // Formateo
    val timerText = timerEngine.formatMillis(elapsed)

    // Último split index (puede ser -1 si no hay splits)
    val lastIndex = current.indexOfLast { it != null }

    // Background
    val bg = if (transparentBackground) Color.Transparent else Color.Black.copy(alpha = 0.8f)

    // Caja principal: siempre visible y clicable
    Box(
        modifier = Modifier
            .wrapContentSize()
            .background(bg)
            .clickable { onClick() } // recibe clicks siempre
            .padding(8.dp)
            .defaultMinSize(minWidth = 80.dp, minHeight = 40.dp), // evita desaparecer
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            // Timer (siempre visible)
            Text(
                text = timerText,
                color = Color.White,
                fontSize = 20.sp,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Mostrar último split/diff si existe, si no mostrar texto guía
            if (lastIndex >= 0) {
                val lastSplitTime = current.getOrNull(lastIndex) ?: 0L
                val diffMillis = diffs.getOrNull(lastIndex)
                val status = statuses.getOrNull(lastIndex) ?: ComparisonStatus.NONE

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Split ${lastIndex + 1}: ${timerEngine.formatMillis(lastSplitTime)}",
                        color = Color.White,
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    val diffText = timerEngine.formatDiffMillis(diffMillis)
                    if (diffText != null) {
                        Text(
                            text = diffText,
                            color = mapStatusToColor(status),
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                // Guía para el usuario cuando no hay splits (evita que la vista desaparezca)
                Text(
                    text = "Tap to start",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
            }
        }
    }
}

// Mapeo de ComparisonStatus a Color (usa los mismos valores que TimerScreen)
@Composable
private fun mapStatusToColor(status: ComparisonStatus): Color {
    return when (status) {
        ComparisonStatus.GOLD -> Color(0xFFFFD700)
        ComparisonStatus.LOSS_LOSING -> Color(0xFFF44336)
        ComparisonStatus.LOSS_GAINING -> Color(0xFFB71C1C)
        ComparisonStatus.GAIN_LOSING -> Color(0xFF2E7D32)
        ComparisonStatus.GAIN_GAINING -> Color(0xFF4CAF50)
        ComparisonStatus.NONE -> Color.Gray
    }
}