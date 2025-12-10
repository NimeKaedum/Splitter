package com.example.livesplitlike.ui.screens.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.ui.platform.LocalContext


private fun shareOnWhatsApp(context: Context, text: String) {
    val pm: PackageManager = context.packageManager
    val whatsappInstalled = try {
        pm.getPackageInfo("com.whatsapp", PackageManager.GET_ACTIVITIES)
        true
    } catch (_: Exception) { false }

    if (whatsappInstalled) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            setPackage("com.whatsapp")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            // fallback to generic share
            val generic = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            context.startActivity(Intent.createChooser(generic, "Compartir"))
        }
    } else {
        val generic = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(generic, "Compartir"))
    }
}

/**
 * Vista de Runs del grupo.
 *
 * - Cabecera grande con el nombre del grupo.
 * - Tabla sin bordes: columnas = splits (+ columna "Compartir"), filas = PB, Best Possible Time, runs...
 * - Scroll horizontal y vertical para muchos splits / runs.
 * - Todas las celdas tienen el mismo ancho fijo para mantener la rejilla.
 */

@Composable
fun ViewRunsScreen(
    groupId: Long,
    onBack: () -> Unit,
    vm: ViewRunsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val groupName = vm.groupName.collectAsState().value
    val templates = vm.templates.collectAsState().value
    val bestCum = vm.bestCumulative.collectAsState().value
    val splitBest = vm.splitBestSegments.collectAsState().value
    val theoretical = vm.theoreticalBestCumulative.collectAsState().value
    val runs = vm.runsWithTimes.collectAsState().value
    val scope = rememberCoroutineScope()

    LaunchedEffect(groupId) {
        vm.loadGroupRuns(groupId)
    }

    // Fixed cell width; adjust if you want smaller/larger cells
    val cellWidth = 96.dp
    val headerHeight = 56.dp
    val smallFont = 12.sp
    val normalFont = 14.sp

    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            // Top: group name large
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(text = groupName, color = Color.White, fontSize = 24.sp, modifier = Modifier.weight(1f))
                Button(onClick = { onBack() }) { Text("Regresar") }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Table area: horizontal scroll for many splits, vertical scroll for many runs
            // We compose a header row and a scrollable body.
            Box(modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
            ) {
                // Horizontal scroll state for columns
                val hScroll = rememberScrollState()
                // Vertical scroll state for rows
                val vScroll = rememberScrollState()

                Column(modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(hScroll)
                ) {
                    // Header row: first column "Runs", then split names, then "Compartir"
                    Row(modifier = Modifier
                        .height(headerHeight)
                        .fillMaxWidth()
                    ) {
                        // Runs header
                        Box(modifier = Modifier.width(cellWidth).padding(4.dp), contentAlignment = Alignment.Center) {
                            Text(text = "Runs", color = Color.White, fontSize = normalFont)
                        }
                        // Split headers
                        templates.forEach { tpl ->
                            Box(modifier = Modifier.width(cellWidth).padding(4.dp), contentAlignment = Alignment.Center) {
                                Text(text = tpl.name, color = Color.White, fontSize = smallFont)
                            }
                        }
                        // Share column header
                        Box(modifier = Modifier.width(cellWidth).padding(4.dp), contentAlignment = Alignment.Center) {
                            Text(text = "Compartir", color = Color.White, fontSize = smallFont)
                        }
                    }

                    Divider(color = Color.DarkGray)

                    // Body: vertical scrollable list of rows
                    Column(modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(vScroll)
                    ) {
                        // First row: PB (best cumulative) in green
                        Row(modifier = Modifier.height(headerHeight).fillMaxWidth()) {
                            Box(modifier = Modifier.width(cellWidth).padding(4.dp), contentAlignment = Alignment.CenterStart) {
                                Text(text = "PB", color = Color.Green, fontSize = normalFont)
                            }
                            for (i in templates.indices) {
                                val t = bestCum.getOrNull(i) ?: 0L
                                Box(modifier = Modifier.width(cellWidth).padding(4.dp), contentAlignment = Alignment.Center) {
                                    Text(text = if (t > 0L) vm.formatMillis(t) else "--:--.---", color = Color.Green, fontSize = smallFont)
                                }
                            }
                            // Share button
                            Box(modifier = Modifier.width(cellWidth).padding(4.dp), contentAlignment = Alignment.Center) {
                                Button(onClick = {
                                    val msg = vm.buildPbShareMessage()
                                    shareOnWhatsApp(context, msg)
                                }) { Text("Compartir", fontSize = 12.sp) }
                            }
                        }


                        Divider(color = Color.DarkGray)

                        // Second row: Best Possible Time (theoretical) in gold
                        Row(modifier = Modifier.height(headerHeight).fillMaxWidth()) {
                            Box(modifier = Modifier.width(cellWidth).padding(4.dp), contentAlignment = Alignment.CenterStart) {
                                Text(text = "Best Possible Time", color = Color(0xFFFFD700), fontSize = normalFont)
                            }
                            for (i in templates.indices) {
                                val t = theoretical.getOrNull(i) ?: 0L
                                Box(modifier = Modifier.width(cellWidth).padding(4.dp), contentAlignment = Alignment.Center) {
                                    Text(text = if (t > 0L) vm.formatMillis(t) else "--:--.---", color = Color(0xFFFFD700), fontSize = smallFont)
                                }
                            }
                            // Share button
                            Box(modifier = Modifier.width(cellWidth).padding(4.dp), contentAlignment = Alignment.Center) {
                                Button(onClick = {
                                    val msg = vm.buildBestPossibleShareMessage()
                                    shareOnWhatsApp(context, msg)
                                }) { Text("Compartir", fontSize = 12.sp) }
                            }
                        }


                        Divider(color = Color.DarkGray)

                        // Remaining rows: each run
                        // We show runs in the order provided by ViewModel (expected newest first)
                        runs.forEachIndexed { idx, pair ->
                            val run = pair.first
                            val times = pair.second.sortedBy { it.splitIndex } // ensure order
                            Row(modifier = Modifier.height(headerHeight).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.width(cellWidth).padding(4.dp), contentAlignment = Alignment.CenterStart) {
                                    Text(text = "Run ${run.id}", color = Color.White, fontSize = normalFont)
                                }
                                for (i in templates.indices) {
                                    val t = times.getOrNull(i)?.timeFromStartMillis ?: 0L
                                    Box(modifier = Modifier.width(cellWidth).padding(4.dp), contentAlignment = Alignment.Center) {
                                        Text(text = if (t > 0L) vm.formatMillis(t) else "--:--.---", color = Color.White, fontSize = smallFont)
                                    }
                                }
                                // Share button
                                Box(modifier = Modifier.width(cellWidth).padding(4.dp), contentAlignment = Alignment.Center) {
                                    Button(onClick = {
                                        val msg = vm.buildRunShareMessage(idx)
                                        shareOnWhatsApp(context, msg)
                                    }) { Text("Compartir", fontSize = 12.sp) }
                                }
                            }
                            Divider(color = Color.DarkGray)
                        }
                    }
                }
            }
        }
    }
}