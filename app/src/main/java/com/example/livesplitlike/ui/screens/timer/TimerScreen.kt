package com.example.livesplitlike.ui.screens.timer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun TimerScreen(viewModel: TimerViewModel = hiltViewModel()) {
    val elapsed = viewModel.elapsedMillis.collectAsState()
    val splits = viewModel.splitsFlow.collectAsState(initial = emptyList())
    val isPaused = viewModel.isPaused.collectAsState()
    val isRunning = viewModel.isRunning.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(Color.DarkGray),
            contentAlignment = Alignment.Center
        ) {
            Text(
                modifier = Modifier
                    .clickable { viewModel.onTimerClicked() }
                    .padding(8.dp),
                text = viewModel.formatMillis(elapsed.value),
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(onClick = { viewModel.onPauseToggle() }, enabled = isRunning.value) {
                Text(if (isPaused.value) "Resume" else "Pause")
            }
            Button(onClick = { viewModel.onReset() }) {
                Text("Reset")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Splits", style = MaterialTheme.typography.titleMedium)

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(splits.value) { split ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = split.name)
                    Text(text = if (split.timeFromStartMillis >= 0) viewModel.formatMillis(split.timeFromStartMillis) else "--")
                }
                Divider()
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(text = "(Toca el cronómetro para: start -> split -> ... -> finish; usa Pause/Reset abajo)")
    }
}