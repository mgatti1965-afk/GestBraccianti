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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gestbraccianti.ui.viewmodel.WorkLogViewModel
import com.example.gestbraccianti.ui.viewmodel.WorkerGroupViewModel
import com.example.gestbraccianti.ui.utils.formatDecimalHours
import com.example.gestbraccianti.ui.utils.generatePdfReport
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

@Composable
fun QuickHelpSummaryDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Guida Riepilogo")
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Filtri Raggruppamento", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
                HelpItem(Icons.Default.Person, "Bracc. : Mostra i dati e i totali per ogni singolo bracciante.")
                HelpItem(Icons.Default.Group, "Gruppi : Raggruppa i lavoratori per squadra, mostrando i costi collettivi.")
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                
                Text("Modalità di Vista", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
                HelpItem(Icons.AutoMirrored.Filled.ListAlt, "Dettagli (📝) : Elenco cronologico di tutte le ore registrate nel periodo.")
                HelpItem(Icons.Default.BarChart, "Totali (📊) : Vista sintetica con solo i totali finali per persona o gruppo.")
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
    val scope = rememberCoroutineScope()
    
    var showReportDialog by remember { mutableStateOf(false) }
    var reportTargetAll by remember { mutableStateOf(true) }
    var selectedWorkerForReport by remember { mutableStateOf<Long?>(null) }
    var selectedGroupId by remember { mutableStateOf<Long?>(null) }
    var groupWorkerIds by remember { mutableStateOf<List<Long>>(emptyList()) }
    var reportStep by remember { mutableIntStateOf(0) }
    var showHelpDialog by remember { mutableStateOf(false) }

    if (showHelpDialog) {
        QuickHelpSummaryDialog(onDismiss = { showHelpDialog = false })
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
                    onClick = { showReportDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Condividi Riepilogo")
                }
            }
        }
    ) { innerPadding ->
        if (showReportDialog) {
            AlertDialog(
                onDismissRequest = { 
                    showReportDialog = false
                    reportStep = 0
                },
                title = { 
                    Text(
                        text = if (reportStep == 0) "Selezione Bracciante" else "Opzioni Report",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                text = {
                    if (reportStep == 0) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { 
                                    reportTargetAll = true
                                    selectedWorkerForReport = null
                                    selectedGroupId = null
                                    reportStep = 1 
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Tutti i Braccianti") }
                            
                            if (groups.isNotEmpty()) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                Text("Gruppo Braccianti:", style = MaterialTheme.typography.labelMedium)
                                LazyColumn(modifier = Modifier.heightIn(max = 150.dp)) {
                                    items(groups) { group ->
                                        OutlinedButton(
                                            onClick = {
                                                scope.launch {
                                                    val workers = groupViewModel.getWorkersInGroup(group.id).first()
                                                    groupWorkerIds = workers.map { it.id }
                                                    reportTargetAll = false
                                                    selectedWorkerForReport = null
                                                    selectedGroupId = group.id
                                                    reportStep = 1
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary)
                                        ) {
                                            Text(group.name)
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            Text("Singolo Braccianti:", style = MaterialTheme.typography.labelMedium)
                            
                            val workersInPeriod = filteredLogs.map { it.workerId }.distinct()
                            LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                                items(workersInPeriod, key = { it }) { wId ->
                                    val w = stats.find { it.workerId == wId }
                                    OutlinedButton(
                                        onClick = {
                                            reportTargetAll = false
                                            selectedWorkerForReport = wId
                                            selectedGroupId = null
                                            reportStep = 1
                                        },
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                    ) {
                                        Text("${w?.surname ?: ""} ${w?.name ?: "Bracc. $wId"}")
                                    }
                                }
                            }
                        }
                    } else {
                        val reportLabel = when {
                            reportTargetAll -> "Report completo"
                            selectedGroupId != null -> "Report Gruppo: ${groups.find { it.id == selectedGroupId }?.name}"
                            else -> "Report per: ${stats.find { it.workerId == selectedWorkerForReport }?.surname ?: ""}"
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = reportLabel,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Button(
                                onClick = {
                                    showReportDialog = false
                                    reportStep = 0
                                    val finalLogs = when {
                                        reportTargetAll -> filteredLogs
                                        selectedGroupId != null -> filteredLogs.filter { it.workerId in groupWorkerIds }
                                        else -> filteredLogs.filter { it.workerId == selectedWorkerForReport }
                                    }
                                    val reportText = generateUnifiedReport(
                                        context = context,
                                        logs = finalLogs,
                                        yearStats = stats,
                                        filterTitle = filters[selectedFilter],
                                        referenceDate = referenceDate
                                    )
                                    shareReport(context, reportText)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { 
                                Icon(Icons.Default.Share, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Invia Report (Testo)") 
                            }
                            
                            Button(
                                onClick = {
                                    showReportDialog = false
                                    reportStep = 0
                                    val finalLogs = when {
                                        reportTargetAll -> filteredLogs
                                        selectedGroupId != null -> filteredLogs.filter { it.workerId in groupWorkerIds }
                                        else -> filteredLogs.filter { it.workerId == selectedWorkerForReport }
                                    }
                                    val pdfFile = generatePdfReport(
                                        context = context,
                                        logs = finalLogs,
                                        yearStats = stats,
                                        filterTitle = filters[selectedFilter],
                                        referenceDate = referenceDate
                                    )
                                    if (pdfFile != null) {
                                        sharePdf(context, pdfFile)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) { 
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Esporta PDF")
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { 
                        if (reportStep > 0) reportStep = 0 else showReportDialog = false
                    }) { 
                        Text(if (reportStep > 0) "Indietro" else "Annulla") 
                    }
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
                IconButton(onClick = { showHelpDialog = true }) {
                    Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "Aiuto", tint = MaterialTheme.colorScheme.primary)
                }
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
                        selectedFilter = selectedFilter
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
    selectedFilter: Int
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
            item {
                calendar.set(Calendar.MONTH, monthIdx)
                val monthName = SimpleDateFormat("MMMM yyyy", Locale.ITALY).format(calendar.time).replaceFirstChar { it.uppercase() }
                
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
                    SummaryCard(title = groupName, subtitle = "Totale Gruppo", hours = hours, earnings = earnings)
                }
            } else if (viewMode == ViewMode.TOTALS) {
                items(workerTotals, key = { "w_${monthIdx}_${it.first}" }) { (wId, hours, earnings) ->
                    val worker = workerMap[wId]
                    val workerName = "${worker?.surname ?: ""} ${worker?.name ?: "Bracc. $wId"}"
                    SummaryCard(title = workerName, subtitle = "Totale Bracciante", hours = hours, earnings = earnings)
                }
            } else {
                items(mLogs.sortedBy { it.date }, key = { log -> "${log.workerId}_${log.date}" }) { log ->
                    val worker = workerMap[log.workerId]
                    val effectiveRate = log.hourlyRate
                    val earnings = log.totalHours * effectiveRate
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
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
                                    val rateStr = String.format(Locale.ITALY, "%.2f", effectiveRate)
                                    Text(
                                        text = "@ $rateStr €/h",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${formatDecimalHours(log.totalHours)} h",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                val earnStr = String.format(Locale.ITALY, "%.2f €", earnings)
                                Text(
                                    text = earnStr,
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
                                formatDecimalHours(mHours),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(" h", style = MaterialTheme.typography.labelSmall)
                            Spacer(modifier = Modifier.width(16.dp))
                            val mEarnStr = String.format(Locale.ITALY, "%.2f €", mEarnings)
                            Text(
                                text = mEarnStr,
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
                                text = "${formatDecimalHours(yHours)} h",
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
                            val yEarnStr = String.format(Locale.ITALY, "%.2f €", yEarnings)
                            Text(
                                text = yEarnStr,
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
fun SummaryCard(title: String, subtitle: String, hours: Double, earnings: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                    text = "${formatDecimalHours(hours)} h",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val earnStr = String.format(Locale.ITALY, "%.2f €", earnings)
                Text(
                    text = earnStr,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

fun generateUnifiedReport(
    context: Context,
    logs: List<WorkLog>,
    yearStats: List<WorkerYearStats>,
    filterTitle: String,
    referenceDate: Long
): String {
    val prefs = context.getSharedPreferences("owner_prefs", Context.MODE_PRIVATE)
    val ownerName = prefs.getString("owner_name", "") ?: ""
    val ownerSurname = prefs.getString("owner_surname", "") ?: ""
    
    val sdf = when (filterTitle) {
        "Mese" -> SimpleDateFormat("MMMM yyyy", Locale.ITALY)
        "Settimana" -> SimpleDateFormat("'Settimana' w, yyyy", Locale.ITALY)
        "Giorno" -> SimpleDateFormat("EEEE d MMMM yyyy", Locale.ITALY)
        else -> SimpleDateFormat("yyyy", Locale.ITALY)
    }
    val period = sdf.format(Date(referenceDate)).replaceFirstChar { it.uppercase() }

    val sb = StringBuilder()
    sb.append("*RIEPILOGO PRESENZE E COMPENSI*\n")
    if (ownerSurname.isNotBlank() || ownerName.isNotBlank()) {
        sb.append("Azienda: $ownerSurname $ownerName\n")
    }
    sb.append("Periodo: $period\n")
    sb.append("----------------------------------\n\n")

    val workerMap = yearStats.associateBy { it.workerId }
    val calendar = Calendar.getInstance(Locale.ITALY)
    val daySdf = SimpleDateFormat("dd/MM", Locale.ITALY)
    
    when (filterTitle) {
        "Giorno" -> {
            var totalDayHours = 0.0
            var totalDayEarnings = 0.0
            logs.sortedBy { it.workerId }.forEach { log ->
                val worker = workerMap[log.workerId]
                val workerName = if (worker != null) "${worker.surname} ${worker.name}" else "Bracciante ${log.workerId}"
                val earnStr = String.format(Locale.ITALY, "%.2f", log.totalHours * log.hourlyRate)
                sb.append("• $workerName: ${formatDecimalHours(log.totalHours)}h | $earnStr€\n")
                totalDayHours += log.totalHours
                totalDayEarnings += (log.totalHours * log.hourlyRate)
            }
            val totDayEarnStr = String.format(Locale.ITALY, "%.2f", totalDayEarnings)
            sb.append("\n*TOTALE GIORNALIERO*\n")
            sb.append("Ore: ${formatDecimalHours(totalDayHours)}h | Importo: $totDayEarnStr€\n")
        }
        "Settimana" -> {
            val groupedByWeek = logs.groupBy { log ->
                calendar.timeInMillis = log.date
                calendar.get(Calendar.YEAR) * 100 + calendar.get(Calendar.WEEK_OF_YEAR)
            }.toSortedMap()

            var totalOverallHours = 0.0
            var totalOverallEarnings = 0.0

            groupedByWeek.forEach { (weekKey, weekLogs) ->
                val weekYear = weekKey / 100
                val weekNum = weekKey % 100
                calendar.set(Calendar.YEAR, weekYear)
                calendar.set(Calendar.WEEK_OF_YEAR, weekNum)
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                val startWeek = Date(calendar.timeInMillis)
                calendar.add(Calendar.DAY_OF_YEAR, 6)
                val endWeek = Date(calendar.timeInMillis)
                
                sb.append("*SETTIMANA $weekNum (${daySdf.format(startWeek)} - ${daySdf.format(endWeek)})*\n")
                var totalWeekHours = 0.0
                var totalWeekEarnings = 0.0
                weekLogs.sortedBy { it.date }.forEach { log ->
                    val worker = workerMap[log.workerId]
                    val workerName = if (worker != null) "${worker.surname} ${worker.name}" else "Bracciante ${log.workerId}"
                    val earnStr = String.format(Locale.ITALY, "%.2f", log.totalHours * log.hourlyRate)
                    sb.append("• ${daySdf.format(Date(log.date))} $workerName: ${formatDecimalHours(log.totalHours)}h | $earnStr€\n")
                    totalWeekHours += log.totalHours
                    totalWeekEarnings += (log.totalHours * log.hourlyRate)
                }
                val totWeekEarnStr = String.format(Locale.ITALY, "%.2f", totalWeekEarnings)
                sb.append("Totale settimana: ${formatDecimalHours(totalWeekHours)}h | $totWeekEarnStr€\n\n")
                totalOverallHours += totalWeekHours
                totalOverallEarnings += totalWeekEarnings
            }
            val totalEarnStr = String.format(Locale.ITALY, "%.2f", totalOverallEarnings)
            sb.append("----------------------------------\n")
            sb.append("*TOTALE COMPLESSIVO*\n")
            sb.append("Ore totali: ${formatDecimalHours(totalOverallHours)} h\n")
            sb.append("Importo totale: $totalEarnStr €\n")
        }
        else -> { // Mese o Anno
            val groupedByMonth = logs.groupBy { log ->
                calendar.timeInMillis = log.date
                calendar.get(Calendar.MONTH)
            }.toSortedMap()

            var totalOverallHours = 0.0
            var totalOverallEarnings = 0.0

            groupedByMonth.forEach { (monthIdx, monthLogs) ->
                calendar.set(Calendar.MONTH, monthIdx)
                val monthName = SimpleDateFormat("MMMM yyyy", Locale.ITALY).format(calendar.time).uppercase()
                sb.append("*$monthName*\n")
                
                var totalMonthHours = 0.0
                var totalMonthEarnings = 0.0
                
                monthLogs.sortedBy { it.date }.forEach { log ->
                    val worker = workerMap[log.workerId]
                    val workerName = if (worker != null) "${worker.surname} ${worker.name}" else "Bracciante ${log.workerId}"
                    val earnStr = String.format(Locale.ITALY, "%.2f", log.totalHours * log.hourlyRate)
                    
                    sb.append("• ${daySdf.format(Date(log.date))} $workerName: ${formatDecimalHours(log.totalHours)}h | $earnStr€\n")
                    
                    totalMonthHours += log.totalHours
                    totalMonthEarnings += (log.totalHours * log.hourlyRate)
                }
                val totMonthEarnStr = String.format(Locale.ITALY, "%.2f", totalMonthEarnings)
                sb.append("Totale mese: ${formatDecimalHours(totalMonthHours)}h | $totMonthEarnStr€\n\n")
                totalOverallHours += totalMonthHours
                totalOverallEarnings += totalMonthEarnings
            }

            val totalEarnStr = String.format(Locale.ITALY, "%.2f", totalOverallEarnings)
            sb.append("----------------------------------\n")
            sb.append("*TOTALE COMPLESSIVO*\n")
            sb.append("Ore totali: ${formatDecimalHours(totalOverallHours)} h\n")
            sb.append("Importo totale: $totalEarnStr €\n")
        }
    }
    return sb.toString()
}

fun shareReport(context: Context, text: String) {
    val prefs = context.getSharedPreferences("owner_prefs", Context.MODE_PRIVATE)
    val ownerPhone = prefs.getString("owner_phone", "") ?: ""
    val cleanPhone = ownerPhone.filter { it.isDigit() || it == '+' }
    
    if (cleanPhone.isNotBlank()) {
        try {
            val url = "https://wa.me/$cleanPhone?text=${Uri.encode(text)}"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = url.toUri()
                setPackage("com.whatsapp")
            }
            context.startActivity(intent)
            return
        } catch (_: Exception) {}
    }
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(sendIntent, "Invia riepilogo..."))
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
fun PeriodNavigation(
    selectedFilter: Int,
    referenceDate: Long,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val calendar = Calendar.getInstance(Locale.ITALY).apply { timeInMillis = referenceDate }
    val currentYearInRef = calendar.get(Calendar.YEAR)

    // Logica di blocco rigorosa: non si può mai uscire dall'anno selezionato (currentYearInRef)
    val canGoPrev = when (selectedFilter) {
        0 -> false // Filtro "Anno": bloccato, non si cambia anno dalle frecce
        1 -> calendar.get(Calendar.MONTH) > Calendar.JANUARY // Resta nell'anno (min Gennaio)
        2 -> {
            // Verifica se la settimana precedente appartiene ancora allo stesso anno
            val tempCal = Calendar.getInstance(Locale.ITALY).apply { timeInMillis = referenceDate }
            tempCal.add(Calendar.WEEK_OF_YEAR, -1)
            tempCal.get(Calendar.YEAR) == currentYearInRef
        }
        3 -> calendar.get(Calendar.DAY_OF_YEAR) > 1 // Resta nell'anno (min 1 Gennaio)
        else -> false
    }

    val canGoNext = when (selectedFilter) {
        0 -> false // Filtro "Anno": bloccato
        1 -> calendar.get(Calendar.MONTH) < Calendar.DECEMBER // Resta nell'anno (max Dicembre)
        2 -> {
            // Verifica se la settimana successiva appartiene ancora allo stesso anno
            val tempCal = Calendar.getInstance(Locale.ITALY).apply { timeInMillis = referenceDate }
            tempCal.add(Calendar.WEEK_OF_YEAR, 1)
            tempCal.get(Calendar.YEAR) == currentYearInRef
        }
        3 -> calendar.get(Calendar.DAY_OF_YEAR) < calendar.getActualMaximum(Calendar.DAY_OF_YEAR) // Resta nell'anno (max 31 Dicembre)
        else -> false
    }

    val sdf = remember(selectedFilter) {
        when (selectedFilter) {
            0 -> SimpleDateFormat("yyyy", Locale.ITALY)
            1 -> SimpleDateFormat("MMMM yyyy", Locale.ITALY)
            2 -> SimpleDateFormat("'Settimana' w, yyyy", Locale.ITALY)
            3 -> SimpleDateFormat("EEEE d MMMM yyyy", Locale.ITALY)
            else -> SimpleDateFormat("dd/MM/yyyy", Locale.ITALY)
        }
    }

    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrev, enabled = canGoPrev) {
            Icon(
                Icons.Default.ChevronLeft,
                contentDescription = "Precedente",
                tint = if (canGoPrev) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            DynamicCalendarIcon(date = referenceDate)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = sdf.format(Date(referenceDate)).replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        IconButton(onClick = onNext, enabled = canGoNext) {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Successivo",
                tint = if (canGoNext) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        }
    }
}
