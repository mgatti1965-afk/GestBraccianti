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
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gestbraccianti.data.entity.WorkLog
import com.example.gestbraccianti.data.entity.Worker
import com.example.gestbraccianti.data.entity.WorkerGroup
import com.example.gestbraccianti.ui.viewmodel.WorkLogViewModel
import com.example.gestbraccianti.ui.viewmodel.WorkerGroupViewModel
import com.example.gestbraccianti.ui.viewmodel.WorkerViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkDayDetailScreen(
    date: Long,
    yearId: Int,
    workLogViewModel: WorkLogViewModel,
    workerViewModel: WorkerViewModel,
    groupViewModel: WorkerGroupViewModel,
    onBack: () -> Unit
) {
    val allLogs by workLogViewModel.allLogs.collectAsState()
    val logsForDay = remember(allLogs, date) { allLogs.filter { it.date == date } }
    val workers by workerViewModel.workersForCurrentYear.collectAsState()
    val groups by groupViewModel.groupsForYear.collectAsState()
    val scope = rememberCoroutineScope()
    
    var showAddWorkerDialog by remember { mutableStateOf(false) }
    var showAddGroupDialog by remember { mutableStateOf(false) }
    var editingLog by remember { mutableStateOf<WorkLog?>(null) }

    val sdf = SimpleDateFormat("EEEE dd MMMM yyyy", Locale.ITALY)

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
                                        text = "${worker?.surname ?: ""} ${worker?.name ?: ""}".trim(),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "M: ${log.morningStart ?: "--"}-${log.morningEnd ?: "--"} | P: ${log.afternoonStart ?: "--"}-${log.afternoonEnd ?: "--"}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "Totale: ${String.format(Locale.ITALY, "%.2f", log.totalHours)} ore",
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

            Column(
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.End
            ) {
                SmallFloatingActionButton(
                    onClick = { showAddGroupDialog = true },
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Group, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Aggiungi Gruppo")
                    }
                }
                FloatingActionButton(
                    onClick = {
                        editingLog = null
                        showAddWorkerDialog = true
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Aggiungi Bracciante")
                }
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

    if (showAddGroupDialog) {
        AddGroupToDayDialog(
            groups = groups,
            onDismiss = { showAddGroupDialog = false },
            onConfirm = { group, mStart, mEnd, pStart, pEnd ->
                scope.launch {
                    val members = groupViewModel.getWorkersInGroup(group.id).first()
                    members.forEach { worker ->
                        workLogViewModel.saveLog(
                            id = 0L,
                            workerId = worker.id,
                            yearId = yearId,
                            date = date,
                            morningStart = mStart,
                            morningEnd = mEnd,
                            afternoonStart = pStart,
                            afternoonEnd = pEnd
                        )
                    }
                }
                showAddGroupDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGroupToDayDialog(
    groups: List<WorkerGroup>,
    onDismiss: () -> Unit,
    onConfirm: (WorkerGroup, String, String, String, String) -> Unit
) {
    var selectedGroup by remember { mutableStateOf<WorkerGroup?>(null) }
    var morningStart by remember { mutableStateOf("08:00") }
    var morningEnd by remember { mutableStateOf("12:00") }
    var afternoonStart by remember { mutableStateOf("13:00") }
    var afternoonEnd by remember { mutableStateOf("17:00") }
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
            onTimeSelected(String.format(Locale.ITALY, "%02d:%02d", h, m))
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
    }

    fun validateTimes(): Boolean {
        val sdf = SimpleDateFormat("HH:mm", Locale.ITALY)
        try {
            if (morningStart.isNotBlank() && morningEnd.isNotBlank()) {
                if (!sdf.parse(morningEnd)!!.after(sdf.parse(morningStart))) {
                    errorMessage = "Fine mattina deve essere dopo l'inizio"
                    return false
                }
            }
            if (afternoonStart.isNotBlank() && afternoonEnd.isNotBlank()) {
                if (!sdf.parse(afternoonEnd)!!.after(sdf.parse(afternoonStart))) {
                    errorMessage = "Fine pomeriggio deve essere dopo l'inizio"
                    return false
                }
            }
        } catch (e: Exception) {
            errorMessage = "Formato orario non valido"
            return false
        }
        errorMessage = null
        return true
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Aggiungi Gruppo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    TextField(
                        value = selectedGroup?.name ?: "Seleziona Gruppo",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        groups.forEach { group ->
                            DropdownMenuItem(text = { Text(group.name) }, onClick = { selectedGroup = group; expanded = false })
                        }
                    }
                }
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = {
                        morningStart = "08:00"; morningEnd = "12:00"; afternoonStart = "13:00"; afternoonEnd = "17:00"
                    }) { Text("Standard 8h") }
                    TextButton(onClick = {
                        morningStart = "08:00"; morningEnd = "12:00"; afternoonStart = ""; afternoonEnd = ""
                    }) { Text("Solo Matt.") }
                }

                Text("Mattina", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showTimePicker(morningStart) { morningStart = it } }, modifier = Modifier.weight(1f)) { Text(morningStart.ifBlank { "Inizio" }) }
                    OutlinedButton(onClick = { showTimePicker(morningEnd) { morningEnd = it } }, modifier = Modifier.weight(1f)) { Text(morningEnd.ifBlank { "Fine" }) }
                }
                Text("Pomeriggio", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showTimePicker(afternoonStart) { afternoonStart = it } }, modifier = Modifier.weight(1f)) { Text(afternoonStart.ifBlank { "Inizio" }) }
                    OutlinedButton(onClick = { showTimePicker(afternoonEnd) { afternoonEnd = it } }, modifier = Modifier.weight(1f)) { Text(afternoonEnd.ifBlank { "Fine" }) }
                }

                if (errorMessage != null) {
                    Text(errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (validateTimes()) {
                    selectedGroup?.let { onConfirm(it, morningStart, morningEnd, afternoonStart, afternoonEnd) }
                }
            }, enabled = selectedGroup != null) { Text("Aggiungi") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } }
    )
}

private fun Modifier.menuAnchor(type: MenuAnchorType): Modifier = this // Mock to fix deprecation warning in the flow

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
    var morningStart by remember { mutableStateOf(editingLog?.morningStart ?: "08:00") }
    var morningEnd by remember { mutableStateOf(editingLog?.morningEnd ?: "12:00") }
    var afternoonStart by remember { mutableStateOf(editingLog?.afternoonStart ?: "13:00") }
    var afternoonEnd by remember { mutableStateOf(editingLog?.afternoonEnd ?: "17:00") }
    
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
            onTimeSelected(String.format(Locale.ITALY, "%02d:%02d", h, m))
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
    }

    fun validateTimes(): Boolean {
        val sdf = SimpleDateFormat("HH:mm", Locale.ITALY)
        try {
            if (morningStart.isNotBlank() && morningEnd.isNotBlank()) {
                if (!sdf.parse(morningEnd)!!.after(sdf.parse(morningStart))) {
                    errorMessage = "Fine mattina deve essere dopo l'inizio"
                    return false
                }
            }
            if (afternoonStart.isNotBlank() && afternoonEnd.isNotBlank()) {
                if (!sdf.parse(afternoonEnd)!!.after(sdf.parse(afternoonStart))) {
                    errorMessage = "Fine pomeriggio deve essere dopo l'inizio"
                    return false
                }
            }
        } catch (e: Exception) {
            errorMessage = "Formato orario non valido"
            return false
        }
        errorMessage = null
        return true
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
                            value = selectedWorker?.let { "${it.surname} ${it.name}".trim() } ?: "Seleziona Bracciante",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true).fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            availableWorkers.filter { w -> existingLogs.none { it.workerId == w.id } }
                                .sortedWith(compareBy({ it.surname }, { it.name }))
                                .forEach { worker ->
                                    DropdownMenuItem(
                                        text = { Text("${worker.surname} ${worker.name}".trim()) },
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
                        "Lavoratore: ${selectedWorker?.surname} ${selectedWorker?.name}".trim(),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = {
                        morningStart = "08:00"; morningEnd = "12:00"; afternoonStart = "13:00"; afternoonEnd = "17:00"
                    }) { Text("Standard 8h") }
                    TextButton(onClick = {
                        morningStart = "08:00"; morningEnd = "12:00"; afternoonStart = ""; afternoonEnd = ""
                    }) { Text("Solo Matt.") }
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

                if (errorMessage != null) {
                    Text(errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (validateTimes()) {
                    selectedWorker?.let { onConfirm(it.id, morningStart, morningEnd, afternoonStart, afternoonEnd) }
                }
            }, enabled = selectedWorker != null) {
                Text("Salva")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        }
    )
}
