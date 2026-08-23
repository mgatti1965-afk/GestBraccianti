package com.example.gestbraccianti.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.gestbraccianti.R
import com.example.gestbraccianti.ui.viewmodel.WorkLogViewModel
import com.example.gestbraccianti.ui.utils.formatHours
import com.example.gestbraccianti.ui.utils.formatDecimal
import com.example.gestbraccianti.ui.utils.TimeUtils
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyLoggingScreen(
    viewModel: WorkLogViewModel,
    onDateClick: (Long) -> Unit
) {
    val allLogs by viewModel.allLogs.collectAsState()
    val referenceDate by viewModel.currentReferenceDate.collectAsState()

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
            // L'Header rimane FISSO in alto
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
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
                            contentDescription = stringResource(R.string.prev_month_desc),
                            tint = if (!isFirstMonth) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    }

                    Text(
                        text = TimeUtils.formatMonth(selectedCalendar.timeInMillis),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(
                        onClick = { viewModel.moveReferenceDate(1, 1) },
                        enabled = !isLastMonth
                    ) {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = stringResource(R.string.next_month_desc),
                            tint = if (!isLastMonth) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    }
                }
            }

            // Tutto il resto (Calendario + Lista) diventa SCROLLABILE insieme
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Il calendario è il primo elemento della lista
                item {
                    MonthGrid(
                        calendar = selectedCalendar,
                        workedDaysMap = workedDaysMap,
                        allLogs = allLogs,
                        onDateClick = onDateClick,
                        onDateLongClick = { date -> viewModel.toggleHolidayForDate(date) }
                    )
                }

                // Etichetta e divisore
                item {
                    Column {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text(
                            stringResource(R.string.registered_days_label),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (filteredDays.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                stringResource(R.string.no_data_month),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                } else {
                    items(filteredDays) { date ->
                        val logsForDay = allLogs.filter { it.date == date }
                        val totalWorkers = logsForDay.size
                        val totalHours = logsForDay.sumOf { it.totalHours }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
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
                                        text = TimeUtils.format(date, TimeUtils.dayNameFormatter).replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = stringResource(R.string.worker_hours_summary, totalWorkers, formatHours(totalHours)),
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
    allLogs: List<com.example.gestbraccianti.data.entity.WorkLog>,
    onDateClick: (Long) -> Unit,
    onDateLongClick: (Long) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("owner_prefs", android.content.Context.MODE_PRIVATE) }
    val festiveType = remember(prefs) { prefs.getInt("festive_days_type", 3) }
    val globalFestiveDates = remember(allLogs) { 
        prefs.getStringSet("global_festive_dates", emptySet())?.mapNotNull { it.toLongOrNull() }?.toSet() ?: emptySet() 
    }

    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfMonth = (calendar.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
    val firstDayOfWeek = firstDayOfMonth.get(Calendar.DAY_OF_WEEK)
    
    val offset = (firstDayOfWeek - 2 + 7) % 7
    
    val today = Calendar.getInstance()
    val isCurrentMonth = today.get(Calendar.MONTH) == calendar.get(Calendar.MONTH) &&
                         today.get(Calendar.YEAR) == calendar.get(Calendar.YEAR)

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf(
                R.string.monday_short,
                R.string.tuesday_short,
                R.string.wednesday_short,
                R.string.thursday_short,
                R.string.friday_short,
                R.string.saturday_short,
                R.string.sunday_short
            ).forEach { dayRes ->
                Text(
                    text = stringResource(dayRes),
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
                        
                        val cellCal = (calendar.clone() as Calendar).apply {
                            set(Calendar.DAY_OF_MONTH, dayNum)
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        val cellDate = cellCal.timeInMillis

                        val logsForDay = allLogs.filter { it.date == cellDate }
                        val isManualHoliday = logsForDay.any { it.isManualHoliday }
                        
                        val isFestive = TimeUtils.isFestive(cellDate, isManualHoliday, festiveType, globalFestiveDates)

                        DayCell(
                            day = dayNum,
                            isToday = isToday,
                            isFestive = isFestive,
                            totalHours = totalHours,
                            modifier = Modifier.weight(1f),
                            onClick = { onDateClick(cellDate) },
                            onLongClick = { onDateLongClick(cellDate) }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DayCell(
    day: Int,
    isToday: Boolean,
    isFestive: Boolean,
    totalHours: Double?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val hasWorked = totalHours != null && totalHours > 0

    Surface(
        modifier = modifier
            .aspectRatio(1f)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = MaterialTheme.shapes.small,
        color = when {
            hasWorked -> if (isFestive) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer
            isFestive -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
            isToday -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            else -> MaterialTheme.colorScheme.surface
        },
        border = if (isToday) androidx.compose.foundation.BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else if (isFestive && !hasWorked) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)) else null,
        tonalElevation = if (hasWorked) 4.dp else if (isToday) 2.dp else 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = day.toString(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (hasWorked || isToday || isFestive) FontWeight.Bold else FontWeight.Normal,
                    color = when {
                        hasWorked -> if (isFestive) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
                        isFestive -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
                if (hasWorked) {
                    Text(
                        text = formatHours(totalHours!!),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isFestive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
