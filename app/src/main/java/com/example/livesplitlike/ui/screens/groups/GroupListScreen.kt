package com.example.livesplitlike.ui.screens.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.livesplitlike.data.local.model.GroupEntity
import androidx.compose.foundation.layout.systemBarsPadding

/**
 * Groups list screen.
 *
 * - onCreateGroup: navigate to create screen
 * - onViewRuns: navigate to runs view for group
 * - onSelectGroup: select group and return to timer (previousBackStackEntry.savedStateHandle is handled by caller)
 *
 * Nota: sólo se modificó la UI (padding, colores, iconos). La lógica permanece intacta.
 */
@Composable
fun GroupsListScreen(
    onCreateGroup: () -> Unit,
    onViewRuns: (Long) -> Unit,
    onSelectGroup: (Long) -> Unit,
    viewModel: GroupsViewModel = hiltViewModel()
) {
    val groups by viewModel.groups.collectAsState()
    var toDelete by remember { mutableStateOf<GroupEntity?>(null) }

    // Columna principal con padding seguro para barras del sistema
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding() // evita la zona de status/navigation
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Top row: título + icono crear
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Grupos",
                style = MaterialTheme.typography.titleLarge
            )

            // Icono de crear grupo (+)
            IconButton(
                onClick = onCreateGroup,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Crear grupo"
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Lista de grupos
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
        ) {
            items(groups, key = { it.id }) { group ->
                GroupRow(
                    group = group,
                    onClick = { onSelectGroup(group.id) },
                    onViewRuns = { onViewRuns(group.id) },
                    onDelete = { toDelete = group }
                )
                Divider()
            }
        }
    }

    // Confirm delete dialog (lógica intacta)
    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Eliminar grupo") },
            text = { Text("¿Eliminar el grupo \"${toDelete!!.name}\" y sus datos? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteGroup(toDelete!!)
                    toDelete = null
                }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { toDelete = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun GroupRow(
    group: GroupEntity,
    onClick: () -> Unit,
    onViewRuns: () -> Unit,
    onDelete: () -> Unit
) {
    // Tarjeta ligera para cada fila
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(8.dp)),
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = group.name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Botón ver runs (queda como botón de texto para no cambiar lógica)
                TextButton(onClick = onViewRuns, modifier = Modifier.padding(end = 8.dp)) {
                    Text("Ver runs")
                }

                // Icono eliminar (basura)
                IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Eliminar grupo"
                    )
                }
            }
        }
    }
}
