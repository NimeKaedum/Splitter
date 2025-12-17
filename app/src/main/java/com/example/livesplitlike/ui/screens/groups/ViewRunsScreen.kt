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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ArrowBackIos
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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

    // Colores usando la paleta del Theme; caídas seguras si no hubiera Theme
    val colors = MaterialTheme.colorScheme
    val background = colors.background
    val surface = colors.surface
    val surfaceVariant = colors.surfaceVariant
    val onSurface = colors.onSurface
    val headerBg = surfaceVariant
    val firstColumnBg = colors.surfaceVariant // ligeramente más oscuro para la primera columna
    val altRowBg = surface
    val rowBg = background

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(), // padding para status/navigation bars

    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            // Top row con título y botón regresar como icono
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = groupName,
                    color = onSurface,
                    fontSize = 24.sp,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.SemiBold
                )

                IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Regresar"
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(modifier = Modifier.fillMaxSize()) {
                val hScroll = rememberScrollState()
                val vScroll = rememberScrollState()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(hScroll)
                ) {
                    // Header (encabezados de columnas)
                    Row(
                        modifier = Modifier
                            .height(headerHeight)
                            .fillMaxWidth()
                    ) {
                        // Primera celda de header (Runs)
                        Box(
                            modifier = Modifier
                                .width(cellWidth)
                                .padding(4.dp)
                                .background(headerBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "Runs", color = onSurface, fontSize = normalFont, fontWeight = FontWeight.Medium)
                        }

                        templates.forEach { tpl ->
                            Box(
                                modifier = Modifier
                                    .width(cellWidth)
                                    .padding(4.dp)
                                    .background(headerBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = tpl.name, color = onSurface, fontSize = smallFont, fontWeight = FontWeight.Medium)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .width(cellWidth)
                                .padding(4.dp)
                                .background(headerBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "Opciones", color = onSurface, fontSize = smallFont, fontWeight = FontWeight.Medium)
                        }
                    }

                    Divider(color = Color.DarkGray)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(vScroll)
                    ) {
                        // PB row (solo compartir) - mantener color verde original y poner en negrita
                        Row(modifier = Modifier.height(headerHeight).fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .width(cellWidth)
                                    .padding(4.dp)
                                    .background(Color.Transparent),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = "PB",
                                    color = Color.Green,
                                    fontSize = normalFont,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            for (i in templates.indices) {
                                val t = bestCum.getOrNull(i) ?: 0L
                                Box(
                                    modifier = Modifier
                                        .width(cellWidth)
                                        .padding(4.dp)
                                        .background(Color.Transparent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (t > 0L) vm.formatMillis(t) else "--:--.---",
                                        color = Color.Green,
                                        fontSize = smallFont,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Icono compartir
                            Box(
                                modifier = Modifier
                                    .width(cellWidth)
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                IconButton(onClick = {
                                    val msg = vm.buildPbShareMessage()
                                    shareOnWhatsApp(context, msg)
                                }, modifier = Modifier.size(40.dp)) {
                                    Icon(imageVector = Icons.Filled.Share, contentDescription = "Compartir PB")
                                }
                            }
                        }

                        Divider(color = Color.DarkGray)

                        // Best Possible Time row (solo compartir) - mantener color original y negrita
                        Row(modifier = Modifier.height(headerHeight).fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .width(cellWidth)
                                    .padding(4.dp)
                                    .background(Color.Transparent),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = "Best Possible Time",
                                    color = Color(0xFFFFD700),
                                    fontSize = normalFont,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            for (i in templates.indices) {
                                val t = theoretical.getOrNull(i) ?: 0L
                                Box(
                                    modifier = Modifier
                                        .width(cellWidth)
                                        .padding(4.dp)
                                        .background(Color.Transparent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (t > 0L) vm.formatMillis(t) else "--:--.---",
                                        color = Color(0xFFFFD700),
                                        fontSize = smallFont,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .width(cellWidth)
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                IconButton(onClick = {
                                    val msg = vm.buildBestPossibleShareMessage()
                                    shareOnWhatsApp(context, msg)
                                }, modifier = Modifier.size(40.dp)) {
                                    Icon(imageVector = Icons.Filled.Share, contentDescription = "Compartir BPT")
                                }
                            }
                        }

                        Divider(color = Color.DarkGray)

                        // Runs rows: cada fila tiene dropdown con Compartir y Eliminar
                        runs.forEachIndexed { idx, pair ->
                            val run = pair.first
                            val times = pair.second.sortedBy { it.splitIndex }
                            // state para el dropdown de esta fila
                            var expanded by remember { mutableStateOf(false) }

                            // Alternar color de fila para facilitar lectura
                            val rowBackground = if (idx % 2 == 0) altRowBg else rowBg

                            Row(
                                modifier = Modifier
                                    .height(headerHeight)
                                    .fillMaxWidth()
                                    .background(rowBackground),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Primera columna (nombre/run) con fondo diferenciado
                                Box(
                                    modifier = Modifier
                                        .width(cellWidth)
                                        .padding(4.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = "Run ${run.id}",
                                        color = onSurface,
                                        fontSize = normalFont
                                    )
                                }

                                for (i in templates.indices) {
                                    val t = times.getOrNull(i)?.timeFromStartMillis ?: 0L
                                    Box(
                                        modifier = Modifier
                                            .width(cellWidth)
                                            .padding(4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (t > 0L) vm.formatMillis(t) else "--:--.---",
                                            color = onSurface,
                                            fontSize = smallFont
                                        )
                                    }
                                }

                                // Opciones: icono que abre DropdownMenu
                                Box(
                                    modifier = Modifier
                                        .width(cellWidth)
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    IconButton(onClick = { expanded = true }, modifier = Modifier.size(40.dp)) {
                                        Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "Opciones")
                                    }

                                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                        DropdownMenuItem(
                                            text = { Text("Compartir") },
                                            leadingIcon = { Icon(imageVector = Icons.Filled.Share, contentDescription = null) },
                                            onClick = {
                                                expanded = false
                                                val msg = vm.buildRunShareMessage(idx)
                                                shareOnWhatsApp(context, msg)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Eliminar") },
                                            leadingIcon = { Icon(imageVector = Icons.Filled.Delete, contentDescription = null) },
                                            onClick = {
                                                expanded = false
                                                // preparar confirmación
                                                pendingDeleteRunId = run.id
                                                pendingDeleteRunLabel = "Run ${run.id}"
                                            }
                                        )
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

    // Dialogo de confirmación para eliminar run (lógica intacta)
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

// Helper para compartir por WhatsApp o fallback al share sheet (sin cambios lógicos)
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
