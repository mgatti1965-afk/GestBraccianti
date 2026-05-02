package com.example.gestbraccianti.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Share
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

import java.util.*
import java.text.SimpleDateFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialSummaryScreen(viewModel: WorkLogViewModel) {
    val stats by viewModel.workerStats.collectAsState()
    val referenceDate by viewModel.currentReferenceDate.collectAsState()
    var selectedFilter by remember { mutableIntStateOf(0) }
    val filters = listOf("Anno", "Mese", "Settimana", "Giorno")
    val context = LocalContext.current

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
            if (stats.isNotEmpty()) {
                FloatingActionButton(
                    onClick = {
                        val reportText = generateReportText(context, stats, filters[selectedFilter], referenceDate)
                        shareReport(context, reportText)
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Condividi Riepilogo")
                }
            }
        }
    ) { innerPadding ->
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
}

fun generateReportText(
    context: Context,
    stats: List<com.example.gestbraccianti.data.model.WorkerYearStats>,
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
    
    sb.append("📊 *RIEPILOGO GESTBRACCIANTI*\n")
    sb.append("👤 Proprietario: $ownerSurname $ownerName\n")
    sb.append("📅 Periodo: $filterTitle ($period)\n")
    sb.append("----------------------------------\n\n")
    
    // Raggruppa per Anno, Mese e Settimana se necessario
    stats.forEach { stat ->
        sb.append("📍 *${stat.surname} ${stat.name}*\n")
        sb.append("   • Ore: ${String.format(Locale.ITALY, "%.1f", stat.totalHours)} h\n")
        sb.append("   • Totale: ${String.format(Locale.ITALY, "%.2f", stat.totalEarnings)} €\n")
        sb.append("\n")
    }
    
    val totalHours = stats.sumOf { it.totalHours }
    val totalEarnings = stats.sumOf { it.totalEarnings }
    
    sb.append("----------------------------------\n")
    sb.append("💰 *TOTALI COMPLESSIVI*\n")
    sb.append("   • Ore totali: ${String.format(Locale.ITALY, "%.1f", totalHours)} h\n")
    sb.append("   • Importo totale: ${String.format(Locale.ITALY, "%.2f", totalEarnings)} €\n")
    
    return sb.toString()
}

fun shareReport(context: Context, text: String) {
    val prefs = context.getSharedPreferences("owner_prefs", Context.MODE_PRIVATE)
    val ownerPhone = prefs.getString("owner_phone", "") ?: ""
    
    // Rimuove eventuali caratteri non numerici tranne il +
    val cleanPhone = ownerPhone.filter { it.isDigit() || it == '+' }
    
    if (cleanPhone.isNotBlank()) {
        try {
            // Proviamo ad aprire direttamente WhatsApp con il numero del proprietario
            val uri = "https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(text)}".toUri()
            val whatsappIntent = Intent(Intent.ACTION_VIEW, uri)
            whatsappIntent.setPackage("com.whatsapp")
            context.startActivity(whatsappIntent)
            return
        } catch (_: Exception) {
            // Se WhatsApp non è installato, usa il selettore normale
        }
    }
    
    // Se non c'è il numero o WhatsApp fallisce, usa il selettore di sistema
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
