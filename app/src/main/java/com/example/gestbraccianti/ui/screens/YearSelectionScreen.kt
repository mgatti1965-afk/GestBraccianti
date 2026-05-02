package com.example.gestbraccianti.ui.screens

import android.util.Log
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

    if (showAddDialog) {
        Log.d("YearSelection", "Showing AddYearDialog")
        AddYearDialog(
            onDismiss = { 
                Log.d("YearSelection", "Dismissing dialog")
                showAddDialog = false 
            },
            onConfirm = { year ->
                Log.d("YearSelection", "Confirming year: $year")
                viewModel.createYear(year)
                showAddDialog = false
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
            FloatingActionButton(onClick = { 
                Log.d("YearSelection", "FAB Clicked")
                showAddDialog = true 
            }) {
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
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Vendemmia ${year.id}",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(Modifier.weight(1f))
                            if (year.isCurrent) {
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text("Corrente") }
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

@Composable
fun AddYearDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    // Propone l'anno in corso (NOW) come valore predefinito
    val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR).toString()
    var yearText by remember { mutableStateOf(currentYear) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuova Annata") },
        text = {
            TextField(
                value = yearText,
                onValueChange = { if (it.all { char -> char.isDigit() }) yearText = it },
                label = { Text("Anno (es. 2024)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        },
        confirmButton = {
            Button(onClick = { yearText.toIntOrNull()?.let { onConfirm(it) } }) {
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
