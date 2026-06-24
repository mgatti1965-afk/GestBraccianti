package com.example.gestbraccianti.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.example.gestbraccianti.data.entity.WorkLog
import com.example.gestbraccianti.data.entity.Worker
import com.example.gestbraccianti.data.entity.WorkerGroup
import com.example.gestbraccianti.ui.viewmodel.WorkLogViewModel
import com.example.gestbraccianti.ui.viewmodel.WorkerGroupViewModel
import com.example.gestbraccianti.ui.viewmodel.WorkerViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.ui.input.pointer.pointerInput
import java.text.SimpleDateFormat
import androidx.compose.material.icons.filled.Warning
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
    var showHelpDialog by remember { mutableStateOf(false) }
    var editingLog by remember { mutableStateOf<WorkLog?>(null) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        workLogViewModel.uiEvent.collect { message ->
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
        }
    }

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

                IconButton(onClick = { showHelpDialog = true }) {
                    Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "Aiuto")
                }

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
                    Text("Nessun bracciante inserito per oggi.", style = MaterialTheme.typography.titleMedium)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 100.dp),
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

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ExtendedFloatingActionButton(
                    onClick = { showAddGroupDialog = true },
                    modifier = Modifier.weight(1f),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    icon = { Icon(Icons.Default.GroupAdd, contentDescription = null) },
                    text = { Text("Gruppi", fontWeight = FontWeight.Bold) }
                )
                ExtendedFloatingActionButton(
                    onClick = {
                        editingLog = null
                        showAddWorkerDialog = true
                    },
                    modifier = Modifier.weight(1f),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                    text = { Text("Bracciante", fontWeight = FontWeight.Bold) }
                )
            }
        }
    }

    if (showAddWorkerDialog) {
        AddWorkerToDayDialog(
            availableWorkers = workers,
            existingLogs = logsForDay,
            editingLog = editingLog,
            currentDate = date,
            onDismiss = { showAddWorkerDialog = false },
            onConfirm = { workerId, mStart, mEnd, pStart, pEnd, rangeEnd ->
                if (rangeEnd != null) {
                    workLogViewModel.saveLogRange(
                        workerId = workerId,
                        yearId = yearId,
                        startDate = date,
                        endDate = rangeEnd,
                        morningStart = mStart,
                        morningEnd = mEnd,
                        afternoonStart = pStart,
                        afternoonEnd = pEnd
                    )
                    onBack()
                } else {
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
                }
                showAddWorkerDialog = false
            }
        )
    }

    if (showAddGroupDialog) {
        AddGroupToDayDialog(
            groups = groups,
            existingLogs = logsForDay,
            currentDate = date,
            onDismiss = { showAddGroupDialog = false },
            onConfirm = { group, mStart, mEnd, pStart, pEnd, rangeEnd ->
                scope.launch {
                    val members = groupViewModel.getWorkersInGroup(group.id).first()
                    members.forEach { worker ->
                        if (rangeEnd != null) {
                            workLogViewModel.saveLogRange(
                                workerId = worker.id,
                                yearId = yearId,
                                startDate = date,
                                endDate = rangeEnd,
                                morningStart = mStart,
                                morningEnd = mEnd,
                                afternoonStart = pStart,
                                afternoonEnd = pEnd
                            )
                        } else {
                            val existing = logsForDay.find { it.workerId == worker.id }
                            workLogViewModel.saveLog(
                                id = existing?.id ?: 0L,
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
                    if (rangeEnd != null) onBack()
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

    if (showHelpDialog) {
        QuickHelpDialog(onDismiss = { showHelpDialog = false })
    }
}

@Composable
fun QuickHelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Guida Utilizzo")
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Inserimento Orari", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
                HelpItem(Icons.Default.Add, "Tasti +/- : Variazione di 15 minuti.")
                HelpItem(Icons.Default.TouchApp, "Pressione Lunga : Scorrimento veloce dei minuti.")
                HelpItem(Icons.Default.Edit, "Tocca l'Orario : Inserimento manuale con tastiera.")
                HelpItem(Icons.Default.DeleteForever, "CANCELLA : Rimuove l'orario (es. se non lavora il pomeriggio).")
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                
                Text("Funzioni Avanzate", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
                HelpItem(Icons.Default.DateRange, "Espandi Periodo : Duplica gli orari su più giorni consecutivi (es. tutta la settimana).")
                HelpItem(Icons.Default.CheckCircle, "Conferma Massiva : Un riepilogo indica quanti giorni verranno creati o sovrascritti.")
                HelpItem(Icons.Default.Sync, "Tariffe : Se modifichi una tariffa nel registro, le giornate passate si aggiorneranno salvandole di nuovo.")
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Ho capito") }
        }
    )
}

@Composable
private fun HelpItem(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

data class SmsData(
    val workerId: Long,
    val senderName: String,
    val senderSurname: String,
    val time: String,
    val timestamp: Long,
    val text: String,
    val type: String
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
    var smsList by remember { mutableStateOf<List<SmsData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showConfirmation by remember { mutableStateOf(false) }

    LaunchedEffect(date, workers) {
        smsList = readSmsForDay(context, date, workers)
        isLoading = false
    }

    if (showConfirmation) {
        val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.ITALY).format(Date(date))
        AlertDialog(
            onDismissRequest = { showConfirmation = false },
            title = { Text("Conferma Importazione") },
            text = { Text("Attenzione: caricamento dati al $dateStr. Confermi l'importazione degli SMS?") },
            confirmButton = {
                Button(onClick = {
                    applySmsImport(smsList, existingLogs, workLogViewModel, yearId, date)
                    showConfirmation = false
                    onDismiss()
                }) { Text("Conferma") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmation = false }) { Text("Annulla") }
            }
        )
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
                Text("Nessun SMS trovato per questa giornata.")
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
                    onClick = { showConfirmation = true },
                    enabled = smsList.isNotEmpty()
                ) { Text("Applica") }
            }
        }
    )
}


private fun applySmsImport(
    smsList: List<SmsData>,
    existingLogs: List<WorkLog>,
    workLogViewModel: WorkLogViewModel,
    yearId: Int,
    date: Long
) {
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
                mEnd = lastOut
            } else {
                mEnd = "12:00"
                aStart = "13:00"
                aEnd = lastOut
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
        val existing = existingLogs.find { it.workerId == workerId }
        workLogViewModel.saveLog(
            id = existing?.id ?: 0L,
            workerId = workerId,
            yearId = yearId,
            date = date,
            morningStart = mStart,
            morningEnd = mEnd.ifBlank { existing?.morningEnd ?: "" },
            afternoonStart = aStart.ifBlank { existing?.afternoonStart ?: "" },
            afternoonEnd = aEnd.ifBlank { existing?.afternoonEnd ?: "" }
        )
    }
}


fun readSmsForDay(context: Context, date: Long, workers: List<Worker>): List<SmsData> {
    val result = mutableListOf<SmsData>()
    val cal = Calendar.getInstance().apply { 
        timeInMillis = date
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val startOfDay = cal.timeInMillis
    val endOfDay = startOfDay + (24 * 60 * 60 * 1000) - 1
    val timeSdf = SimpleDateFormat("HH:mm", Locale.ITALY)
    val uri = "content://sms/inbox".toUri()
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
    return result.sortedBy { it.timestamp }
}

private fun processSmsEntry(address: String?, body: String?, smsDate: Long, workers: List<Worker>, timeSdf: SimpleDateFormat, result: MutableList<SmsData>) {
    if (body.isNullOrBlank()) return
    val type = body.trim().firstOrNull()?.uppercaseChar()?.toString()
    if (type != "I" && type != "F") return
    val cleanAddress = address?.filter { it.isDigit() }?.takeLast(10) ?: ""
    val worker = workers.find { it.phoneNumber.filter { char -> char.isDigit() }.takeLast(10) == cleanAddress }
    if (worker != null) {
        result.add(SmsData(workerId = worker.id, senderName = worker.name, senderSurname = worker.surname, time = timeSdf.format(Date(smsDate)), timestamp = smsDate, text = body, type = type))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGroupToDayDialog(
    groups: List<WorkerGroup>,
    existingLogs: List<WorkLog>,
    currentDate: Long,
    onDismiss: () -> Unit,
    onConfirm: (WorkerGroup, String, String, String, String, Long?) -> Unit
) {
    var selectedGroup by remember { mutableStateOf<WorkerGroup?>(if (groups.size == 1) groups.first() else null) }
    
    // Logica di inizializzazione "a cascata" per NUOVO o MODIFICA
    var morningStart by remember { mutableStateOf("08:00") }
    var morningEnd by remember { mutableStateOf("") }
    var afternoonStart by remember { mutableStateOf("") }
    var afternoonEnd by remember { mutableStateOf("") }

    var expandPeriod by remember { mutableStateOf(false) }
    var endDate by remember { mutableStateOf(currentDate) }
    var showRangeConfirmDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }

    if (showHelpDialog) {
        QuickHelpDialog(onDismiss = { showHelpDialog = false })
    }

    // Riferimenti per i suggerimenti (servono per il colore)
    var suggestedMorningEnd by remember { mutableStateOf("") }
    var suggestedAfternoonStart by remember { mutableStateOf("") }
    var suggestedAfternoonEnd by remember { mutableStateOf("") }

    if (showRangeConfirmDialog) {
        val daysCount = ((endDate - currentDate) / (24 * 60 * 60 * 1000)).toInt() + 1
        AlertDialog(
            onDismissRequest = { showRangeConfirmDialog = false },
            title = { Text("Inserimento orari per $daysCount giorni lavorativi.", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
            text = { Text("Attenzione: gli orari già presenti in questo periodo verranno sovrascritti. Vuoi continuare?") },
            confirmButton = {
                Button(onClick = {
                    showRangeConfirmDialog = false
                    selectedGroup?.let { onConfirm(it, morningStart, morningEnd, afternoonStart, afternoonEnd, endDate) }
                }) { Text("Conferma") }
            },
            dismissButton = {
                TextButton(onClick = { showRangeConfirmDialog = false }) { Text("Annulla") }
            }
        )
    }

    // Logica basata sulla PRESENZA - Solo all'ingresso (per i gruppi è sempre "nuovo")
    LaunchedEffect(Unit) {
        // In "Aggiungi Gruppo" non suggeriamo mai nulla all'ingresso (morningEnd resta vuoto)
        // E non ci sono LaunchedEffect che ascoltano le modifiche manuali, quindi non succederà nulla dopo.
    }
    var expanded by remember { mutableStateOf(selectedGroup == null && groups.size > 1) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun isTimeRangeValid(start: String, end: String): Boolean {
        if (start.isBlank() || end.isBlank()) return true
        return try {
            val sdf = SimpleDateFormat("HH:mm", Locale.ITALY)
            val s = sdf.parse(start)
            val e = sdf.parse(end)
            s != null && e != null && e.after(s)
        } catch (_: Exception) {
            false
        }
    }

    val isMorningValid = isTimeRangeValid(morningStart, morningEnd)
    val isAfternoonValid = isTimeRangeValid(afternoonStart, afternoonEnd)
    val isFormValid = selectedGroup != null && isMorningValid && isAfternoonValid && 
                     (morningStart.isNotBlank() || afternoonStart.isNotBlank())

    fun validateTimes(): Boolean {
        if (!isMorningValid) {
            errorMessage = "Fine mattina deve essere dopo l'inizio"
            return false
        }
        if (!isAfternoonValid) {
            errorMessage = "Fine pomeriggio deve essere dopo l'inizio"
            return false
        }
        if (morningStart.isBlank() && afternoonStart.isBlank()) {
            errorMessage = "Inserire almeno un orario di inizio"
            return false
        }
        errorMessage = null
        return true
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Aggiungi Gruppo", 
                    style = MaterialTheme.typography.headlineSmall, 
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { showHelpDialog = true }) {
                    Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "Aiuto", tint = MaterialTheme.colorScheme.primary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    TextField(
                        value = selectedGroup?.name ?: "Seleziona Gruppo",
                        onValueChange = {},
                        readOnly = true,
                        textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        groups.forEach { group ->
                            DropdownMenuItem(
                                text = { Text(group.name, style = MaterialTheme.typography.titleMedium) }, 
                                onClick = { 
                                    selectedGroup = group
                                    expanded = false 
                                },
                                contentPadding = PaddingValues(16.dp)
                            )
                        }
                    }
                }
                HorizontalDivider()
                TimePickerSection(
                    label = "Mattina",
                    start = morningStart,
                    end = morningEnd,
                    onStartChange = { morningStart = it },
                    onEndChange = { morningEnd = it },
                    suggestedEnd = suggestedMorningEnd,
                    onConfirmSuggestedEnd = { suggestedMorningEnd = "" },
                    defaultStart = "08:00",
                    defaultEnd = "12:00"
                )
                HorizontalDivider()
                TimePickerSection(
                    label = "Pomeriggio",
                    start = afternoonStart,
                    end = afternoonEnd,
                    onStartChange = { afternoonStart = it },
                    onEndChange = { afternoonEnd = it },
                    suggestedStart = suggestedAfternoonStart,
                    suggestedEnd = suggestedAfternoonEnd,
                    onConfirmSuggestedStart = { suggestedAfternoonStart = "" },
                    onConfirmSuggestedEnd = { suggestedAfternoonEnd = "" },
                    defaultStart = "13:00",
                    defaultEnd = "17:00"
                )
                if (errorMessage != null) {
                    Text(errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                ExpandPeriodSection(
                    isExpanded = expandPeriod,
                    onExpandedChange = { expandPeriod = it },
                    currentDate = currentDate,
                    endDate = endDate,
                    onEndDateChange = { endDate = it }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (validateTimes()) {
                    if (expandPeriod) {
                        showRangeConfirmDialog = true
                    } else {
                        selectedGroup?.let { onConfirm(it, morningStart, morningEnd, afternoonStart, afternoonEnd, null) }
                    }
                }
            }, enabled = isFormValid) { Text("Salva") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } }
    )
}

@Composable
fun TimePickerSection(
    label: String,
    start: String,
    end: String,
    onStartChange: (String) -> Unit,
    onEndChange: (String) -> Unit,
    suggestedStart: String = "",
    suggestedEnd: String = "",
    onConfirmSuggestedStart: () -> Unit = {},
    onConfirmSuggestedEnd: () -> Unit = {},
    defaultStart: String = "08:00",
    defaultEnd: String = "12:00"
) {
    Column {
        Text(text = label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Inizio", style = MaterialTheme.typography.labelMedium)
                TactileTimePicker(
                    value = start,
                    defaultValue = defaultStart,
                    onValueChange = onStartChange,
                    isSuggested = suggestedStart.isNotBlank() && start == suggestedStart,
                    onConfirmSuggested = onConfirmSuggestedStart
                )
            }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Fine", style = MaterialTheme.typography.labelMedium)
                TactileTimePicker(
                    value = end,
                    defaultValue = defaultEnd,
                    onValueChange = onEndChange,
                    isSuggested = suggestedEnd.isNotBlank() && end == suggestedEnd,
                    onConfirmSuggested = onConfirmSuggestedEnd
                )
            }
        }
    }
}

@Composable
fun TactileTimePicker(
    value: String,
    defaultValue: String = "08:00",
    onValueChange: (String) -> Unit,
    isSuggested: Boolean = false,
    onConfirmSuggested: () -> Unit = {}
) {
    val displayValue = value.ifBlank { "--:--" }
    val textColor = if (isSuggested) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
    val fontWeight = if (isSuggested) FontWeight.Normal else FontWeight.ExtraBold
    
    var showManualEdit by remember { mutableStateOf(false) }

    fun adjust(deltaMinutes: Int) {
        val current = value.ifBlank { defaultValue }
        try {
            val parts = current.split(":")
            val h = parts[0].toInt()
            val m = parts[1].toInt()
            val cal = Calendar.getInstance().apply { 
                set(Calendar.HOUR_OF_DAY, h)
                set(Calendar.MINUTE, m)
                add(Calendar.MINUTE, deltaMinutes) 
            }
            onValueChange(String.format(Locale.ITALY, "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)))
        } catch (_: Exception) {
            onValueChange(defaultValue)
        }
    }

    if (showManualEdit) {
        var tempTime by remember { mutableStateOf(value.ifBlank { defaultValue }) }
        AlertDialog(
            onDismissRequest = { showManualEdit = false },
            title = { Text("Inserimento Manuale") },
            text = {
                TextField(
                    value = tempTime, 
                    onValueChange = { tempTime = it }, 
                    placeholder = { Text("HH:mm") }, 
                    textStyle = MaterialTheme.typography.displaySmall.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold), 
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number), 
                    modifier = Modifier.fillMaxWidth(), 
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (tempTime.matches(Regex("^([01]\\d|2[0-3]):([0-5]\\d)$"))) { 
                        onValueChange(tempTime)
                        showManualEdit = false 
                    }
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showManualEdit = false }) { Text("Annulla") } }
        )
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        RepeatingIconButton(
            onClick = { adjust(15) },
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            icon = Icons.Default.Add,
            contentDescription = "Più"
        )

        Text(
            text = displayValue,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = fontWeight,
            color = textColor,
            modifier = Modifier
                .padding(vertical = 2.dp)
                .clickable { 
                    if (isSuggested) {
                        onConfirmSuggested()
                    } else if (value.isBlank()) {
                        onValueChange(defaultValue)
                    } else {
                        showManualEdit = true
                    }
                }
        )

        RepeatingIconButton(
            onClick = { adjust(-15) },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            icon = Icons.Default.Remove,
            contentDescription = "Meno"
        )

        if (value.isNotBlank()) {
            Text(
                "CANCELLA", 
                style = MaterialTheme.typography.labelSmall, 
                color = MaterialTheme.colorScheme.error, 
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp).clickable { onValueChange("") }
            )
        } else { 
            Spacer(modifier = Modifier.height(14.dp)) 
        }
    }
}

@Composable
fun RepeatingIconButton(
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String
) {
    val scope = rememberCoroutineScope()
    val currentOnClick by rememberUpdatedState(onClick)
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(width = 90.dp, height = 36.dp) // Altezza ridotta
            .background(
                if (isPressed) containerColor.copy(alpha = 0.7f) else containerColor,
                RoundedCornerShape(8.dp)
            )
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitFirstDown(requireUnconsumed = false)
                        isPressed = true
                        val job = scope.launch {
                            currentOnClick()
                            delay(450)
                            while (true) {
                                currentOnClick()
                                delay(120)
                            }
                        }
                        waitForUpOrCancellation()
                        job.cancel()
                        isPressed = false
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon, 
            contentDescription = contentDescription,
            modifier = Modifier.size(24.dp),
            tint = contentColor
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWorkerToDayDialog(
    availableWorkers: List<Worker>,
    existingLogs: List<WorkLog>,
    editingLog: WorkLog?,
    currentDate: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long, String, String, String, String, Long?) -> Unit
) {
    val selectableWorkers = remember(availableWorkers, existingLogs) {
        availableWorkers.filter { w -> existingLogs.none { it.workerId == w.id } }.sortedWith(compareBy({ it.surname }, { it.name }))
    }
    var selectedWorker by remember(editingLog) { mutableStateOf<Worker?>(editingLog?.let { log -> availableWorkers.find { it.id == log.workerId } } ?: if (selectableWorkers.size == 1) selectableWorkers.first() else null) }

    // Logica di inizializzazione "a cascata" per NUOVO o MODIFICA
    var morningStart by remember(editingLog) { mutableStateOf(editingLog?.morningStart ?: "08:00") }
    var morningEnd by remember(editingLog) { mutableStateOf(editingLog?.morningEnd ?: "") }
    var afternoonStart by remember(editingLog) { mutableStateOf(editingLog?.afternoonStart ?: "") }
    var afternoonEnd by remember(editingLog) { mutableStateOf(editingLog?.afternoonEnd ?: "") }

    var expandPeriod by remember { mutableStateOf(false) }
    var endDate by remember { mutableStateOf(currentDate) }
    var showRangeConfirmDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }

    if (showHelpDialog) {
        QuickHelpDialog(onDismiss = { showHelpDialog = false })
    }

    // Riferimenti per i suggerimenti (servono per il colore)
    var suggestedMorningEnd by remember { mutableStateOf("") }
    var suggestedAfternoonStart by remember { mutableStateOf("") }
    var suggestedAfternoonEnd by remember { mutableStateOf("") }

    if (showRangeConfirmDialog) {
        val daysCount = ((endDate - currentDate) / (24 * 60 * 60 * 1000)).toInt() + 1
        AlertDialog(
            onDismissRequest = { showRangeConfirmDialog = false },
            title = { Text("Inserimento orari per $daysCount giorni lavorativi.", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
            text = { Text("Attenzione: gli orari già presenti in questo periodo verranno sovrascritti. Vuoi continuare?") },
            confirmButton = {
                Button(onClick = {
                    showRangeConfirmDialog = false
                    selectedWorker?.let { onConfirm(it.id, morningStart, morningEnd, afternoonStart, afternoonEnd, endDate) }
                }) { Text("Conferma") }
            },
            dismissButton = {
                TextButton(onClick = { showRangeConfirmDialog = false }) { Text("Annulla") }
            }
        )
    }

    // Logica basata sulla PRESENZA - SOLO INGRESSO DI DIALOG
    LaunchedEffect(Unit) {
        // 1. Se è un NUOVO inserimento (Aggiungi), non suggeriamo NULLA
        if (editingLog == null) return@LaunchedEffect

        // 2. Se è una MODIFICA, applichiamo lo schema solo una volta all'apertura
        // Step 1: Solo Inizio presente -> Suggerisco 12:00
        if (morningStart.isNotBlank() && morningEnd.isBlank()) {
            morningEnd = "12:00"
            suggestedMorningEnd = "12:00"
        } 
        // Step 2: Inizio e Fine presenti -> Suggerisco 13:00
        else if (morningStart.isNotBlank() && morningEnd.isNotBlank() && afternoonStart.isBlank()) {
            afternoonStart = "13:00"
            suggestedAfternoonStart = "13:00"
        } 
        // Step 3: Mattina (I/F) e Inizio pomeriggio presenti -> Suggerisco 17:00
        else if (morningStart.isNotBlank() && morningEnd.isNotBlank() && afternoonStart.isNotBlank() && afternoonEnd.isBlank()) {
            afternoonEnd = "17:00"
            suggestedAfternoonEnd = "17:00"
        }
    }
    
    // RIMOSSI I LAUNCHED EFFECT CHE CAUSAVANO IL DOMINO

    var expanded by remember { mutableStateOf(editingLog == null && selectedWorker == null && selectableWorkers.size > 1) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun isTimeRangeValid(start: String, end: String): Boolean {
        if (start.isBlank() || end.isBlank()) return true
        return try {
            val sdf = SimpleDateFormat("HH:mm", Locale.ITALY)
            val s = sdf.parse(start)
            val e = sdf.parse(end)
            s != null && e != null && e.after(s)
        } catch (_: Exception) {
            false
        }
    }

    val isMorningValid = isTimeRangeValid(morningStart, morningEnd)
    val isAfternoonValid = isTimeRangeValid(afternoonStart, afternoonEnd)
    val isFormValid = selectedWorker != null && isMorningValid && isAfternoonValid && 
                     (morningStart.isNotBlank() || afternoonStart.isNotBlank())

    fun validateTimes(): Boolean {
        val sdf = SimpleDateFormat("HH:mm", Locale.ITALY)
        try {
            if (morningStart.isNotBlank() && morningEnd.isNotBlank()) {
                val start = sdf.parse(morningStart)
                val end = sdf.parse(morningEnd)
                if (start != null && end != null && !end.after(start)) {
                    errorMessage = "Fine mattina deve essere dopo l'inizio"
                    return false
                }
            }
            if (afternoonStart.isNotBlank() && afternoonEnd.isNotBlank()) {
                val start = sdf.parse(afternoonStart)
                val end = sdf.parse(afternoonEnd)
                if (start != null && end != null && !end.after(start)) {
                    errorMessage = "Fine pomeriggio deve essere dopo l'inizio"
                    return false
                }
            }
        } catch (_: Exception) {
            errorMessage = "Formato orario non valido"
            return false
        }
        errorMessage = null
        return true
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (editingLog == null) "Aggiungi Bracciante" else "Modifica Orari", 
                    style = MaterialTheme.typography.headlineSmall, 
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { showHelpDialog = true }) {
                    Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "Aiuto", tint = MaterialTheme.colorScheme.primary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (editingLog == null) {
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                        TextField(
                            value = selectedWorker?.let { "${it.surname} ${it.name}".trim() } ?: "Seleziona Bracciante", 
                            onValueChange = {}, 
                            readOnly = true, 
                            textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), 
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }, 
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            selectableWorkers.forEach { worker ->
                                DropdownMenuItem(
                                    text = { Text("${worker.surname} ${worker.name}".trim(), style = MaterialTheme.typography.titleMedium) }, 
                                    onClick = { 
                                        selectedWorker = worker
                                        expanded = false 
                                    }, 
                                    contentPadding = PaddingValues(12.dp)
                                )
                            }
                        }
                    }
                } else {
                    Text("Bracciante: ${selectedWorker?.surname} ${selectedWorker?.name}".trim(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                TimePickerSection(
                    label = "Mattina",
                    start = morningStart,
                    end = morningEnd,
                    onStartChange = { morningStart = it },
                    onEndChange = { morningEnd = it },
                    suggestedEnd = suggestedMorningEnd,
                    onConfirmSuggestedEnd = { suggestedMorningEnd = "" },
                    defaultStart = "08:00",
                    defaultEnd = "12:00"
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                TimePickerSection(
                    label = "Pomeriggio",
                    start = afternoonStart,
                    end = afternoonEnd,
                    onStartChange = { afternoonStart = it },
                    onEndChange = { afternoonEnd = it },
                    suggestedStart = suggestedAfternoonStart,
                    suggestedEnd = suggestedAfternoonEnd,
                    onConfirmSuggestedStart = { suggestedAfternoonStart = "" },
                    onConfirmSuggestedEnd = { suggestedAfternoonEnd = "" },
                    defaultStart = "13:00",
                    defaultEnd = "17:00"
                )
                if (errorMessage != null) {
                    Text(errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                
                ExpandPeriodSection(
                    isExpanded = expandPeriod,
                    onExpandedChange = { expandPeriod = it },
                    currentDate = currentDate,
                    endDate = endDate,
                    onEndDateChange = { endDate = it }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (validateTimes()) {
                    if (expandPeriod) {
                        showRangeConfirmDialog = true
                    } else {
                        selectedWorker?.let { onConfirm(it.id, morningStart, morningEnd, afternoonStart, afternoonEnd, null) }
                    }
                }
            }, enabled = isFormValid) { Text("Salva") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } }
    )
}

@Composable
fun ExpandPeriodSection(
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    currentDate: Long,
    endDate: Long,
    onEndDateChange: (Long) -> Unit
) {
    val sdf = SimpleDateFormat("EEE dd/MM/yyyy", Locale.ITALY)
    
    val daysCount = remember(currentDate, endDate) {
        val diff = endDate - currentDate
        (diff / (24 * 60 * 60 * 1000)).toInt() + 1
    }
    
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().clickable { onExpandedChange(!isExpanded) }
        ) {
            Checkbox(checked = isExpanded, onCheckedChange = onExpandedChange)
            Text("Espandi a più giorni", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        }
        
        if (isExpanded) {
            Column(modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)) {
                Text("Data fine periodo:", style = MaterialTheme.typography.labelMedium)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RepeatingIconButton(
                        onClick = {
                            val cal = Calendar.getInstance().apply { 
                                timeInMillis = endDate
                                add(Calendar.DAY_OF_YEAR, -1)
                            }
                            if (cal.timeInMillis >= currentDate) onEndDateChange(cal.timeInMillis)
                        },
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        icon = Icons.Default.Remove,
                        contentDescription = "Meno un giorno"
                    )
                    
                    Text(
                        text = sdf.format(Date(endDate)).replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.widthIn(min = 120.dp),
                        textAlign = TextAlign.Center
                    )
                    
                    RepeatingIconButton(
                        onClick = {
                            val cal = Calendar.getInstance().apply { 
                                timeInMillis = endDate
                                add(Calendar.DAY_OF_YEAR, 1)
                            }
                            onEndDateChange(cal.timeInMillis)
                        },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        icon = Icons.Default.Add,
                        contentDescription = "Più un giorno"
                    )
                }
                
                Text(
                    text = "Totale: $daysCount giorni",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Gli orari esistenti nel periodo verranno sovrascritti.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
