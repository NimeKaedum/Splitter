package com.example.livesplitlike.ui.screens.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Create group screen:
 * - define name
 * - define number of splits (1..50)
 * - edit each split name
 * - Save -> returns created groupId via onCreated
 *
 * Solo cambios de UI: paddings, estilos, botones. Lógica intacta.
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

    // local state para el input numérico (se mantiene la misma lógica que tenías)
    var countText by remember { mutableStateOf(splits.size.toString()) }

    val colors = MaterialTheme.colorScheme
    val onSurface = colors.onSurface
    val surfaceVariant = colors.surfaceVariant
    val surface = colors.surface
    val background = colors.background
    val errorColor = colors.error

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding() // padding seguro en top/bottom
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Título
        Text(
            text = "Crear grupo",
            style = MaterialTheme.typography.titleLarge,
            color = onSurface,
            modifier = Modifier.padding(vertical = 6.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Card ligera que contiene el formulario
        Surface(
            tonalElevation = 2.dp,
            shape = MaterialTheme.shapes.medium,
            color = surface, // superficie ligeramente diferenciada
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                // Nombre del grupo
                OutlinedTextField(
                    value = name,
                    onValueChange = { vm.setGroupName(it) },
                    label = { Text("Nombre del grupo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = surfaceVariant,
                        unfocusedContainerColor = surfaceVariant,
                        disabledContainerColor = surfaceVariant,
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.onSurface.copy(alpha = 0.12f),
                        focusedTextColor = onSurface,
                        unfocusedTextColor = onSurface,
                        cursorColor = colors.primary,
                        focusedLabelColor = colors.primary,
                        unfocusedLabelColor = onSurface.copy(alpha = 0.6f)
                    ),
                    textStyle = LocalTextStyle.current.copy(fontSize = 16.sp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Número de splits (campo numérico)
                OutlinedTextField(
                    value = countText,
                    onValueChange = {
                        val filtered = it.filter { ch -> ch.isDigit() }
                        countText = filtered
                        val num = filtered.toIntOrNull() ?: 0
                        if (num in 1..50) vm.setSplitCount(num)
                    },
                    label = { Text("Cantidad de splits (1-50)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = surfaceVariant,
                        unfocusedContainerColor = surfaceVariant,
                        disabledContainerColor = surfaceVariant,
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.onSurface.copy(alpha = 0.12f),
                        focusedTextColor = onSurface,
                        unfocusedTextColor = onSurface,
                        cursorColor = colors.primary,
                        focusedLabelColor = colors.primary,
                        unfocusedLabelColor = onSurface.copy(alpha = 0.6f)
                    ),
                    textStyle = LocalTextStyle.current.copy(fontSize = 15.sp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Lista de nombres de splits (cada uno con más espacio y separación)
                Column(modifier = Modifier.fillMaxWidth()) {
                    splits.forEachIndexed { idx, s ->
                        OutlinedTextField(
                            value = s,
                            onValueChange = { vm.setSplitName(idx, it) },
                            label = { Text("Split ${idx + 1}") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = surfaceVariant,
                                unfocusedContainerColor = surfaceVariant,
                                disabledContainerColor = surfaceVariant,
                                focusedBorderColor = colors.primary,
                                unfocusedBorderColor = colors.onSurface.copy(alpha = 0.08f),
                                focusedTextColor = onSurface,
                                unfocusedTextColor = onSurface,
                                cursorColor = colors.primary,
                                focusedLabelColor = colors.primary,
                                unfocusedLabelColor = onSurface.copy(alpha = 0.6f)
                            ),
                            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Error text (si lo hay) — mantiene la misma lógica, usando el color de error del Theme
        if (!error.isNullOrEmpty()) {
            Text(
                text = error!!,
                color = errorColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                textAlign = TextAlign.Start
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Botones: cancelar (outline/text) y crear (elevated, con icono)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cancelar: botón de texto con icono (ligero)
            TextButton(
                onClick = onCancel,
                modifier = Modifier.height(48.dp)
            ) {
                Icon(imageVector = Icons.Filled.Close, contentDescription = "Cancelar")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cancelar")
            }

            // Crear: botón destacado (Elevated) con icono
            ElevatedButton(
                onClick = {
                    vm.createGroup { id -> onCreated(id) }
                },
                modifier = Modifier.height(48.dp)
            ) {
                Icon(imageVector = Icons.Filled.Check, contentDescription = "Crear")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Crear")
            }
        }
    }
}
