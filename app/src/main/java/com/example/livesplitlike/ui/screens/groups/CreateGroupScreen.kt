package com.example.livesplitlike.ui.screens.groups

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Create group screen:
 * - define name
 * - define number of splits (1..50)
 * - edit each split name
 * - Save -> returns created groupId via onCreated
 */
@Composable
fun CreateGroupScreen(
    onCreated: (Long) -> Unit,
    onCancel: () -> Unit,
    vm: CreateGroupViewModel = hiltViewModel()
) {
    val name by vm.groupName.collectAsState()
    val splits by vm.splitNames.collectAsState()
    val error by vm.error.collectAsState()

    // initialize defaults once
    LaunchedEffect(Unit) { vm.initDefaults(5) }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Text(text = "Crear grupo")
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { vm.setGroupName(it) },
            label = { Text("Nombre del grupo") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Number of splits input (simple numeric field)
        var countText by remember { mutableStateOf(splits.size.toString()) }
        OutlinedTextField(
            value = countText,
            onValueChange = {
                // allow only digits
                val filtered = it.filter { ch -> ch.isDigit() }
                countText = filtered
                val num = filtered.toIntOrNull() ?: 0
                if (num in 1..50) vm.setSplitCount(num)
            },
            label = { Text("Cantidad de splits (1-50)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Split names
        Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
            splits.forEachIndexed { idx, s ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    OutlinedTextField(
                        value = s,
                        onValueChange = { vm.setSplitName(idx, it) },
                        label = { Text("Split ${idx + 1}") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Divider()
            }
        }

        if (!error.isNullOrEmpty()) {
            Text(text = error!!, color = androidx.compose.ui.graphics.Color.Red)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onCancel) { Text("Cancelar") }
            Button(onClick = {
                vm.createGroup { id -> onCreated(id) }
            }) { Text("Crear") }
        }
    }
}