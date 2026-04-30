package com.example.gestbraccianti.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gestbraccianti.ui.viewmodel.WorkLogViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyLoggingScreen(
    viewModel: WorkLogViewModel,
    onDateClick: (Long) -> Unit
) {
    val allLogs by viewModel.allLogs.collectAsState()
    val sdf = SimpleDateFormat("EEEE dd MMMM yyyy", Locale.ITALY)
    val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.ITALY)
    val context = LocalContext.current

    var selectedCalendar by remember {
        mutableStateOf(Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        })
    }

    // Group logs by date to identify worked days
    val workedDays = remember(allLogs) {
        allLogs.map { it.date }.distinct().sortedDescending()
    }

    val filteredDays = remember(workedDays, selectedCalendar) {
        workedDays.filter { date ->
            val cal = Calendar.getInstance().apply { timeInMillis = date }
            cal.get(Calendar.MONTH) == selectedCalendar.get(Calendar.MONTH) &&
                    cal.get(Calendar.YEAR) == selectedCalendar.get(Calendar.YEAR)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Month Selector
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        val newCal = (selectedCalendar.clone() as Calendar).apply {
                            add(Calendar.MONTH, -1)
                        }
                        selectedCalendar = newCal
                    }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Mese precedente")
                    }

                    Text(
                        text = monthYearFormat.format(selectedCalendar.time).replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(onClick = {
                        val newCal = (selectedCalendar.clone() as Calendar).apply {
                            add(Calendar.MONTH, 1)
                        }
                        selectedCalendar = newCal
                    }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Mese successivo")
                    }
                }
            }

            if (filteredDays.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Nessuna giornata registrata per questo mese.")
                        if (workedDays.isNotEmpty()) {
                            TextButton(onClick = {
                                // Jump to latest worked day's month
                                val latest = Calendar.getInstance().apply { timeInMillis = workedDays.first() }
                                latest.set(Calendar.DAY_OF_MONTH, 1)
                                selectedCalendar = latest
                            }) {
                                Text("Vai all'ultimo mese lavorato")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredDays) { date ->
                        val logsForDay = allLogs.filter { it.date == date }
                        val totalWorkers = logsForDay.size
                        val totalHours = logsForDay.sumOf { it.totalHours }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onDateClick(date) }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = sdf.format(Date(date)).replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "$totalWorkers braccianti | ${
                                            String.format(
                                                Locale.ITALY,
                                                "%.1f",
                                                totalHours
                                            )
                                        } ore totali",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = "Dettagli")
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                val calendar = Calendar.getInstance()
                DatePickerDialog(
                    context,
                    { _, y, m, d ->
                        val newCal = Calendar.getInstance().apply {
                            set(y, m, d, 0, 0, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        onDateClick(newCal.timeInMillis)
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Aggiungi Giornata")
        }
    }
}
