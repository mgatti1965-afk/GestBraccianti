package com.example.gestbraccianti.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    val referenceDate by viewModel.currentReferenceDate.collectAsState()
    val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.ITALY)

    var selectedCalendar by remember {
        mutableStateOf(Calendar.getInstance().apply {
            timeInMillis = referenceDate
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        })
    }

    LaunchedEffect(referenceDate) {
        val cal = Calendar.getInstance().apply {
            timeInMillis = referenceDate
            set(Calendar.DAY_OF_MONTH, 1)
        }
        selectedCalendar = cal
    }

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

    val workedDaysMap = remember(allLogs, selectedCalendar) {
        allLogs.filter { log ->
            val cal = Calendar.getInstance().apply { timeInMillis = log.date }
            cal.get(Calendar.MONTH) == selectedCalendar.get(Calendar.MONTH) &&
                    cal.get(Calendar.YEAR) == selectedCalendar.get(Calendar.YEAR)
        }.groupBy { 
            Calendar.getInstance().apply { timeInMillis = it.date }.get(Calendar.DAY_OF_MONTH)
        }.mapValues { entry ->
            entry.value.sumOf { it.totalHours }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                    val isFirstMonth = selectedCalendar.get(Calendar.MONTH) == Calendar.JANUARY
                    val isLastMonth = selectedCalendar.get(Calendar.MONTH) == Calendar.DECEMBER

                    IconButton(
                        onClick = { viewModel.moveReferenceDate(1, -1) },
                        enabled = !isFirstMonth
                    ) {
                        Icon(
                            Icons.Default.ChevronLeft,
                            contentDescription = "Mese precedente",
                            tint = if (!isFirstMonth) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    }

                    Text(
                        text = monthYearFormat.format(selectedCalendar.time).replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(
                        onClick = { viewModel.moveReferenceDate(1, 1) },
                        enabled = !isLastMonth
                    ) {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = "Mese successivo",
                            tint = if (!isLastMonth) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    }
                }
            }

            MonthGrid(
                calendar = selectedCalendar,
                workedDaysMap = workedDaysMap,
                onDateClick = onDateClick
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                "Giornate registrate",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.primary
            )

            if (filteredDays.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Nessun dato per questo mese.", style = MaterialTheme.typography.bodySmall)
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
                                .padding(horizontal = 4.dp)
                                .clickable { onDateClick(date) },
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val cal = Calendar.getInstance().apply { timeInMillis = date }
                                Text(
                                    text = cal.get(Calendar.DAY_OF_MONTH).toString(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(32.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = SimpleDateFormat("EEEE", Locale.ITALY).format(cal.time).replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "$totalWorkers bracc. • ${String.format(Locale.ITALY, "%.1f", totalHours)} ore",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MonthGrid(
    calendar: Calendar,
    workedDaysMap: Map<Int, Double>,
    onDateClick: (Long) -> Unit
) {
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfMonth = (calendar.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
    val firstDayOfWeek = firstDayOfMonth.get(Calendar.DAY_OF_WEEK)
    
    val offset = (firstDayOfWeek - 2 + 7) % 7
    
    val today = Calendar.getInstance()
    val isCurrentMonth = today.get(Calendar.MONTH) == calendar.get(Calendar.MONTH) &&
                         today.get(Calendar.YEAR) == calendar.get(Calendar.YEAR)

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("L", "M", "M", "G", "V", "S", "D").forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))

        val totalCells = daysInMonth + offset
        val rows = (totalCells + 6) / 7
        
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val dayNum = cellIndex - offset + 1
                    
                    if (dayNum in 1..daysInMonth) {
                        val isToday = isCurrentMonth && dayNum == today.get(Calendar.DAY_OF_MONTH)
                        val totalHours = workedDaysMap[dayNum]
                        
                        DayCell(
                            day = dayNum,
                            isToday = isToday,
                            totalHours = totalHours,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val clickCal = (calendar.clone() as Calendar).apply {
                                    set(Calendar.DAY_OF_MONTH, dayNum)
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                onDateClick(clickCal.timeInMillis)
                            }
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
fun DayCell(
    day: Int,
    isToday: Boolean,
    totalHours: Double?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val hasWorked = totalHours != null && totalHours > 0

    Surface(
        onClick = onClick,
        modifier = modifier.aspectRatio(1f),
        shape = MaterialTheme.shapes.small,
        color = when {
            hasWorked -> MaterialTheme.colorScheme.primaryContainer
            isToday -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            else -> MaterialTheme.colorScheme.surface
        },
        border = if (isToday) androidx.compose.foundation.BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else null,
        tonalElevation = if (hasWorked) 4.dp else if (isToday) 2.dp else 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = day.toString(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (hasWorked || isToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (hasWorked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
                if (hasWorked) {
                    Text(
                        text = "${String.format(Locale.ITALY, "%.0f", totalHours)}h",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
