package com.example.gestbraccianti.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Payments
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
import com.example.gestbraccianti.ui.viewmodel.WorkLogViewModel
import com.example.gestbraccianti.ui.utils.formatDecimalHours
import com.example.gestbraccianti.data.entity.WorkLog
import com.example.gestbraccianti.data.model.WorkerYearStats
import java.util.*
import java.text.SimpleDateFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialSummaryScreen(viewModel: WorkLogViewModel) {
    val stats by viewModel.yearlyStats.collectAsState()
    val filteredLogs by viewModel.filteredLogs.collectAsState()
    val referenceDate by viewModel.currentReferenceDate.collectAsState()
    var selectedFilter by remember { mutableIntStateOf(0) }
    val filters = listOf("Anno", "Mese", "Settimana", "Giorno")
    val context = LocalContext.current
    var showReportDialog by remember { mutableStateOf(false) }
    var reportTargetAll by remember { mutableStateOf(true) }
    var selectedWorkerForReport by remember { mutableStateOf<Long?>(null) }
    var reportStep by remember { mutableIntStateOf(0) } // 0: Scelta Target, 1: Scelta Frequenza

    LaunchedEffect(selectedFilter, referenceDate) {
        val calendar = Calendar.getInstance(Locale.ITALY)
        calendar.timeInMillis = referenceDate
        
        when (selectedFilter) {
            0 -> viewModel.setDateRange(null, null) // Anno (full)
            1 -> { // Mese
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
            2 -> { // Settimana
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
            3 -> { // Giorno
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
                    text = if (reportStep == 0) "Selezione Bracciante" else "Tipo di Totale",
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
                                reportStep = 1 
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Tutti i Braccianti") }
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Text("Singolo Bracciante:", style = MaterialTheme.typography.labelMedium)
                        
                        val workersInPeriod = filteredLogs.map { it.workerId }.distinct()
                        LazyColumn(modifier = Modifier.heightIn(max = 250.dp)) {
                            items(workersInPeriod) { wId ->
                                val w = stats.find { it.workerId == wId }
                                OutlinedButton(
                                    onClick = {
                                        reportTargetAll = false
                                        selectedWorkerForReport = wId
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
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = if (reportTargetAll) "Report completo" else "Report per: ${stats.find { it.workerId == selectedWorkerForReport }?.surname ?: ""}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(
                            onClick = {
                                showReportDialog = false
                                reportStep = 0
                                val reportText = generateUnifiedReport(
                                    context = context,
                                    logs = if (reportTargetAll) filteredLogs else filteredLogs.filter { it.workerId == selectedWorkerForReport },
                                    yearStats = stats,
                                    filterTitle = filters[selectedFilter],
                                    referenceDate = referenceDate,
                                    reportByWeek = false
                                )
                                shareReport(context, reportText)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Totali Mensili") }
                        
                        Button(
                            onClick = {
                                showReportDialog = false
                                reportStep = 0
                                val reportText = generateUnifiedReport(
                                    context = context,
                                    logs = if (reportTargetAll) filteredLogs else filteredLogs.filter { it.workerId == selectedWorkerForReport },
                                    yearStats = stats,
                                    filterTitle = filters[selectedFilter],
                                    referenceDate = referenceDate,
                                    reportByWeek = true
                                )
                                shareReport(context, reportText)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Totali Settimanali") }
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

            PeriodNavigation(
                selectedFilter = selectedFilter,
                referenceDate = referenceDate,
                onPrev = { viewModel.moveReferenceDate(selectedFilter, -1) },
                onNext = { viewModel.moveReferenceDate(selectedFilter, 1) }
            )

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
                        isFullYear = selectedFilter == 0
                    )
                }
            }
        }
    }
}

@Composable
fun GroupedFinancialView(logs: List<WorkLog>, yearStats: List<WorkerYearStats>, isFullYear: Boolean) {
    val workerMap = remember(yearStats) { yearStats.associateBy { it.workerId } }
    val calendar = remember { Calendar.getInstance(Locale.ITALY) }
    val daySdf = remember { SimpleDateFormat("dd/MM", Locale.ITALY) }

    val monthlyLogs = remember(logs) {
        logs.groupBy {
            calendar.timeInMillis = it.date
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
            
            items(mLogs.sortedBy { it.date }) { log ->
                val worker = workerMap[log.workerId]
                // Usa sempre la tariffa salvata nel log (snapshot)
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
                                Text(
                                    text = "@ ${String.format(Locale.ITALY, "%.2f", effectiveRate)} €/h",
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
                            Text(
                                text = String.format(Locale.ITALY, "%.2f €", earnings),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
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
                        Text(
                            "Totale Mensile",
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
                            Text(
                                String.format(Locale.ITALY, "%.2f €", mEarnings),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
        
        // Totale Finale del Periodo
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
                        text = if (isFullYear) "RIEPILOGO ANNUALE" else "RIEPILOGO PERIODO",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified,
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
                        
                        // Separatore verticale
                        VerticalDivider(
                            modifier = Modifier.height(48.dp).padding(horizontal = 16.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                        )
                        
                        // Colonna Guadagno
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
                                text = String.format(Locale.ITALY, "%.2f €", yEarnings),
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

fun generateUnifiedReport(
    context: Context,
    logs: List<WorkLog>,
    yearStats: List<WorkerYearStats>,
    filterTitle: String,
    referenceDate: Long,
    reportByWeek: Boolean
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
    
    if (!reportByWeek) {
        // RAGGRUPPAMENTO PER MESE
        val groupedByMonth = logs.groupBy {
            calendar.timeInMillis = it.date
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
                val effectiveRate = log.hourlyRate
                val earnings = log.totalHours * effectiveRate
                
                sb.append("• ${daySdf.format(Date(log.date))} $workerName: ${formatDecimalHours(log.totalHours)}h | ${String.format(Locale.ITALY, "%.2f", earnings)}€\n")
                
                totalMonthHours += log.totalHours
                totalMonthEarnings += earnings
            }
            
            sb.append("Totale mese: ${formatDecimalHours(totalMonthHours)}h | ${String.format(Locale.ITALY, "%.2f", totalMonthEarnings)}€\n\n")
            
            totalOverallHours += totalMonthHours
            totalOverallEarnings += totalMonthEarnings
        }

        sb.append("----------------------------------\n")
        sb.append("*TOTALE COMPLESSIVO*\n")
        sb.append("Ore totali: ${formatDecimalHours(totalOverallHours)} h\n")
        sb.append("Importo totale: ${String.format(Locale.ITALY, "%.2f", totalOverallEarnings)} €\n")
    } else {
        // RAGGRUPPAMENTO PER SETTIMANA
        val groupedByWeek = logs.groupBy {
            calendar.timeInMillis = it.date
            calendar.get(Calendar.YEAR) * 100 + calendar.get(Calendar.WEEK_OF_YEAR)
        }.toSortedMap()

        var totalOverallHours = 0.0
        var totalOverallEarnings = 0.0

        groupedByWeek.forEach { (weekKey, weekLogs) ->
            val weekYear = weekKey / 100
            val weekNum = weekKey % 100
            
            // Calcolo range della settimana
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
                val effectiveRate = log.hourlyRate
                val earnings = log.totalHours * effectiveRate
                
                sb.append("• ${daySdf.format(Date(log.date))} $workerName: ${formatDecimalHours(log.totalHours)}h | ${String.format(Locale.ITALY, "%.2f", earnings)}€\n")
                
                totalWeekHours += log.totalHours
                totalWeekEarnings += earnings
            }
            
            sb.append("Totale settimana: ${formatDecimalHours(totalWeekHours)}h | ${String.format(Locale.ITALY, "%.2f", totalWeekEarnings)}€\n\n")
            
            totalOverallHours += totalWeekHours
            totalOverallEarnings += totalWeekEarnings
        }

        sb.append("----------------------------------\n")
        sb.append("*TOTALE COMPLESSIVO*\n")
        sb.append("Ore totali: ${formatDecimalHours(totalOverallHours)} h\n")
        sb.append("Importo totale: ${String.format(Locale.ITALY, "%.2f", totalOverallEarnings)} €\n")
    }
    
    return sb.toString()
}

fun shareReport(context: Context, text: String) {
    val prefs = context.getSharedPreferences("owner_prefs", Context.MODE_PRIVATE)
    val ownerPhone = prefs.getString("owner_phone", "") ?: ""
    
    // Rimuove eventuali caratteri non numerici tranne il +
    val cleanPhone = ownerPhone.filter { it.isDigit() || it == '+' }
    
    if (cleanPhone.isNotBlank()) {
        try {
            // URL specifico per aprire direttamente la chat di WhatsApp con quel numero e il testo pronto
            val url = "https://wa.me/$cleanPhone?text=${Uri.encode(text)}"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
                setPackage("com.whatsapp") // Forza l'uso di WhatsApp
            }
            context.startActivity(intent)
            return
        } catch (e: Exception) {
            // Fallback se WhatsApp non è installato
        }
    }
    
    // Se non c'è il numero o WhatsApp fallisce, usa il selettore di sistema come ultima risorsa
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    val shareIntent = Intent.createChooser(sendIntent, "Invia riepilogo...")
    context.startActivity(shareIntent)
}

@Composable
fun PeriodNavigation(
    selectedFilter: Int,
    referenceDate: Long,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    val calendar = Calendar.getInstance(Locale.ITALY).apply { timeInMillis = referenceDate }
    val currentYearInRef = calendar.get(Calendar.YEAR)
    val nowYear = Calendar.getInstance(Locale.ITALY).get(Calendar.YEAR)

    val canGoPrev = when (selectedFilter) {
        0 -> currentYearInRef > 2021
        1 -> calendar.get(Calendar.MONTH) > Calendar.JANUARY || currentYearInRef > 2021
        2 -> calendar.get(Calendar.WEEK_OF_YEAR) > 1 || calendar.get(Calendar.MONTH) > Calendar.JANUARY || currentYearInRef > 2021
        3 -> calendar.get(Calendar.DAY_OF_YEAR) > 1 || currentYearInRef > 2021
        else -> true
    }

    val canGoNext = when (selectedFilter) {
        0 -> currentYearInRef < nowYear
        1 -> calendar.get(Calendar.MONTH) < Calendar.DECEMBER || currentYearInRef < nowYear
        2 -> calendar.get(Calendar.WEEK_OF_YEAR) < calendar.getActualMaximum(Calendar.WEEK_OF_YEAR) || calendar.get(Calendar.MONTH) < Calendar.DECEMBER || currentYearInRef < nowYear
        3 -> calendar.get(Calendar.DAY_OF_YEAR) < calendar.getActualMaximum(Calendar.DAY_OF_YEAR) || currentYearInRef < nowYear
        else -> true
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
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
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
        Text(
            text = sdf.format(Date(referenceDate)).replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = onNext, enabled = canGoNext) {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Successivo",
                tint = if (canGoNext) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
fun SummaryHeader() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Bracciante", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(2f))
        Text("Ore", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
        Text("Totale (€)", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1.5f))
    }
}

@Composable
fun WorkerStatCard(stat: com.example.gestbraccianti.data.model.WorkerYearStats) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar or Initial
            Surface(
                modifier = Modifier.size(48.dp),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = stat.surname.take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${stat.surname} ${stat.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = String.format(Locale.ITALY, "Tariffa: %.2f €/h", stat.hourlyRate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatDecimalHours(stat.totalHours),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = String.format(Locale.ITALY, "%.2f €", stat.totalEarnings),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
fun TotalFooter(totalHours: Double, totalEarnings: Double) {
    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("TOTALE GENERALE", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
        Column(horizontalAlignment = Alignment.End) {
            Text(formatDecimalHours(totalHours), style = MaterialTheme.typography.bodyLarge)
            Text(
                text = String.format(Locale.ITALY, "%.2f €", totalEarnings),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}
