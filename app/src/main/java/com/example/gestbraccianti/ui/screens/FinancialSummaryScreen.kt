package com.example.gestbraccianti.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gestbraccianti.ui.viewmodel.WorkLogViewModel

import java.util.*
import java.text.SimpleDateFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialSummaryScreen(viewModel: WorkLogViewModel) {
    val stats by viewModel.workerStats.collectAsState()
    val referenceDate by viewModel.currentReferenceDate.collectAsState()
    var selectedFilter by remember { mutableIntStateOf(0) }
    val filters = listOf("Anno", "Mese", "Settimana", "Giorno")

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

    Column {
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

        if (selectedFilter != 0) {
            PeriodNavigation(
                selectedFilter = selectedFilter,
                referenceDate = referenceDate,
                onPrev = { viewModel.moveReferenceDate(selectedFilter, -1) },
                onNext = { viewModel.moveReferenceDate(selectedFilter, 1) }
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            if (stats.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nessun dato disponibile per questa selezione.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        SummaryHeader()
                    }
                    items(stats) { item ->
                        WorkerStatCard(item)
                    }
                    item {
                        TotalFooter(
                            totalHours = stats.sumOf { it.totalHours },
                            totalEarnings = stats.sumOf { it.totalEarnings }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PeriodNavigation(
    selectedFilter: Int,
    referenceDate: Long,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    val sdf = when (selectedFilter) {
        1 -> SimpleDateFormat("MMMM yyyy", Locale.ITALY)
        2 -> SimpleDateFormat("'Settimana' w, yyyy", Locale.ITALY)
        3 -> SimpleDateFormat("EEEE d MMMM yyyy", Locale.ITALY)
        else -> SimpleDateFormat("yyyy", Locale.ITALY)
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrev) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Precedente")
        }
        Text(
            text = sdf.format(Date(referenceDate)).replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = onNext) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Successivo")
        }
    }
}

@Composable
fun SummaryHeader() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Lavoratore", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(2f))
        Text("Ore", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
        Text("Totale (€)", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1.5f))
    }
}

@Composable
fun WorkerStatCard(stat: com.example.gestbraccianti.data.model.WorkerYearStats) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(2f)) {
                Text("${stat.surname} ${stat.name}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(String.format(Locale.ITALY, "Tariffa: %.2f €/h", stat.hourlyRate), style = MaterialTheme.typography.bodySmall)
            }
            Text(String.format(Locale.ITALY, "%.2f h", stat.totalHours), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(
                text = String.format(Locale.ITALY, "%.2f €", stat.totalEarnings),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1.5f)
            )
        }
    }
}

@Composable
fun TotalFooter(totalHours: Double, totalEarnings: Double) {
    Divider(modifier = Modifier.padding(vertical = 8.dp))
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("TOTALE GENERALE", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
        Column(horizontalAlignment = Alignment.End) {
            Text(String.format(Locale.ITALY, "%.2f h", totalHours), style = MaterialTheme.typography.bodyLarge)
            Text(
                text = String.format(Locale.ITALY, "%.2f €", totalEarnings),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}
