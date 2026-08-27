package com.example.gestbraccianti.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.res.stringResource
import com.example.gestbraccianti.R
import com.example.gestbraccianti.data.entity.WorkLog
import com.example.gestbraccianti.data.entity.Worker
import com.example.gestbraccianti.data.entity.WorkerGroup
import com.example.gestbraccianti.ui.components.SmallStatChip
import com.example.gestbraccianti.ui.viewmodel.WorkLogViewModel
import com.example.gestbraccianti.ui.viewmodel.WorkerGroupViewModel
import com.example.gestbraccianti.ui.viewmodel.WorkerViewModel
import com.example.gestbraccianti.ui.utils.TimeUtils
import com.example.gestbraccianti.ui.utils.formatHours
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.ui.input.pointer.pointerInput
import java.text.SimpleDateFormat
import androidx.compose.material.icons.filled.Warning
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WorkDayDetailScreen(
    date: Long,
    yearId: Int,
    workLogViewModel: WorkLogViewModel,
    workerViewModel: WorkerViewModel,
    groupViewModel: WorkerGroupViewModel,
    onBack: () -> Unit,
    onShowHelp: (String) -> Unit
) {
    val allLogs by workLogViewModel.allLogs.collectAsState()
    val logsForDay = remember(allLogs, date) { allLogs.filter { it.date == date } }
    val workers by workerViewModel.workersForCurrentYear.collectAsState()
    val groups by groupViewModel.groupsForYear.collectAsState()
    val scope = rememberCoroutineScope()
    
    var showAddWorkerDialog by remember { mutableStateOf(false) }
    var showAddGroupDialog by remember { mutableStateOf(false) }
    var editingLog by remember { mutableStateOf<WorkLog?>(null) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        workLogViewModel.uiEvent.collect { message ->
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
        }
    }

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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_desc))
                }
                Text(
                    text = TimeUtils.format(date, TimeUtils.fullDateFormatter).replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp).weight(1f)
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            if (logsForDay.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.no_workers_today), style = MaterialTheme.typography.titleMedium)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val sortedLogs = logsForDay.sortedWith(
                        compareByDescending<WorkLog> { it.date }
                            .thenBy { log ->
                                val worker = workers.find { it.id == log.workerId }
                                "${worker?.surname} ${worker?.name}"
                            }
                    )
                    itemsIndexed(sortedLogs, key = { _, log: WorkLog -> log.id }) { index, log: WorkLog ->
                        val worker = workers.find { it.id == log.workerId }
                        val cardBg = if (index % 2 == 0) MaterialTheme.colorScheme.surface 
                                     else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        editingLog = log
                                        showAddWorkerDialog = true
                                    },
                                    onLongClick = {
                                        workLogViewModel.toggleManualHoliday(log)
                                        val status = if (!log.isManualHoliday) context.getString(R.string.manual_holiday_toggle_toast_on) 
                                                     else context.getString(R.string.manual_holiday_toggle_toast_off)
                                        Toast.makeText(context, status.format("${worker?.surname} ${worker?.name}"), Toast.LENGTH_SHORT).show()
                                    }
                                ),
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            border = if (index == 0) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                     else null,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    modifier = Modifier.size(48.dp),
                                    shape = MaterialTheme.shapes.small,
                                    color = if (log.isManualHoliday) MaterialTheme.colorScheme.tertiaryContainer 
                                            else if (index % 2 == 0) MaterialTheme.colorScheme.secondaryContainer 
                                            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = worker?.surname?.take(1)?.uppercase() ?: "?",
                                            style = MaterialTheme.typography.titleLarge,
                                            color = if (log.isManualHoliday) MaterialTheme.colorScheme.onTertiaryContainer 
                                                    else if (index % 2 == 0) MaterialTheme.colorScheme.onSecondaryContainer
                                                    else MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${worker?.surname ?: ""} ${worker?.name ?: ""}".trim(),
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (log.isManualHoliday) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                                shape = MaterialTheme.shapes.extraSmall,
                                                modifier = Modifier.padding(start = 4.dp)
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.holiday_indicator),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                                    fontWeight = FontWeight.Black
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        text = stringResource(R.string.total_hours_short, formatHours(log.totalHours)),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (log.holidayHours > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                                    )
                                    if (log.ordinaryHours > 0 || log.extraHours > 0 || log.holidayHours > 0) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            if (log.ordinaryHours > 0) {
                                                SmallStatChip(label = "Ord", hours = log.ordinaryHours, color = MaterialTheme.colorScheme.primary)
                                            }
                                            if (log.extraHours > 0) {
                                                SmallStatChip(label = "STR", hours = log.extraHours, color = MaterialTheme.colorScheme.secondary)
                                            }
                                            if (log.holidayHours > 0) {
                                                SmallStatChip(label = "fest", hours = log.holidayHours, color = MaterialTheme.colorScheme.tertiary)
                                            }
                                        }
                                    }
                                }
                                IconButton(onClick = { workLogViewModel.deleteLog(log) }) {
                                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.remove_desc), tint = MaterialTheme.colorScheme.error)
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
                    text = { Text(stringResource(R.string.btn_groups), fontWeight = FontWeight.Bold) }
                )
                ExtendedFloatingActionButton(
                    onClick = {
                        editingLog = null
                        showAddWorkerDialog = true
                    },
                    modifier = Modifier.weight(1f),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                    text = { Text(stringResource(R.string.btn_worker), fontWeight = FontWeight.Bold) }
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
            },
            onShowHelp = onShowHelp
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
            },
            onShowHelp = onShowHelp
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddGroupToDayDialog(
    groups: List<WorkerGroup>,
    existingLogs: List<WorkLog>,
    currentDate: Long,
    onDismiss: () -> Unit,
    onConfirm: (WorkerGroup, String, String, String, String, Long?) -> Unit,
    onShowHelp: (String) -> Unit
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

    // Riferimenti per i suggerimenti (servono per il colore)
    var suggestedMorningEnd by remember { mutableStateOf("") }
    var suggestedAfternoonStart by remember { mutableStateOf("") }
    var suggestedAfternoonEnd by remember { mutableStateOf("") }

    if (showRangeConfirmDialog) {
        val daysCount = ((endDate - currentDate) / (24 * 60 * 60 * 1000)).toInt() + 1
        AlertDialog(
            onDismissRequest = { showRangeConfirmDialog = false },
            title = { Text(stringResource(R.string.range_confirm_title, daysCount), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.range_confirm_text)) },
            confirmButton = {
                Button(onClick = {
                    showRangeConfirmDialog = false
                    selectedGroup?.let { onConfirm(it, morningStart, morningEnd, afternoonStart, afternoonEnd, endDate) }
                }) { Text(stringResource(R.string.confirm_btn)) }
            },
            dismissButton = {
                TextButton(onClick = { showRangeConfirmDialog = false }) { Text(stringResource(R.string.cancel_btn)) }
            }
        )
    }

    // Logica basata sulla PRESENZA - Solo all'ingresso (per i gruppi è sempre "nuovo")
    LaunchedEffect(Unit) {
        // In "Aggiungi Gruppo" non suggeriamo mai nulla all'ingresso (morningEnd resta vuoto)
        // E non ci sono LaunchedEffect che ascoltano le modifiche manuali, quindi non succederà nulla dopo.
    }
    var expanded by remember { mutableStateOf(selectedGroup == null && groups.size > 1) }
    var errorMessage by remember { mutableStateOf<Int?>(null) }

    fun isTimeRangeValid(start: String, end: String): Boolean {
        if (start.isBlank() || end.isBlank()) return true
        return try {
            val sdf = TimeUtils.timeFormatter
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
            errorMessage = R.string.error_morning_range
            return false
        }
        if (!isAfternoonValid) {
            errorMessage = R.string.error_afternoon_range
            return false
        }
        if (morningStart.isBlank() && afternoonStart.isBlank()) {
            errorMessage = R.string.error_start_required
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
                    text = stringResource(R.string.add_group_title), 
                    style = MaterialTheme.typography.headlineSmall, 
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { onShowHelp("modifica_orari") }) {
                    Icon(Icons.Default.HelpOutline, contentDescription = stringResource(R.string.help_desc), tint = MaterialTheme.colorScheme.primary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = selectedGroup?.name ?: stringResource(R.string.select_group_hint),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.group_name_label), style = MaterialTheme.typography.labelLarge) },
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
                    label = stringResource(R.string.morning_label),
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
                    label = stringResource(R.string.afternoon_label),
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
                errorMessage?.let {
                    Text(stringResource(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
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
            }, enabled = isFormValid) { Text(stringResource(R.string.btn_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel_btn)) } }
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
                Text(stringResource(R.string.start_label), style = MaterialTheme.typography.labelMedium)
                TactileTimePicker(
                    value = start,
                    defaultValue = defaultStart,
                    onValueChange = onStartChange,
                    isSuggested = suggestedStart.isNotBlank() && start == suggestedStart,
                    onConfirmSuggested = onConfirmSuggestedStart
                )
            }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.end_label), style = MaterialTheme.typography.labelMedium)
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
            title = { Text(stringResource(R.string.manual_entry_title)) },
            text = {
                OutlinedTextField(
                    value = tempTime, 
                    onValueChange = { tempTime = it }, 
                    placeholder = { Text(stringResource(R.string.time_hint)) }, 
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
                }) { Text(stringResource(R.string.ok_btn)) }
            },
            dismissButton = { TextButton(onClick = { showManualEdit = false }) { Text(stringResource(R.string.cancel_btn)) } }
        )
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        RepeatingIconButton(
            onClick = { adjust(15) },
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            icon = Icons.Default.Add,
            contentDescription = stringResource(R.string.plus_desc)
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
            contentDescription = stringResource(R.string.minus_desc)
        )

        if (value.isNotBlank()) {
            Text(
                stringResource(R.string.clear_btn),
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
private fun AddWorkerToDayDialog(
    availableWorkers: List<Worker>,
    existingLogs: List<WorkLog>,
    editingLog: WorkLog?,
    currentDate: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long, String, String, String, String, Long?) -> Unit,
    onShowHelp: (String) -> Unit
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

    // Riferimenti per i suggerimenti (servono per il colore)
    var suggestedMorningEnd by remember { mutableStateOf("") }
    var suggestedAfternoonStart by remember { mutableStateOf("") }
    var suggestedAfternoonEnd by remember { mutableStateOf("") }

    if (showRangeConfirmDialog) {
        val daysCount = ((endDate - currentDate) / (24 * 60 * 60 * 1000)).toInt() + 1
        AlertDialog(
            onDismissRequest = { showRangeConfirmDialog = false },
            title = { Text(stringResource(R.string.range_confirm_title, daysCount), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.range_confirm_text)) },
            confirmButton = {
                Button(onClick = {
                    showRangeConfirmDialog = false
                    selectedWorker?.let { onConfirm(it.id, morningStart, morningEnd, afternoonStart, afternoonEnd, endDate) }
                }) { Text(stringResource(R.string.confirm_btn)) }
            },
            dismissButton = {
                TextButton(onClick = { showRangeConfirmDialog = false }) { Text(stringResource(R.string.cancel_btn)) }
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
    var errorMessage by remember { mutableStateOf<Int?>(null) }

    fun isTimeRangeValid(start: String, end: String): Boolean {
        if (start.isBlank() || end.isBlank()) return true
        return try {
            val sdf = TimeUtils.timeFormatter
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
        val sdf = TimeUtils.timeFormatter
        try {
            if (morningStart.isNotBlank() && morningEnd.isNotBlank()) {
                val start = sdf.parse(morningStart)
                val end = sdf.parse(morningEnd)
                if (start != null && end != null && !end.after(start)) {
                    errorMessage = R.string.error_morning_range
                    return false
                }
            }
            if (afternoonStart.isNotBlank() && afternoonEnd.isNotBlank()) {
                val start = sdf.parse(afternoonStart)
                val end = sdf.parse(afternoonEnd)
                if (start != null && end != null && !end.after(start)) {
                    errorMessage = R.string.error_afternoon_range
                    return false
                }
            }
        } catch (_: Exception) {
            errorMessage = R.string.error_invalid_time
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
                    text = stringResource(if (editingLog == null) R.string.add_worker_desc else R.string.edit_hours_title), 
                    style = MaterialTheme.typography.headlineSmall, 
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { onShowHelp("modifica_orari") }) {
                    Icon(Icons.Default.HelpOutline, contentDescription = stringResource(R.string.help_desc), tint = MaterialTheme.colorScheme.primary)
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
                        OutlinedTextField(
                            value = selectedWorker?.let { "${it.surname} ${it.name}".trim() } ?: stringResource(R.string.select_worker_hint), 
                            onValueChange = {}, 
                            readOnly = true, 
                            label = { Text(stringResource(R.string.chip_workers), style = MaterialTheme.typography.labelLarge) },
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
                    Text(stringResource(R.string.worker_label_prefix, "${selectedWorker?.surname} ${selectedWorker?.name}".trim()), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                TimePickerSection(
                    label = stringResource(R.string.morning_label),
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
                    label = stringResource(R.string.afternoon_label),
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
                errorMessage?.let {
                    Text(stringResource(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
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
            }, enabled = isFormValid) { Text(stringResource(R.string.btn_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel_btn)) } }
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
            Text(stringResource(R.string.expand_period_label), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        }
        
        if (isExpanded) {
            Column(modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)) {
                Text(stringResource(R.string.end_date_label), style = MaterialTheme.typography.labelMedium)
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
                        contentDescription = stringResource(R.string.prev_day_desc)
                    )
                    
                    Text(
                        text = TimeUtils.format(endDate, TimeUtils.shortDateDayFormatter).replaceFirstChar { it.uppercase() },
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
                        contentDescription = stringResource(R.string.next_day_desc)
                    )
                }
                
                Text(
                    text = stringResource(R.string.total_days_label, daysCount),
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
                        stringResource(R.string.overwrite_warning),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
