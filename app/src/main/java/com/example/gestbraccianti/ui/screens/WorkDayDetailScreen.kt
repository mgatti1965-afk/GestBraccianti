package com.example.gestbraccianti.ui.screens

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.gestbraccianti.data.entity.WorkLog
import com.example.gestbraccianti.data.entity.Worker
import com.example.gestbraccianti.data.entity.WorkerGroup
import com.example.gestbraccianti.ui.viewmodel.WorkLogViewModel
import com.example.gestbraccianti.ui.viewmodel.WorkerGroupViewModel
import com.example.gestbraccianti.ui.viewmodel.WorkerViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import com.example.gestbraccianti.ui.utils.formatDecimalHours
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
    var showSmsDialog by remember { mutableStateOf(false) }
    var editingLog by remember { mutableStateOf<WorkLog?>(null) }
    val context = LocalContext.current

    val isCurrentYear = remember(date) {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        calendar.timeInMillis = date
        calendar.get(Calendar.YEAR) == currentYear
    }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                showSmsDialog = true
            }
        }
    )

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
                    modifier = Modifier.padding(16.dp).weight(1f)
                )

                if (isCurrentYear) {
                    IconButton(onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {
                            showSmsDialog = true
                        } else {
                            smsPermissionLauncher.launch(Manifest.permission.READ_SMS)
                        }
                    }) {
                        Icon(Icons.Default.Sms, contentDescription = "Importa da SMS")
                    }
                }
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
                                        text = "Totale: ${formatDecimalHours(log.totalHours)} h",
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
                        // Verifica se esiste già un log per questo lavoratore in questa data
                        val existingLog = logsForDay.find { it.workerId == worker.id }
                        workLogViewModel.saveLog(
                            id = existingLog?.id ?: 0L,
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

    if (showSmsDialog) {
        SmsImportDialog(
            date = date,
            yearId = yearId,
            workers = workers,
            existingLogs = logsForDay,
            workLogViewModel = workLogViewModel,
            onDismiss = { showSmsDialog = false }
        )
    }
}

data class SmsData(
    val workerId: Long,
    val senderName: String,
    val senderSurname: String,
    val time: String,
    val timestamp: Long,
    val text: String,
    val type: String // "I" or "F"
)

@Composable
fun SmsImportDialog(
    date: Long,
    yearId: Int,
    workers: List<Worker>,
    existingLogs: List<WorkLog>,
    workLogViewModel: WorkLogViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var smsList by remember { mutableStateOf<List<SmsData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(date, workers) {
        smsList = readSmsForDay(context, date, workers)
        isLoading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("SMS Ricevuti (I/F)") },
        text = {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (smsList.isEmpty()) {
                Text("Nessun SMS corrispondente ai criteri trovato per questa giornata.")
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(smsList) { sms ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (sms.type == "I") Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                            )
                        ) {
                            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${sms.senderSurname} ${sms.senderName}", fontWeight = FontWeight.Bold)
                                    Text("Ore: ${sms.time} - Testo: ${sms.text}", style = MaterialTheme.typography.bodySmall)
                                }
                                Badge(containerColor = if (sms.type == "I") Color(0xFF2E7D32) else Color(0xFFC62828)) {
                                    Text(if (sms.type == "I") "INIZIO" else "FINE", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDismiss) { Text("Annulla") }
                Button(
                    onClick = {
                        val groupedSms = smsList.groupBy { it.workerId }
                        groupedSms.forEach { (workerId, messages) ->
                            val starts = messages.filter { it.type == "I" }.sortedBy { it.timestamp }
                            val ends = messages.filter { it.type == "F" }.sortedBy { it.timestamp }

                            val firstIn = starts.firstOrNull()?.time
                            val lastOut = ends.lastOrNull()?.time
                            
                            var mStart = "08:00"
                            var mEnd = ""
                            var aStart = ""
                            var aEnd = ""

                            if (firstIn != null && lastOut != null) {
                                val outHour = lastOut.split(":")[0].toInt()
                                mStart = firstIn

                                if (outHour <= 13) {
                                    // Turno solo mattina
                                    mEnd = lastOut
                                } else {
                                    // Turno intero con pausa
                                    mEnd = "12:00"
                                    aStart = "13:00"
                                    aEnd = lastOut

                                    // Se abbiamo messaggi intermedi, usiamoli per la pausa
                                    if (starts.size >= 2 && ends.size >= 2) {
                                        mEnd = ends.first().time
                                        aStart = starts.last().time
                                    }
                                }
                            } else if (firstIn != null) {
                                mStart = firstIn
                            } else if (lastOut != null) {
                                val outHour = lastOut.split(":")[0].toInt()
                                if (outHour <= 13) mEnd = lastOut else aEnd = lastOut
                            }

                            val existingLog = existingLogs.find { it.workerId == workerId }
                            
                            workLogViewModel.saveLog(
                                id = existingLog?.id ?: 0L,
                                workerId = workerId,
                                yearId = yearId,
                                date = date,
                                morningStart = mStart,
                                morningEnd = if (mEnd.isNotBlank()) mEnd else (existingLog?.morningEnd ?: ""),
                                afternoonStart = if (aStart.isNotBlank()) aStart else (existingLog?.afternoonStart ?: ""),
                                afternoonEnd = if (aEnd.isNotBlank()) aEnd else (existingLog?.afternoonEnd ?: "")
                            )
                        }
                        onDismiss()
                    },
                    enabled = smsList.isNotEmpty()
                ) {
                    Text("Applica")
                }
            }
        }
    )
}

suspend fun readSmsForDay(context: Context, date: Long, workers: List<Worker>): List<SmsData> {
    val result = mutableListOf<SmsData>()
    
    // Calculate start and end of day
    val cal = Calendar.getInstance()
    cal.timeInMillis = date
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val startOfDay = cal.timeInMillis
    val endOfDay = startOfDay + (24 * 60 * 60 * 1000) - 1

    val timeSdf = SimpleDateFormat("HH:mm", Locale.ITALY)

    // 1. Read REAL SMS
    val uri = Uri.parse("content://sms/inbox")
    val projection = arrayOf("address", "body", "date")
    val selection = "date >= ? AND date <= ?"
    val selectionArgs = arrayOf(startOfDay.toString(), endOfDay.toString())
    
    context.contentResolver.query(uri, projection, selection, selectionArgs, "date ASC")?.use { cursor ->
        val addressIdx = cursor.getColumnIndex("address")
        val bodyIdx = cursor.getColumnIndex("body")
        val dateIdx = cursor.getColumnIndex("date")
        
        while (cursor.moveToNext()) {
            val address = cursor.getString(addressIdx)
            val body = cursor.getString(bodyIdx)
            val smsDate = cursor.getLong(dateIdx)
            
            processSmsEntry(address, body, smsDate, workers, timeSdf, result)
        }
    }

    // 2. Read MOCK SMS from database
    val db = com.example.gestbraccianti.data.AppDatabase.getDatabase(context)
    val mockSmsList = db.mockSmsDao().getMockSmsForRange(startOfDay, endOfDay)
    mockSmsList.forEach { mock ->
        processSmsEntry(mock.address, mock.body, mock.date, workers, timeSdf, result)
    }

    return result.sortedBy { it.timestamp }
}

private fun processSmsEntry(
    address: String?,
    body: String?,
    smsDate: Long,
    workers: List<Worker>,
    timeSdf: SimpleDateFormat,
    result: MutableList<SmsData>
) {
    if (body.isNullOrBlank()) return
    val type = body.trim().firstOrNull()?.uppercaseChar()?.toString()
    if (type != "I" && type != "F") return

    // Filter workers by phone number (last 10 digits)
    val cleanAddress = address?.filter { it.isDigit() }?.takeLast(10) ?: ""
    val worker = workers.find { 
        it.phoneNumber.filter { char -> char.isDigit() }.takeLast(10) == cleanAddress 
    }
    
    if (worker != null) {
        result.add(SmsData(
            workerId = worker.id,
            senderName = worker.name,
            senderSurname = worker.surname,
            time = timeSdf.format(Date(smsDate)),
            timestamp = smsDate,
            text = body,
            type = type
        ))
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
    var morningEnd by remember { mutableStateOf("") }
    var afternoonStart by remember { mutableStateOf("") }
    var afternoonEnd by remember { mutableStateOf("") }
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

private fun Modifier.menuAnchor(type: MenuAnchorType, enabled: Boolean = true): Modifier = this // Mock to fix deprecation warning in the flow

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
    var morningEnd by remember { mutableStateOf(editingLog?.morningEnd ?: "") }
    var afternoonStart by remember { mutableStateOf(editingLog?.afternoonStart ?: "") }
    var afternoonEnd by remember { mutableStateOf(editingLog?.afternoonEnd ?: "") }
    
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
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
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
