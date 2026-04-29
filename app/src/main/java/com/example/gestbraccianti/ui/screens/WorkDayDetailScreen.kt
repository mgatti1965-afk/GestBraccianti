package com.example.gestbraccianti.ui.screens

import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gestbraccianti.data.entity.WorkLog
import com.example.gestbraccianti.data.entity.Worker
import com.example.gestbraccianti.ui.viewmodel.WorkLogViewModel
import com.example.gestbraccianti.ui.viewmodel.WorkerViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkDayDetailScreen(
    date: Long,
    yearId: Int,
    workLogViewModel: WorkLogViewModel,
    workerViewModel: WorkerViewModel,
    onBack: () -> Unit
) {
    val allLogs by workLogViewModel.allLogs.collectAsState()
    val logsForDay = remember(allLogs, date) { allLogs.filter { it.date == date } }
    val workers by workerViewModel.workersForCurrentYear.collectAsState()
    
    var showAddWorkerDialog by remember { mutableStateOf(false) }
    var editingLog by remember { mutableStateOf<WorkLog?>(null) }

    val sdf = SimpleDateFormat("EEEE dd MMMM yyyy", Locale.getDefault())

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                }
                Text(
                    text = sdf.format(Date(date)).replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            if (logsForDay.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nessun bracciante inserito per oggi.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(logsForDay) { log ->
                        val worker = workers.find { it.id == log.workerId }
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable {
                                editingLog = log
                                showAddWorkerDialog = true
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${worker?.name ?: ""} ${worker?.surname ?: ""}".trim(),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "M: ${log.morningStart ?: "--"}-${log.morningEnd ?: "--"} | P: ${log.afternoonStart ?: "--"}-${log.afternoonEnd ?: "--"}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "Totale: ${String.format(Locale.getDefault(), "%.2f", log.totalHours)} ore",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(onClick = { workLogViewModel.deleteLog(log) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Rimuovi", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }

            FloatingActionButton(
                onClick = {
                    editingLog = null
                    showAddWorkerDialog = true
                },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Aggiungi Bracciante")
            }
        }
    }

    if (showAddWorkerDialog) {
        AddWorkerToDayDialog(
            availableWorkers = workers,
            existingLogs = logsForDay,
            editingLog = editingLog,
            onDismiss = { showAddWorkerDialog = false },
            onConfirm = { workerId, mStart, mEnd, pStart, pEnd ->
                workLogViewModel.saveLog(
                    id = editingLog?.id ?: 0L,
                    workerId = workerId,
                    yearId = yearId,
                    date = date,
                    morningStart = mStart,
                    morningEnd = mEnd,
                    afternoonStart = pStart,
                    afternoonEnd = pEnd
                )
                showAddWorkerDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWorkerToDayDialog(
    availableWorkers: List<Worker>,
    existingLogs: List<WorkLog>,
    editingLog: WorkLog?,
    onDismiss: () -> Unit,
    onConfirm: (Long, String, String, String, String) -> Unit
) {
    var selectedWorker by remember { mutableStateOf<Worker?>(availableWorkers.find { it.id == editingLog?.workerId }) }
    var morningStart by remember { mutableStateOf(editingLog?.morningStart ?: "") }
    var morningEnd by remember { mutableStateOf(editingLog?.morningEnd ?: "") }
    var afternoonStart by remember { mutableStateOf(editingLog?.afternoonStart ?: "") }
    var afternoonEnd by remember { mutableStateOf(editingLog?.afternoonEnd ?: "") }
    
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    fun showTimePicker(initialValue: String, onTimeSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        if (initialValue.isNotBlank()) {
            val parts = initialValue.split(":")
            if (parts.size == 2) {
                calendar.set(Calendar.HOUR_OF_DAY, parts[0].toInt())
                calendar.set(Calendar.MINUTE, parts[1].toInt())
            }
        }
        TimePickerDialog(context, { _, h, m ->
            onTimeSelected(String.format(Locale.getDefault(), "%02d:%02d", h, m))
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editingLog == null) "Aggiungi Bracciante" else "Modifica Orari") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (editingLog == null) {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        TextField(
                            value = selectedWorker?.let { "${it.name} ${it.surname}".trim() } ?: "Seleziona Bracciante",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            availableWorkers.filter { w -> existingLogs.none { it.workerId == w.id } }.forEach { worker ->
                                DropdownMenuItem(
                                    text = { Text("${worker.name} ${worker.surname}".trim()) },
                                    onClick = {
                                        selectedWorker = worker
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        "Lavoratore: ${selectedWorker?.name} ${selectedWorker?.surname}".trim(),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text("Mattina", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showTimePicker(morningStart) { morningStart = it } }, modifier = Modifier.weight(1f)) {
                        Text(morningStart.ifBlank { "Inizio" })
                    }
                    OutlinedButton(onClick = { showTimePicker(morningEnd) { morningEnd = it } }, modifier = Modifier.weight(1f)) {
                        Text(morningEnd.ifBlank { "Fine" })
                    }
                }
                
                Text("Pomeriggio", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showTimePicker(afternoonStart) { afternoonStart = it } }, modifier = Modifier.weight(1f)) {
                        Text(afternoonStart.ifBlank { "Inizio" })
                    }
                    OutlinedButton(onClick = { showTimePicker(afternoonEnd) { afternoonEnd = it } }, modifier = Modifier.weight(1f)) {
                        Text(afternoonEnd.ifBlank { "Fine" })
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                selectedWorker?.let { onConfirm(it.id, morningStart, morningEnd, afternoonStart, afternoonEnd) }
            }, enabled = selectedWorker != null) {
                Text("Salva")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        }
    )
}
