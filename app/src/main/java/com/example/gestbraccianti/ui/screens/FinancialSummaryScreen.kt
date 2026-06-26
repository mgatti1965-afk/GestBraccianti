package com.example.gestbraccianti.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gestbraccianti.ui.viewmodel.WorkLogViewModel
import com.example.gestbraccianti.ui.viewmodel.WorkerGroupViewModel
import com.example.gestbraccianti.ui.utils.formatHours
import com.example.gestbraccianti.ui.utils.generatePdfReport
import com.example.gestbraccianti.ui.utils.formatCurrency
import com.example.gestbraccianti.ui.utils.formatDecimal
import com.example.gestbraccianti.data.entity.WorkLog
import com.example.gestbraccianti.data.model.WorkerYearStats
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*
import java.text.SimpleDateFormat
import androidx.core.content.FileProvider
import java.io.File

import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

enum class GroupingType { BY_WORKER, BY_GROUP }
enum class ViewMode { DETAIL, TOTALS }

data class AggregatedSummary(
    val title: String,
    val period: String,
    val totalHours: Double,
    val totalEarnings: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialSummaryScreen(viewModel: WorkLogViewModel, groupViewModel: WorkerGroupViewModel) {
    val stats by viewModel.yearlyStats.collectAsState()
    val filteredLogs by viewModel.filteredLogs.collectAsState()
    val referenceDate by viewModel.currentReferenceDate.collectAsState()
    val groups by groupViewModel.groupsForYear.collectAsState()
    var selectedFilter by remember { mutableIntStateOf(0) }
    val filters = listOf("Anno", "Mese", "Settimana", "Giorno")
    
    var groupingType by remember { mutableStateOf(GroupingType.BY_WORKER) }
    var viewMode by remember { mutableStateOf(ViewMode.DETAIL) }
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
                    Icon(Icons.Default.PictureAsPdf, contentDescription = "Esporta PDF")
                }
            }
        }
    ) { innerPadding ->
        if (showReportDialog) {
            AlertDialog(
                onDismissRequest = { showReportDialog = false },
                title = { 
                    Text(
                        text = "Genera Report PDF",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (groupingType == GroupingType.BY_GROUP) {
                            Text(
                                text = "Verrà generato il PDF per i gruppi come visualizzati a video.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        } else {
                            Text(
                                text = "Seleziona i braccianti da includere:",
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
                                    Text("Tutti i braccianti", style = MaterialTheme.typography.bodyLarge)
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                
                                val workersInPeriod = filteredLogs.map { it.workerId }.distinct()
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
                                viewMode = viewMode,
                                groups = groups,
                                groupToWorkers = groupToWorkers
                            )
                            if (pdfFile != null) sharePdf(context, pdfFile)
                        }
                    ) {
                        Text("Genera PDF")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showReportDialog = false }) { Text("Annulla") }
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
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = groupingType == GroupingType.BY_WORKER,
                    onClick = { groupingType = GroupingType.BY_WORKER },
                    label = { Text("👤 Bracc.") },
                    leadingIcon = if (groupingType == GroupingType.BY_WORKER) {
                        { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                    } else null
                )
                FilterChip(
                    selected = groupingType == GroupingType.BY_GROUP,
                    onClick = { 
                        groupingType = GroupingType.BY_GROUP
                        viewMode = ViewMode.TOTALS
                    },
                    label = { Text("👥 Gruppi") },
                    leadingIcon = if (groupingType == GroupingType.BY_GROUP) {
                        { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                    } else null
                )
                
                Spacer(modifier = Modifier.weight(1f))

                FilterChip(
                    selected = viewMode == ViewMode.DETAIL,
                    onClick = { viewMode = ViewMode.DETAIL },
                    enabled = groupingType == GroupingType.BY_WORKER,
                    label = { Text("📝") },
                )
                FilterChip(
                    selected = viewMode == ViewMode.TOTALS,
                    onClick = { viewMode = ViewMode.TOTALS },
                    label = { Text("📊") },
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                if (filteredLogs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (selectedFilter == 0) "Nessun movimento registrato nell'anno."
                            else "Nessun dato disponibile per questa selezione.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    GroupedFinancialView(
                        logs = filteredLogs,
                        yearStats = stats,
                        groupingType = groupingType,
                        viewMode = viewMode,
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
    val daySdf = remember { SimpleDateFormat("dd/MM", Locale.ITALY) }

    val monthlyLogs = remember(logs) {
        logs.groupBy { log ->
            calendar.timeInMillis = log.date
            calendar.get(Calendar.MONTH)
        }.toSortedMap()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        monthlyLogs.forEach { (monthIdx, mLogs) ->
            val monthName = SimpleDateFormat("MMMM yyyy", Locale.ITALY).format(
                Calendar.getInstance(Locale.ITALY).apply { 
                    mLogs.firstOrNull()?.let { timeInMillis = it.date }
                    set(Calendar.MONTH, monthIdx) 
                }.time
            ).replaceFirstChar { it.uppercase() }

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
                    val earnings = wLogs.sumOf { it.totalHours * it.hourlyRate }
                    Triple(wId, hours, earnings)
                }.sortedByDescending { it.third }
            } else emptyList()

            val groupTotals = if (groupingType == GroupingType.BY_GROUP) {
                val result = mutableListOf<Triple<Long?, Double, Double>>()
                groups.forEach { group ->
                    val workersInGroup = groupToWorkers[group.id] ?: emptyList()
                    val gLogs = mLogs.filter { it.workerId in workersInGroup }
                    if (gLogs.isNotEmpty()) {
                        val hours = gLogs.sumOf { it.totalHours }
                        val earnings = gLogs.sumOf { it.totalHours * it.hourlyRate }
                        result.add(Triple(group.id, hours, earnings))
                    }
                }
                val allGroupWorkers = groupToWorkers.values.flatten().toSet()
                val noGroupLogs = mLogs.filter { it.workerId !in allGroupWorkers }
                if (noGroupLogs.isNotEmpty()) {
                    val hours = noGroupLogs.sumOf { it.totalHours }
                    val earnings = noGroupLogs.sumOf { it.totalHours * it.hourlyRate }
                    result.add(Triple(null, hours, earnings))
                }
                result.sortedByDescending { it.third }
            } else emptyList()

            if (groupingType == GroupingType.BY_GROUP) {
                items(groupTotals, key = { "g_${monthIdx}_${it.first ?: -1}" }) { (gId, hours, earnings) ->
                    val groupName = groups.find { it.id == gId }?.name ?: "Senza Gruppo"
                    SummaryCard(
                        title = groupName,
                        subtitle = "Totale Gruppo",
                        hours = hours,
                        earnings = earnings,
                        onClick = { onTotalClick(AggregatedSummary(groupName, monthName, hours, earnings)) }
                    )
                }
            } else if (viewMode == ViewMode.TOTALS) {
                items(workerTotals, key = { "w_${monthIdx}_${it.first}" }) { (wId, hours, earnings) ->
                    val worker = workerMap[wId]
                    val workerName = "${worker?.surname ?: ""} ${worker?.name ?: "Bracc. $wId"}"
                    SummaryCard(
                        title = workerName,
                        subtitle = "Totale Bracciante",
                        hours = hours,
                        earnings = earnings,
                        onClick = { onTotalClick(AggregatedSummary(workerName, monthName, hours, earnings)) }
                    )
                }
            } else {
                items(mLogs.sortedBy { it.date }, key = { log -> "${log.workerId}_${log.date}" }) { log ->
                    val worker = workerMap[log.workerId]
                    val effectiveRate = log.hourlyRate
                    val earnings = log.totalHours * effectiveRate
                    
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onLogClick(log) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = daySdf.format(Date(log.date)),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
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
                                if (effectiveRate > 0) {
                                    val rateStr = formatDecimal(effectiveRate)
                                    Text(
                                        text = "@ $rateStr €/h",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${formatHours(log.totalHours)} h",
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
                    }
                }
            }
            
            item {
                val mHours = mLogs.sumOf { it.totalHours }
                val mEarnings = mLogs.sumOf { log -> log.totalHours * log.hourlyRate }
                
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
                            2 -> "Totale Settimanale"
                            3 -> "Totale Giornaliero"
                            else -> "Totale Mensile"
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
                            Text(" h", style = MaterialTheme.typography.labelSmall)
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
            val yHours = logs.sumOf { it.totalHours }
            val yEarnings = logs.sumOf { log -> log.totalHours * log.hourlyRate }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "RIEPILOGO COMPLESSIVO",
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
                                    "Ore Totali",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }
                            Text(
                                text = "${formatHours(yHours)} h",
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
                                    "Importo Totale",
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
                }
            }
        }
    }
}

@Composable
fun SummaryCard(title: String, subtitle: String, hours: Double, earnings: Double, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
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
                    text = "${formatHours(hours)} h",
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
    context.startActivity(Intent.createChooser(intent, "Invia PDF..."))
}

@Composable
fun DynamicCalendarIcon(date: Long) {
    val calendar = Calendar.getInstance(Locale.ITALY).apply { timeInMillis = date }
    val day = calendar.get(Calendar.DAY_OF_MONTH).toString()
    val monthShort = SimpleDateFormat("MMM", Locale.ITALY).format(calendar.time).uppercase()

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
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DetailRow(label = "Totale Ore", value = "${formatHours(summary.totalHours)} h", isBold = true)
                        DetailRow(
                            label = "Importo Totale", 
                            value = formatCurrency(summary.totalEarnings),
                            isBold = true,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    "Riepilogo dei compensi per il periodo selezionato.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Chiudi") }
        }
    )
}

@Composable
fun WorkLogDetailDialog(log: WorkLog, workerName: String, onDismiss: () -> Unit) {
    val daySdf = remember { SimpleDateFormat("EEEE d MMMM yyyy", Locale.ITALY) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(text = workerName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    text = daySdf.format(Date(log.date)).replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DetailRow(label = "Mattina", value = formatInterval(log.morningStart, log.morningEnd))
                        DetailRow(label = "Pomeriggio", value = formatInterval(log.afternoonStart, log.afternoonEnd))
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        DetailRow(label = "Totale Ore", value = "${formatHours(log.totalHours)} h", isBold = true)
                        DetailRow(label = "Tariffa", value = "${formatCurrency(log.hourlyRate)}/h")
                        DetailRow(
                            label = "Importo", 
                            value = formatCurrency(log.totalHours * log.hourlyRate),
                            isBold = true,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    "Vista in sola lettura. Per modificare, usa la sezione di inserimento giornaliero.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Chiudi") }
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
        1 -> SimpleDateFormat("MMMM yyyy", Locale.ITALY).format(calendar.time)
        2 -> {
            val start = calendar.time
            calendar.add(Calendar.DAY_OF_YEAR, 6)
            val end = calendar.time
            val sdf = SimpleDateFormat("d MMM", Locale.ITALY)
            "${sdf.format(start)} - ${sdf.format(end)}"
        }
        3 -> SimpleDateFormat("EEE d MMMM yyyy", Locale.ITALY).format(calendar.time)
        else -> ""
    }.replaceFirstChar { it.uppercase() }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onPrev) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Precedente")
        }
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = onNext) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Successivo")
        }
    }
}
