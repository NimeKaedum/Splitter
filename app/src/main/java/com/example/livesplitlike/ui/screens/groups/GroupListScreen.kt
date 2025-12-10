package com.example.livesplitlike.ui.screens.groups

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.livesplitlike.data.local.model.GroupEntity

/**
 * Groups list screen.
 *
 * - onCreateGroup: navigate to create screen
 * - onViewRuns: navigate to runs view for group
 * - onSelectGroup: select group and return to timer (previousBackStackEntry.savedStateHandle is handled by caller)
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

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "Grupos")
            Button(onClick = onCreateGroup) { Text("Crear grupo") }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
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

    // Confirm delete dialog
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = group.name, modifier = Modifier.weight(1f))
        Row {
            Button(onClick = onViewRuns, modifier = Modifier.padding(end = 8.dp)) { Text("Ver runs") }
            Button(onClick = onDelete) { Text("Eliminar") }
        }
    }
}