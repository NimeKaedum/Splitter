package com.example.livesplitlike.ui.screens.groups

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch

@Composable
fun ViewRunsScreen(
    groupId: Long,
    onBack: () -> Unit,
    vm: ViewRunsViewModel = hiltViewModel()
) {
    val templates by vm.templates.collectAsState()
    val bestCum by vm.bestCumulative.collectAsState()
    val theoretical by vm.theoreticalBestCumulative.collectAsState()
    val runs by vm.runsWithTimes.collectAsState()
    val groupName by vm.groupName.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Estado para confirmar eliminación
    var pendingDeleteRunId by remember { mutableStateOf<Long?>(null) }
    var pendingDeleteRunLabel by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(groupId) {
        vm.loadGroupRuns(groupId)
    }

    val cellWidth = 96.dp
    val headerHeight = 56.dp
    val smallFont = 12.sp
    val normalFont = 14.sp

    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(text = groupName, color = Color.White, fontSize = 24.sp, modifier = Modifier.weight(1f))
                Button(onClick = { onBack() }) { Text("Regresar") }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(modifier = Modifier.fillMaxSize().background(Color.Transparent)) {
                val hScroll = rememberScrollState()
                val vScroll = rememberScrollState()

                Column(modifier = Modifier.fillMaxSize().horizontalScroll(hScroll)) {
                    // Header
                    Row(modifier = Modifier.height(headerHeight).fillMaxWidth()) {
                        Box(modifier = Modifier.width(cellWidth).padding(4.dp), contentAlignment = Alignment.Center) {
                            Text(text = "Runs", color = Color.White, fontSize = normalFont)
                        }
                        templates.forEach { tpl ->
                            Box(modifier = Modifier.width(cellWidth).padding(4.dp), contentAlignment = Alignment.Center) {
                                Text(text = tpl.name, color = Color.White, fontSize = smallFont)
                            }
                        }
                        Box(modifier = Modifier.width(cellWidth).padding(4.dp), contentAlignment = Alignment.Center) {
                            Text(text = "Opciones", color = Color.White, fontSize = smallFont)
                        }
                    }

                    Divider(color = Color.DarkGray)

                    Column(modifier = Modifier.fillMaxWidth().verticalScroll(vScroll)) {
                        // PB row (solo compartir)
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
                            // Compartir PB
                            Box(modifier = Modifier.width(cellWidth).padding(4.dp), contentAlignment = Alignment.Center) {
                                Button(onClick = {
                                    val msg = vm.buildPbShareMessage()
                                    shareOnWhatsApp(context, msg)
                                }) { Text("Compartir", fontSize = 12.sp) }
                            }
                        }

                        Divider(color = Color.DarkGray)

                        // Best Possible Time row (solo compartir)
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
                            Box(modifier = Modifier.width(cellWidth).padding(4.dp), contentAlignment = Alignment.Center) {
                                Button(onClick = {
                                    val msg = vm.buildBestPossibleShareMessage()
                                    shareOnWhatsApp(context, msg)
                                }) { Text("Compartir", fontSize = 12.sp) }
                            }
                        }

                        Divider(color = Color.DarkGray)

                        // Runs rows: cada fila tiene dropdown con Compartir y Eliminar
                        runs.forEachIndexed { idx, pair ->
                            val run = pair.first
                            val times = pair.second.sortedBy { it.splitIndex }
                            // state para el dropdown de esta fila
                            var expanded by remember { mutableStateOf(false) }

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

                                // Botón "..." que abre dropdown con Compartir y Eliminar
                                Box(modifier = Modifier.width(cellWidth).padding(4.dp), contentAlignment = Alignment.Center) {
                                    TextButton(onClick = { expanded = true }) {
                                        Text(text = "...", color = Color.White, fontSize = 18.sp)
                                    }
                                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                        DropdownMenuItem(text = { Text("Compartir") }, onClick = {
                                            expanded = false
                                            val msg = vm.buildRunShareMessage(idx)
                                            shareOnWhatsApp(context, msg)
                                        })
                                        DropdownMenuItem(text = { Text("Eliminar") }, onClick = {
                                            expanded = false
                                            // preparar confirmación
                                            pendingDeleteRunId = run.id
                                            pendingDeleteRunLabel = "Run ${run.id}"
                                        })
                                    }
                                }
                            }
                            Divider(color = Color.DarkGray)
                        }
                    }
                }
            }
        }
    }

    // Dialogo de confirmación para eliminar run
    if (pendingDeleteRunId != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteRunId = null; pendingDeleteRunLabel = null },
            title = { Text("Eliminar run") },
            text = { Text("¿Eliminar ${pendingDeleteRunLabel ?: "esta run"}? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    val rid = pendingDeleteRunId!!
                    pendingDeleteRunId = null
                    val label = pendingDeleteRunLabel
                    pendingDeleteRunLabel = null
                    // ejecutar eliminación y recargar
                    scope.launch {
                        vm.deleteRun(groupId, rid)
                    }
                }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteRunId = null; pendingDeleteRunLabel = null }) { Text("Cancelar") }
            }
        )
    }
}

// Helper para compartir por WhatsApp o fallback al share sheet
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