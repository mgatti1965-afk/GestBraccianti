package com.example.gestbraccianti.ui.screens

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialSummaryScreen(viewModel: WorkLogViewModel) {
    val stats by viewModel.workerStats.collectAsState()

    Box {
        if (stats.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nessun dato disponibile per questa annata.")
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
                Text("${stat.name} ${stat.surname}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text("Tariffa: ${stat.hourlyRate}€/h", style = MaterialTheme.typography.bodySmall)
            }
            Text("${stat.totalHours}h", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(
                text = String.format("%.2f €", stat.totalEarnings),
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
            Text("${totalHours}h", style = MaterialTheme.typography.bodyLarge)
            Text(
                text = String.format("%.2f €", totalEarnings),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}
