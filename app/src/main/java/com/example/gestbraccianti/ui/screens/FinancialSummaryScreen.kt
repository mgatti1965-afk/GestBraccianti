package com.example.gestbraccianti.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.gestbraccianti.R
import com.example.gestbraccianti.data.entity.WorkLog
import com.example.gestbraccianti.data.model.WorkerYearStats
import com.example.gestbraccianti.ui.components.SmallStatChip
import com.example.gestbraccianti.ui.utils.*
import com.example.gestbraccianti.ui.viewmodel.WorkLogViewModel
import com.example.gestbraccianti.ui.viewmodel.WorkerGroupViewModel
import kotlinx.coroutines.flow.first
import java.io.File
import java.util.*

enum class GroupingType { BY_WORKER, BY_GROUP }
enum class ViewMode { DETAIL, TOTALS }

data class AggregatedSummary(
    val title: String,
    val period: String,
    val totalHours: Double,
    val totalEarnings: Double,
    val ordHours: Double = 0.0,
    val extHours: Double = 0.0,
    val holHours: Double = 0.0,
    val ordAmount: Double = 0.0,
    val extAmount: Double = 0.0,
    val holAmount: Double = 0.0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialSummaryScreen(viewModel: WorkLogViewModel, groupViewModel: WorkerGroupViewModel) {
    val stats by viewModel.yearlyStats.collectAsState()
    val filteredLogs by viewModel.filteredLogs.collectAsState()
    val referenceDate by viewModel.currentReferenceDate.collectAsState()
    val groups by groupViewModel.groupsForYear.collectAsState()
    var selectedFilter by remember { mutableIntStateOf(0) }
    val filters = listOf(
        stringResource(R.string.filter_year),
        stringResource(R.string.filter_month),
        stringResource(R.string.filter_week),
        stringResource(R.string.filter_day)
    )
    
    var groupingType by remember { mutableStateOf(GroupingType.BY_WORKER) }
    var viewModeForWorker by remember { mutableStateOf(ViewMode.DETAIL) }
    val effectiveViewMode = if (groupingType == GroupingType.BY_GROUP) ViewMode.TOTALS else viewModeForWorker
    var groupToWorkers by remember { mutableStateOf<Map<Long, List<Long>>>(emptyMap()) }

    LaunchedEffect(groups) {
        val map = mutableMapOf<Long, List<Long>>()
        groups.forEach { group ->
            val workers = groupViewModel.getWorkersInGroup(group.id).first()
            map[group.id] = workers.map { it.id }
        }
        groupToWorkers = map
    }

    val context = LocalContext.current
    
    var showReportDialog by remember { mutableStateOf(false) }
    var selectedWorkerIdForReport by remember { mutableStateOf<Long?>(null) }
    var selectedLogForDetail by remember { mutableStateOf<WorkLog?>(null) }
    var selectedTotalForDetail by remember { mutableStateOf<AggregatedSummary?>(null) }

    if (selectedLogForDetail != null) {
        WorkLogDetailDialog(
            log = selectedLogForDetail!!,
            workerName = stats.find { it.workerId == selectedLogForDetail!!.workerId }?.let { "${it.surname} ${it.name}" } ?: "Bracciante",
            onDismiss = { selectedLogForDetail = null }
        )
    }

    if (selectedTotalForDetail != null) {
        AggregatedSummaryDialog(
            summary = selectedTotalForDetail!!,
            onDismiss = { selectedTotalForDetail = null }
        )
    }

    LaunchedEffect(selectedFilter, referenceDate) {
        val calendar = Calendar.getInstance(Locale.ITALY)
        calendar.timeInMillis = referenceDate
        
        when (selectedFilter) {
            0 -> viewModel.setDateRange(null, null)
            1 -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                calendar.add(Calendar.MONTH, 1)
                calendar.add(Calendar.MILLISECOND, -1)
                viewModel.setDateRange(start, calendar.timeInMillis)
            }
            2 -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                calendar.add(Calendar.WEEK_OF_YEAR, 1)
                calendar.add(Calendar.MILLISECOND, -1)
                viewModel.setDateRange(start, calendar.timeInMillis)
            }
            3 -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                calendar.add(Calendar.MILLISECOND, -1)
                viewModel.setDateRange(start, calendar.timeInMillis)
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            if (filteredLogs.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { 
                        selectedWorkerIdForReport = null
                        showReportDialog = true 
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = stringResource(R.string.export_pdf_desc))
                }
            }
        }
    ) { innerPadding ->
        if (showReportDialog) {
            AlertDialog(
                onDismissRequest = { showReportDialog = false },
                title = { 
                    Text(
                        text = stringResource(R.string.pdf_report_dialog_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (groupingType == GroupingType.BY_GROUP) {
                            Text(
                                text = stringResource(R.string.pdf_group_desc),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.pdf_select_workers_label),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedWorkerIdForReport = null }
                                        .background(
                                            if (selectedWorkerIdForReport == null) MaterialTheme.colorScheme.primaryContainer 
                                            else MaterialTheme.colorScheme.surface,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            1.dp, 
                                            if (selectedWorkerIdForReport == null) MaterialTheme.colorScheme.primary 
                                            else MaterialTheme.colorScheme.outline,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(12.dp)
                                ) {
                                    RadioButton(
                                        selected = selectedWorkerIdForReport == null,
                                        onClick = { selectedWorkerIdForReport = null }
                                    )
                                    Text(stringResource(R.string.pdf_all_workers), style = MaterialTheme.typography.bodyLarge)
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                
                                val workersInPeriod = remember(filteredLogs, stats) {
                                    filteredLogs.map { it.workerId }.distinct().sortedBy { wId ->
                                        val w = stats.find { it.workerId == wId }
                                        "${w?.surname} ${w?.name}"
                                    }
                                }
                                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                                    items(workersInPeriod, key = { it }) { wId ->
                                        val w = stats.find { it.workerId == wId }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 2.dp)
                                                .clickable { selectedWorkerIdForReport = wId }
                                                .background(
                                                    if (selectedWorkerIdForReport == wId) MaterialTheme.colorScheme.primaryContainer 
                                                    else MaterialTheme.colorScheme.surface,
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .border(
                                                    1.dp,
                                                    if (selectedWorkerIdForReport == wId) MaterialTheme.colorScheme.primary 
                                                    else MaterialTheme.colorScheme.outline,
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .padding(8.dp)
                                        ) {
                                            RadioButton(
                                                selected = selectedWorkerIdForReport == wId,
                                                onClick = { selectedWorkerIdForReport = wId }
                                            )
                                            Text("${w?.surname ?: ""} ${w?.name ?: "Bracc. $wId"}")
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showReportDialog = false
                            val logsToExport = if (selectedWorkerIdForReport == null) {
                                filteredLogs
                            } else {
                                filteredLogs.filter { it.workerId == selectedWorkerIdForReport }
                            }
                            
                            val pdfFile = generatePdfReport(
                                context = context,
                                logs = logsToExport,
                                yearStats = stats,
                                filterTitle = filters[selectedFilter],
                                referenceDate = referenceDate,
                                groupingType = if (selectedWorkerIdForReport != null) GroupingType.BY_WORKER else groupingType,
                                viewMode = effectiveViewMode,
                                groups = groups,
                                groupToWorkers = groupToWorkers
                            )
                            if (pdfFile != null) sharePdf(context, pdfFile)
                        }
                    ) {
                        Text(stringResource(R.string.btn_generate_pdf))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showReportDialog = false }) { Text(stringResource(R.string.cancel_btn)) }
                }
            )
        }
        Column(modifier = Modifier.padding(innerPadding)) {
            ScrollableTabRow(
                selectedTabIndex = selectedFilter,
                edgePadding = 16.dp,
                divider = {}
            ) {
                filters.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedFilter == index,
                        onClick = { selectedFilter = index },
                        text = { Text(title) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PeriodNavigation(
                    selectedFilter = selectedFilter,
                    referenceDate = referenceDate,
                    onPrev = { viewModel.moveReferenceDate(selectedFilter, -1) },
                    onNext = { viewModel.moveReferenceDate(selectedFilter, 1) },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Selezione Raggruppamento (Bracc / Grp)
                // Colore Blu Elettrico più saturo e vivo
                val groupColor = Color(0xFF1E88E5)
                
                Button(
                    onClick = { groupingType = GroupingType.BY_WORKER },
                    enabled = groupingType != GroupingType.BY_WORKER,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = groupColor,
                        contentColor = Color.White,
                        disabledContainerColor = groupColor.copy(alpha = 0.15f),
                        disabledContentColor = groupColor.copy(alpha = 0.9f)
                    ),
                    shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(stringResource(R.string.chip_workers), style = MaterialTheme.typography.labelLarge)
                }

                Button(
                    onClick = { 
                        groupingType = GroupingType.BY_GROUP
                    },
                    enabled = groupingType != GroupingType.BY_GROUP,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = groupColor,
                        contentColor = Color.White,
                        disabledContainerColor = groupColor.copy(alpha = 0.15f),
                        disabledContentColor = groupColor.copy(alpha = 0.9f)
                    ),
                    shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(stringResource(R.string.chip_groups), style = MaterialTheme.typography.labelLarge)
                }
                
                Spacer(modifier = Modifier.weight(1f))

                // Selezione Visualizzazione (Dett / Tot)
                // Colore Arancio Zucca vibrante
                val modeColor = Color(0xFFF4511E)
                val detailAvailable = groupingType == GroupingType.BY_WORKER
                
                Button(
                    onClick = { viewModeForWorker = ViewMode.DETAIL },
                    enabled = detailAvailable && viewModeForWorker != ViewMode.DETAIL,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = modeColor,
                        contentColor = Color.White,
                        disabledContainerColor = if (detailAvailable) modeColor.copy(alpha = 0.15f) 
                                                 else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        disabledContentColor = if (detailAvailable) modeColor.copy(alpha = 0.9f)
                                               else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(stringResource(R.string.chip_detail), style = MaterialTheme.typography.labelLarge)
                }

                Button(
                    onClick = { viewModeForWorker = ViewMode.TOTALS },
                    enabled = (groupingType == GroupingType.BY_WORKER && viewModeForWorker != ViewMode.TOTALS),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = modeColor,
                        contentColor = Color.White,
                        disabledContainerColor = modeColor.copy(alpha = 0.15f),
                        disabledContentColor = modeColor.copy(alpha = 0.9f)
                    ),
                    shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(stringResource(R.string.chip_totals), style = MaterialTheme.typography.labelLarge)
                }

            }

            Box(modifier = Modifier.weight(1f)) {
                if (filteredLogs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (selectedFilter == 0) stringResource(R.string.no_data_year)
                            else stringResource(R.string.no_data_selection),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    GroupedFinancialView(
                        logs = filteredLogs,
                        yearStats = stats,
                        groupingType = groupingType,
                        viewMode = effectiveViewMode,
                        groups = groups,
                        groupToWorkers = groupToWorkers,
                        selectedFilter = selectedFilter,
                        onLogClick = { selectedLogForDetail = it },
                        onTotalClick = { selectedTotalForDetail = it }
                    )
                }
            }
        }
    }
}

@Composable
fun GroupedFinancialView(
    logs: List<WorkLog>,
    yearStats: List<WorkerYearStats>,
    groupingType: GroupingType,
    viewMode: ViewMode,
    groups: List<com.example.gestbraccianti.data.entity.WorkerGroup>,
    groupToWorkers: Map<Long, List<Long>>,
    selectedFilter: Int,
    onLogClick: (WorkLog) -> Unit,
    onTotalClick: (AggregatedSummary) -> Unit
) {
    val workerMap = remember(yearStats) { yearStats.associateBy { it.workerId } }
    val calendar = remember { Calendar.getInstance(Locale.ITALY) }

    val monthlyLogs = remember(logs, workerMap) {
        logs.groupBy { log ->
            calendar.timeInMillis = log.date
            calendar.get(Calendar.MONTH)
        }.mapValues { (_, mLogs) ->
            val sorted = mLogs.sortedWith(
                compareByDescending<WorkLog> { it.date }
                    .thenBy { log ->
                        val w = workerMap[log.workerId]
                        "${w?.surname} ${w?.name}"
                    }
            )
            val indices = sorted.map { it.date }.distinct().withIndex().associate { it.value to it.index }
            sorted to indices
        }.toSortedMap(compareByDescending { it })
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        monthlyLogs.forEach { (monthIdx, data) ->
            val mLogs = data.first
            val dayIndices = data.second
            val monthName = TimeUtils.formatMonth(mLogs.first().date)

            item {
                Column(modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)) {
                    Text(
                        text = monthName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(top = 4.dp),
                        thickness = 2.dp,
                        color = MaterialTheme.colorScheme.primaryContainer
                    )
                }
            }
            
            val workerTotals = if (viewMode == ViewMode.TOTALS && groupingType == GroupingType.BY_WORKER) {
                mLogs.groupBy { it.workerId }.map { (wId, wLogs) ->
                    val hours = wLogs.sumOf { it.totalHours }
                    val earnings = wLogs.sumOf { it.totalAmount }
                    Triple(wId, hours, earnings)
                }.sortedBy { (wId, _, _) ->
                    val w = workerMap[wId]
                    "${w?.surname} ${w?.name}"
                }
            } else emptyList()

            val groupTotals = if (groupingType == GroupingType.BY_GROUP) {
                val result = mutableListOf<Triple<Long?, Double, Double>>()
                groups.forEach { group ->
                    val workersInGroup = groupToWorkers[group.id] ?: emptyList()
                    val gLogs = mLogs.filter { it.workerId in workersInGroup }
                    if (gLogs.isNotEmpty()) {
                        val hours = gLogs.sumOf { it.totalHours }
                        val earnings = gLogs.sumOf { it.totalAmount }
                        result.add(Triple(group.id, hours, earnings))
                    }
                }
                val allGroupWorkers = groupToWorkers.values.flatten().toSet()
                val noGroupLogs = mLogs.filter { it.workerId !in allGroupWorkers }
                if (noGroupLogs.isNotEmpty()) {
                    val hours = noGroupLogs.sumOf { it.totalHours }
                    val earnings = noGroupLogs.sumOf { it.totalAmount }
                    result.add(Triple(null, hours, earnings))
                }
                result.sortedBy { (gId, _, _) ->
                    if (gId == null) "" else groups.find { it.id == gId }?.name ?: ""
                }
            } else emptyList()

            if (groupingType == GroupingType.BY_GROUP) {
                itemsIndexed(groupTotals, key = { _, it -> "g_${monthIdx}_${it.first ?: -1}" }) { index, (gId, hours, earnings) ->
                    val groupName = groups.find { it.id == gId }?.name ?: stringResource(R.string.no_group_label)
                    
                    val workersInGroup = if (gId != null) groupToWorkers[gId] ?: emptyList() 
                                         else mLogs.map { it.workerId }.distinct().filter { wId -> 
                                             groupToWorkers.values.none { wId in it } 
                                         }
                    val gLogs = mLogs.filter { it.workerId in workersInGroup }
                    
                    var gOrdH = 0.0; var gExtH = 0.0; var gHolH = 0.0
                    var gOrdA = 0.0; var gExtA = 0.0; var gHolA = 0.0
                    
                    gLogs.forEach { log ->
                        gOrdH += log.ordinaryHours
                        gExtH += log.extraHours
                        gHolH += log.holidayHours
                        gOrdA += log.ordinaryAmount
                        gExtA += log.extraAmount
                        gHolA += log.holidayAmount
                    }

                    val cardBg = if (index % 2 == 0) MaterialTheme.colorScheme.surface 
                                 else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    val border = if (index == 0) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)) 
                                 else null

                    SummaryCard(
                        title = groupName,
                        subtitle = stringResource(R.string.group_total_subtitle),
                        hours = hours,
                        earnings = earnings,
                        ordH = gOrdH, extH = gExtH, holH = gHolH,
                        ordA = gOrdA, extA = gExtA, holA = gHolA,
                        containerColor = cardBg,
                        border = border,
                        onClick = { onTotalClick(AggregatedSummary(groupName, monthName, hours, earnings, gOrdH, gExtH, gHolH, gOrdA, gExtA, gHolA)) }
                    )
                }
            } else if (viewMode == ViewMode.TOTALS) {
                itemsIndexed(workerTotals, key = { _, it -> "w_${monthIdx}_${it.first}" }) { index, (wId, hours, earnings) ->
                    val worker = workerMap[wId]
                    val workerName = "${worker?.surname ?: ""} ${worker?.name ?: "Bracc. $wId"}"
                    
                    val wLogs = mLogs.filter { it.workerId == wId }
                    var wOrdH = 0.0; var wExtH = 0.0; var wHolH = 0.0
                    var wOrdA = 0.0; var wExtA = 0.0; var wHolA = 0.0
                    
                    wLogs.forEach { log ->
                        wOrdH += log.ordinaryHours
                        wExtH += log.extraHours
                        wHolH += log.holidayHours
                        wOrdA += log.ordinaryAmount
                        wExtA += log.extraAmount
                        wHolA += log.holidayAmount
                    }

                    val cardBg = if (index % 2 == 0) MaterialTheme.colorScheme.surface 
                                 else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    val border = if (index == 0) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)) 
                                 else null

                    SummaryCard(
                        title = workerName,
                        subtitle = stringResource(R.string.worker_total_subtitle),
                        hours = hours,
                        earnings = earnings,
                        ordH = wOrdH, extH = wExtH, holH = wHolH,
                        ordA = wOrdA, extA = wExtA, holA = wHolA,
                        containerColor = cardBg,
                        border = border,
                        onClick = { onTotalClick(AggregatedSummary(workerName, monthName, hours, earnings, wOrdH, wExtH, wHolH, wOrdA, wExtA, wHolA)) }
                    )
                }
            } else {
                itemsIndexed(mLogs, key = { _, log -> "${log.workerId}_${log.date}" }) { index, log ->
                    val worker = workerMap[log.workerId]
                    val earnings = log.totalAmount
                    val dayIndex = dayIndices[log.date] ?: 0
                    val isNewDay = index == 0 || mLogs[index - 1].date != log.date
                    
                    val cardBg = if (dayIndex % 2 == 0) MaterialTheme.colorScheme.surface 
                                 else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)

                    Column {
                        if (isNewDay && index > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onLogClick(log) },
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            border = if (index == 0) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)) 
                                     else null,
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = if (log.isManualHoliday) MaterialTheme.colorScheme.tertiaryContainer 
                                                else if (dayIndex % 2 == 0) MaterialTheme.colorScheme.secondaryContainer 
                                                else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                                        shape = MaterialTheme.shapes.small
                                    ) {
                                        Text(
                                            text = TimeUtils.format(log.date, TimeUtils.dayMonthFormatter),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (log.isManualHoliday) MaterialTheme.colorScheme.onTertiaryContainer 
                                                    else if (dayIndex % 2 == 0) MaterialTheme.colorScheme.onSecondaryContainer
                                                    else MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${worker?.surname ?: ""} ${worker?.name ?: "Bracciante ${log.workerId}"}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = formatHours(log.totalHours),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = formatCurrency(earnings),
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = if (log.isManualHoliday) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                
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
                        }
                    }
                }
            }
            
            item {
                val mHours = mLogs.sumOf { it.totalHours }
                val mEarnings = mLogs.sumOf { it.totalAmount }
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 16.dp)
                ) {
                    HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Row(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val totalLabel = when (selectedFilter) {
                            2 -> stringResource(R.string.total_weekly)
                            3 -> stringResource(R.string.total_daily)
                            else -> stringResource(R.string.total_monthly)
                        }
                        Text(
                            totalLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                formatHours(mHours),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = formatCurrency(mEarnings),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
        
        item {
            var totalOrdHours = 0.0
            var totalExtHours = 0.0
            var totalHolHours = 0.0
            var totalOrdAmt = 0.0
            var totalExtAmt = 0.0
            var totalHolAmt = 0.0

            logs.forEach { log ->
                totalOrdHours += log.ordinaryHours
                totalExtHours += log.extraHours
                totalHolHours += log.holidayHours
                totalOrdAmt += log.ordinaryAmount
                totalExtAmt += log.extraAmount
                totalHolAmt += log.holidayAmount
            }

            val yHours = totalOrdHours + totalExtHours + totalHolHours
            val yEarnings = totalOrdAmt + totalExtAmt + totalHolAmt
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = stringResource(R.string.overall_summary_title),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth()) {
                        // Colonna Ore
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.AccessTime,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    stringResource(R.string.total_hours_label),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }
                            Text(
                                text = formatHours(yHours),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        
                        VerticalDivider(
                            modifier = Modifier.height(48.dp).padding(horizontal = 16.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                        )
                        
                        Column(modifier = Modifier.weight(1.2f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Payments,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    stringResource(R.string.total_amount_label),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }
                            Text(
                                text = formatCurrency(yEarnings),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(16.dp))

                    // Dettaglio suddivisione
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SummaryBreakdownRow(
                            label = stringResource(R.string.total_ordinary_label),
                            hours = totalOrdHours,
                            amount = totalOrdAmt
                        )
                        SummaryBreakdownRow(
                            label = stringResource(R.string.total_extra_label),
                            hours = totalExtHours,
                            amount = totalExtAmt
                        )
                        SummaryBreakdownRow(
                            label = stringResource(R.string.total_holiday_label),
                            hours = totalHolHours,
                            amount = totalHolAmt
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryBreakdownRow(label: String, hours: Double, amount: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = formatHours(hours),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = formatCurrency(amount),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun SummaryCard(
    title: String,
    subtitle: String,
    hours: Double,
    earnings: Double,
    ordH: Double = 0.0,
    extH: Double = 0.0,
    holH: Double = 0.0,
    ordA: Double = 0.0,
    extA: Double = 0.0,
    holA: Double = 0.0,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    border: BorderStroke? = null,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = border,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatHours(hours),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatCurrency(earnings),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            if (ordH > 0 || extH > 0 || holH > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (ordH > 0) {
                        SmallStatChip(label = "Ord", hours = ordH, color = MaterialTheme.colorScheme.primary)
                    }
                    if (extH > 0) {
                        SmallStatChip(label = "STR", hours = extH, color = MaterialTheme.colorScheme.secondary)
                    }
                    if (holH > 0) {
                        SmallStatChip(label = "fest", hours = holH, color = MaterialTheme.colorScheme.tertiary)
                    }
                }
            }
        }
    }
}

fun sharePdf(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.btn_send_pdf)))
}

@Composable
fun DynamicCalendarIcon(date: Long) {
    val calendar = Calendar.getInstance(Locale.ITALY).apply { timeInMillis = date }
    val day = calendar.get(Calendar.DAY_OF_MONTH).toString()
    val monthShort = TimeUtils.format(calendar.timeInMillis, TimeUtils.monthShortFormatter).uppercase()

    Column(
        modifier = Modifier
            .size(32.dp)
            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = monthShort,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 6.sp),
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )
        }
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = day,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.ExtraBold, fontSize = 14.sp),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun AggregatedSummaryDialog(summary: AggregatedSummary, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(text = summary.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    text = summary.period,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (summary.ordHours > 0) {
                            DetailRow(label = stringResource(R.string.total_ordinary_label), value = "${formatHours(summary.ordHours)} (${formatCurrency(summary.ordAmount)})")
                        }
                        if (summary.extHours > 0) {
                            DetailRow(label = stringResource(R.string.total_extra_label), value = "${formatHours(summary.extHours)} (${formatCurrency(summary.extAmount)})")
                        }
                        if (summary.holHours > 0) {
                            DetailRow(label = stringResource(R.string.total_holiday_label), value = "${formatHours(summary.holHours)} (${formatCurrency(summary.holAmount)})")
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)

                        DetailRow(label = stringResource(R.string.total_hours_label), value = formatHours(summary.totalHours), isBold = true)
                        DetailRow(
                            label = stringResource(R.string.total_amount_label), 
                            value = formatCurrency(summary.totalEarnings),
                            isBold = true,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    stringResource(R.string.summary_desc),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_close)) }
        }
    )
}

@Composable
fun WorkLogDetailDialog(log: WorkLog, workerName: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("owner_prefs", Context.MODE_PRIVATE) }
    val festiveType = prefs.getInt("festive_days_type", 3)

    val isFestive = TimeUtils.isFestive(log.date, log.isManualHoliday, festiveType)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = workerName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    if (isFestive) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.holiday_indicator),
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
                Text(
                    text = TimeUtils.format(log.date, TimeUtils.fullDateFormatter).replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DetailRow(label = stringResource(R.string.morning_label), value = formatInterval(log.morningStart, log.morningEnd))
                        DetailRow(label = stringResource(R.string.afternoon_label), value = formatInterval(log.afternoonStart, log.afternoonEnd))
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        
                        if (isFestive) {
                            DetailRow(label = stringResource(R.string.total_holiday_label), value = formatHours(log.holidayHours), isBold = true)
                            DetailRow(label = "Importo fest", value = formatCurrency(log.holidayAmount))
                        } else {
                            DetailRow(label = stringResource(R.string.total_ordinary_label), value = formatHours(log.ordinaryHours))
                            if (log.extraHours > 0) {
                                DetailRow(label = stringResource(R.string.total_extra_label), value = formatHours(log.extraHours))
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
                            DetailRow(label = stringResource(R.string.total_hours_label), value = formatHours(log.totalHours), isBold = true)
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            DetailRow(label = "Importo Ord", value = formatCurrency(log.ordinaryAmount))
                            if (log.extraHours > 0) {
                                DetailRow(label = "Importo STR", value = formatCurrency(log.extraAmount))
                            }
                        }

                        DetailRow(
                            label = stringResource(R.string.amount_label), 
                            value = formatCurrency(log.totalAmount),
                            isBold = true,
                            color = if (isFestive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    stringResource(R.string.detail_readonly_msg),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_close)) }
        }
    )
}

@Composable
fun DetailRow(label: String, value: String, isBold: Boolean = false, color: Color = Color.Unspecified) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = value, 
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = color
        )
    }
}

fun formatInterval(start: String?, end: String?): String {
    return if (!start.isNullOrBlank() && !end.isNullOrBlank()) "$start - $end" else "N/A"
}

@Composable
fun PeriodNavigation(
    selectedFilter: Int,
    referenceDate: Long,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val calendar = Calendar.getInstance(Locale.ITALY)
    calendar.timeInMillis = referenceDate

    val label = when (selectedFilter) {
        0 -> calendar.get(Calendar.YEAR).toString()
        1 -> TimeUtils.formatMonth(calendar.timeInMillis)
        2 -> {
            val start = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_YEAR, 6)
            val end = calendar.timeInMillis
            "${TimeUtils.format(start, TimeUtils.dayMonthShortFormatter)} - ${TimeUtils.format(end, TimeUtils.dayMonthShortFormatter)}"
        }
        3 -> TimeUtils.format(calendar.timeInMillis, TimeUtils.dayShortFullDateFormatter)
        else -> ""
    }.replaceFirstChar { it.uppercase() }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onPrev) {
            Icon(Icons.Default.ChevronLeft, contentDescription = stringResource(R.string.nav_prev_desc))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = onNext) {
            Icon(Icons.Default.ChevronRight, contentDescription = stringResource(R.string.nav_next_desc))
        }
    }
}
