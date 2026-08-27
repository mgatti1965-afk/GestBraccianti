package com.example.gestbraccianti.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.gestbraccianti.R
import com.example.gestbraccianti.data.entity.WorkLog
import com.example.gestbraccianti.ui.viewmodel.WorkLogViewModel
import com.example.gestbraccianti.ui.utils.formatHours
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

    val haptic = LocalHapticFeedback.current

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // L'Header rimane FISSO in alto
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
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

            val isCalendarExpanded by viewModel.isCalendarExpanded.collectAsState()
            val lazyListState = rememberLazyListState()

            // Sincronizzazione: collassa il calendario quando si scende nella lista (solo durante lo scroll attivo)
            LaunchedEffect(lazyListState) {
                snapshotFlow { lazyListState.firstVisibleItemIndex to lazyListState.isScrollInProgress }
                    .collect { (index, isScrolling) ->
                        if (index > 0 && isScrolling) {
                            viewModel.setCalendarExpanded(false)
                        }
                    }
            }

            // Riposizionamento automatico sulla giornata selezionata (es. al ritorno dal dettaglio)
            LaunchedEffect(referenceDate, filteredDays) {
                val index = filteredDays.indexOfFirst { it == referenceDate }
                if (index >= 0) {
                    lazyListState.scrollToItem(index)
                }
            }

            // Calendario Espandibile/Collassabile (FISSO rispetto alla lista sotto)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .clickable { viewModel.setCalendarExpanded(!isCalendarExpanded) }
            ) {
                MonthGrid(
                    calendar = selectedCalendar,
                    workedDaysMap = workedDaysMap,
                    allLogs = allLogs,
                    isExpanded = isCalendarExpanded,
                    viewModel = viewModel,
                    onDateClick = onDateClick,
                    onDateLongClick = { date -> 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.toggleHolidayForDate(date) 
                    }
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Long press per festività",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                        modifier = Modifier.align(Alignment.CenterStart)
                    )
                    Icon(
                        imageVector = if (isCalendarExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            
            Text(
                stringResource(R.string.registered_days_label),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.primary
            )

            // Lista dei giorni (SCROLLABILE)
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
                    itemsIndexed(filteredDays, key = { _, date -> date }) { index, date ->
                        val logsForDay = allLogs.filter { it.date == date }
                        val totalWorkers = logsForDay.size
                        val totalHours = logsForDay.sumOf { it.totalHours }
                        val isSelected = date == referenceDate
                        val cardBg = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                     else if (index % 2 == 0) MaterialTheme.colorScheme.surface 
                                     else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clickable { onDateClick(date) },
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                                     else if (index == 0) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                     else null,
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
    allLogs: List<WorkLog>,
    isExpanded: Boolean,
    viewModel: WorkLogViewModel,
    onDateClick: (Long) -> Unit,
    onDateLongClick: (Long) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("owner_prefs", android.content.Context.MODE_PRIVATE) }
    val festiveType = remember(prefs) { prefs.getInt("festive_days_type", 3) }
    val globalFestiveDates by viewModel.globalFestiveDates.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()

    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfMonth = (calendar.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
    val firstDayOfWeek = firstDayOfMonth.get(Calendar.DAY_OF_WEEK)
    
    // Offset per far iniziare la settimana da Lunedì (2 in Calendar.DAY_OF_WEEK)
    val offset = (firstDayOfWeek - 2 + 7) % 7
    
    val today = Calendar.getInstance()
    
    // Se non è espanso, mostriamo solo la settimana che contiene "oggi" (se è il mese corrente) 
    // o la prima settimana (se è un mese diverso)
    val displayedRows = if (isExpanded) {
        val totalCells = daysInMonth + offset
        (totalCells + 6) / 7
    } else {
        1
    }

    // Calcoliamo quale riga mostrare quando è compresso: diamo priorità alla data selezionata (se nel mese), poi a oggi
    val rowToShow = if (!isExpanded) {
        val focusCal = Calendar.getInstance()
        selectedDate?.let { focusCal.timeInMillis = it } ?: run { focusCal.timeInMillis = today.timeInMillis }
        
        val dayToFocus = if (focusCal.get(Calendar.MONTH) == calendar.get(Calendar.MONTH) &&
            focusCal.get(Calendar.YEAR) == calendar.get(Calendar.YEAR)) {
            focusCal.get(Calendar.DAY_OF_MONTH)
        } else if (today.get(Calendar.MONTH) == calendar.get(Calendar.MONTH) &&
            today.get(Calendar.YEAR) == calendar.get(Calendar.YEAR)) {
            today.get(Calendar.DAY_OF_MONTH)
        } else {
            1
        }
        (dayToFocus + offset - 1) / 7
    } else {
        0
    }

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
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))

        for (r in 0 until displayedRows) {
            val actualRow = if (isExpanded) r else rowToShow
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (col in 0 until 7) {
                    val cellIndex = actualRow * 7 + col
                    val dayNum = cellIndex - offset + 1
                    
                    if (dayNum in 1..daysInMonth) {
                        val cellCal = (calendar.clone() as Calendar).apply {
                            set(Calendar.DAY_OF_MONTH, dayNum)
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        val cellDate = cellCal.timeInMillis

                        val isToday = today.get(Calendar.DAY_OF_MONTH) == dayNum &&
                                      today.get(Calendar.MONTH) == calendar.get(Calendar.MONTH) &&
                                      today.get(Calendar.YEAR) == calendar.get(Calendar.YEAR)

                        val isSelected = selectedDate == cellDate

                        val totalHours = workedDaysMap[dayNum]

                        val logsForDay = allLogs.filter { it.date == cellDate }
                        val isManualFromLogs = logsForDay.any { it.isManualHoliday }
                        val isGlobalOverride = globalFestiveDates.contains(cellDate)
                        
                        val isFestive = TimeUtils.isFestive(cellDate, isManualFromLogs, festiveType, globalFestiveDates)

                        DayCell(
                            day = dayNum,
                            isToday = isToday,
                            isSelected = isSelected,
                            isFestive = isFestive,
                            isManualOverride = isGlobalOverride || isManualFromLogs,
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
    isSelected: Boolean,
    isFestive: Boolean,
    isManualOverride: Boolean,
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
            isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
            isFestive -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
            isToday -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            else -> MaterialTheme.colorScheme.surface
        },
        border = when {
            isToday -> BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
            isSelected -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
            else -> null
        },
        tonalElevation = 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isManualOverride) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiary)
                )
            }
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
                        text = formatHours(totalHours),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isFestive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
