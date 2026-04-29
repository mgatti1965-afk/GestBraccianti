package com.example.gestbraccianti.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gestbraccianti.data.entity.Worker
import com.example.gestbraccianti.ui.viewmodel.WorkerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerRegistryScreen(
    viewModel: WorkerViewModel,
    yearId: Int
) {
    val workers by viewModel.workersForCurrentYear.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var selectedWorker by remember { mutableStateOf<Worker?>(null) }
    var currentRate by remember { mutableStateOf(0.0) }

    if (showDialog) {
        AddEditWorkerDialog(
            worker = selectedWorker,
            initialRate = currentRate,
            onDismiss = { showDialog = false },
            onConfirm = { name, surname, rate ->
                if (selectedWorker == null) {
                    viewModel.addWorkerToYear(name, surname, rate, yearId)
                } else {
                    viewModel.updateWorkerInfo(selectedWorker!!.id, name, surname, yearId, rate)
                }
                showDialog = false
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (workers.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Nessun bracciante registrato per questa annata.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(workers) { worker ->
                    var rate by remember { mutableStateOf(0.0) }
                    
                    LaunchedEffect(worker.id) {
                        rate = viewModel.getWorkerConfig(worker.id, yearId)?.hourlyRate ?: 0.0
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedWorker = worker
                                currentRate = rate
                                showDialog = true
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${worker.name} ${worker.surname}".trim(),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Tariffa: $rate €/h",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Icon(Icons.Default.Edit, contentDescription = "Modifica")
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { 
                selectedWorker = null
                currentRate = 0.0
                showDialog = true 
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Aggiungi Bracciante")
        }
    }
}

@Composable
fun AddEditWorkerDialog(
    worker: Worker?,
    initialRate: Double,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Double) -> Unit
) {
    var name by remember { mutableStateOf(worker?.name ?: "") }
    var surname by remember { mutableStateOf(worker?.surname ?: "") }
    var rate by remember { mutableStateOf(if (initialRate > 0) initialRate.toString() else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (worker == null) "Nuovo Bracciante" else "Modifica Bracciante") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(value = name, onValueChange = { name = it }, label = { Text("Nome (Obbligatorio)") })
                TextField(value = surname, onValueChange = { surname = it }, label = { Text("Cognome") })
                TextField(value = rate, onValueChange = { rate = it }, label = { Text("Paga Oraria (€)") })
            }
        },
        confirmButton = {
            Button(onClick = {
                val r = rate.toDoubleOrNull() ?: 0.0
                if (name.isNotBlank()) { // Only name is mandatory
                    onConfirm(name, surname, r)
                }
            }) {
                Text(if (worker == null) "Aggiungi" else "Salva")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}
