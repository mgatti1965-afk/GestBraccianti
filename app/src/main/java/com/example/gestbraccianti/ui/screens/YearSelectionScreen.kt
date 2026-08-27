package com.example.gestbraccianti.ui.screens

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.gestbraccianti.data.entity.HarvestYear
import com.example.gestbraccianti.ui.viewmodel.HarvestViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YearSelectionScreen(
    viewModel: HarvestViewModel,
    onYearSelected: (Int) -> Unit
) {
    val years by viewModel.allYears.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var yearToDelete by remember { mutableStateOf<HarvestYear?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    if (showAddDialog) {
        val lastYear = years.maxByOrNull { it.id }?.id
        val suggestedYear = if (lastYear == null) {
            java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        } else {
            lastYear + 1
        }

        AddYearDialog(
            suggestedYear = suggestedYear,
            onDismiss = { showAddDialog = false },
            onConfirm = { year, notes, migrateWorkers, migrateGroups ->
                viewModel.createYear(
                    year = year,
                    notes = notes,
                    migrateFrom = lastYear,
                    migrateWorkers = migrateWorkers,
                    migrateGroups = migrateGroups,
                    onSuccess = {
                        showAddDialog = false
                    },
                    onError = { error ->
                        errorMessage = error
                    }
                )
            },
            hasPreviousYear = years.isNotEmpty()
        )
    }

    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("Attenzione") },
            text = { Text(errorMessage!!) },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) {
                    Text("OK")
                }
            }
        )
    }

    if (yearToDelete != null) {
        AlertDialog(
            onDismissRequest = { yearToDelete = null },
            title = { Text("Elimina Annata") },
            text = { Text("Sei sicuro di voler eliminare l'annata ${yearToDelete!!.id}? Tutti i dati relativi (braccianti e ore di lavoro) verranno persi permanentemente.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteYear(yearToDelete!!.id)
                        yearToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Elimina")
                }
            },
            dismissButton = {
                TextButton(onClick = { yearToDelete = null }) {
                    Text("Annulla")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Seleziona Annata") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Aggiungi Annata")
            }
        }
    ) { padding ->
        if (years.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Nessuna annata presente. Creane una!")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(years) { year ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                viewModel.selectYear(year.id)
                                onYearSelected(year.id)
                            },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (year.notes.isNotEmpty()) "${year.id} - ${year.notes}" else "${year.id}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                if (year.isCurrent) {
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text("Corrente") },
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                }
                                IconButton(onClick = { yearToDelete = year }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Elimina Annata",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddYearDialog(
    suggestedYear: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, String, Boolean, Boolean) -> Unit,
    hasPreviousYear: Boolean
) {
    var yearText by remember(suggestedYear) { mutableStateOf(suggestedYear.toString()) }
    var notesText by remember { mutableStateOf("") }
    var migrateWorkers by remember { mutableStateOf(false) }
    var migrateGroups by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuova Annata", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = yearText,
                    onValueChange = { if (it.all { char -> char.isDigit() }) yearText = it },
                    label = { Text("Anno (es. 2024)", style = MaterialTheme.typography.labelLarge) },
                    textStyle = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Note (es. Vendemmia)", style = MaterialTheme.typography.labelLarge) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                if (hasPreviousYear) {
                    Text(
                        text = "Importa dati dall'ultima annata:",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { migrateWorkers = !migrateWorkers }
                            .padding(vertical = 8.dp)
                    ) {
                        Text("Braccianti", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Switch(
                            checked = migrateWorkers,
                            onCheckedChange = { migrateWorkers = it }
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { migrateGroups = !migrateGroups }
                            .padding(vertical = 8.dp)
                    ) {
                        Text("Gruppi", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Switch(
                            checked = migrateGroups,
                            onCheckedChange = { migrateGroups = it }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { 
                yearText.toIntOrNull()?.let { 
                    onConfirm(it, notesText, migrateWorkers && hasPreviousYear, migrateGroups && hasPreviousYear)
                } 
            }) {
                Text("Crea")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}
