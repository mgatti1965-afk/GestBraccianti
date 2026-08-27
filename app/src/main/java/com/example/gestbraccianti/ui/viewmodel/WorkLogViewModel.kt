package com.example.gestbraccianti.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gestbraccianti.data.entity.WorkLog
import com.example.gestbraccianti.data.repository.WorkLogRepository
import com.example.gestbraccianti.data.repository.WorkerYearConfigRepository
import com.example.gestbraccianti.data.model.WorkerYearStats
import com.example.gestbraccianti.ui.utils.TimeUtils
import com.example.gestbraccianti.ui.utils.formatCurrency
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
class WorkLogViewModel(
    private val workLogRepository: WorkLogRepository,
    private val configRepository: WorkerYearConfigRepository,
) : ViewModel() {

    private val _selectedYearId = MutableStateFlow<Int?>(null)
    private val _dateRange = MutableStateFlow<Pair<Long, Long>?>(null)
    private val _currentReferenceDate = MutableStateFlow(Calendar.getInstance(Locale.ITALY).timeInMillis)
    val currentReferenceDate: StateFlow<Long> = _currentReferenceDate

    private val _isCalendarExpanded = MutableStateFlow(false)
    val isCalendarExpanded: StateFlow<Boolean> = _isCalendarExpanded.asStateFlow()

    private val _selectedDate = MutableStateFlow<Long?>(null)
    val selectedDate: StateFlow<Long?> = _selectedDate.asStateFlow()

    private val _globalFestiveDates = MutableStateFlow<Set<Long>>(emptySet())
    val globalFestiveDates: StateFlow<Set<Long>> = _globalFestiveDates.asStateFlow()

    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent = _uiEvent.asSharedFlow()

    init {
        loadGlobalFestiveDates()
    }

    private fun loadGlobalFestiveDates() {
        val context = com.example.gestbraccianti.GestBracciantiApplication.instance
        val prefs = context.getSharedPreferences("owner_prefs", android.content.Context.MODE_PRIVATE)
        val set = prefs.getStringSet("global_festive_dates", emptySet())?.mapNotNull { it.toLongOrNull() }?.toSet() ?: emptySet()
        _globalFestiveDates.value = set
    }

    fun setSelectedYear(yearId: Int) {
        _selectedYearId.value = yearId
        viewModelScope.launch {
            workLogRepository.fillMissingRates()
            // Ricalcoliamo gli importi e le suddivisioni ore se mancano (migrazione dati)
            val logs = workLogRepository.getLogsForYear(yearId).first()
            logs.forEach { log ->
                val needsUpdate = log.totalHours > 0 && (
                    log.totalAmount == 0.0 || 
                    (log.ordinaryHours == 0.0 && log.extraHours == 0.0 && log.holidayHours == 0.0)
                )
                if (needsUpdate) {
                    val updated = calculateAmounts(log)
                    workLogRepository.updateLog(updated)
                }
            }
        }
        val now = Calendar.getInstance(Locale.ITALY)
        val currentSystemYear = now[Calendar.YEAR]
        
        val cal = Calendar.getInstance(Locale.ITALY).apply {
            if (yearId == currentSystemYear) {
                timeInMillis = now.timeInMillis
            } else {
                set(Calendar.YEAR, yearId)
                set(Calendar.MONTH, Calendar.JANUARY)
                set(Calendar.DAY_OF_MONTH, 1)
            }
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        _currentReferenceDate.value = cal.timeInMillis
    }

    fun setDateRange(start: Long?, end: Long?) {
        if ((start == null) || (end == null)) {
            _dateRange.value = null
        } else {
            _dateRange.value = Pair(start, end)
        }
    }

    fun moveReferenceDate(filterType: Int, delta: Int) {
        val cal = Calendar.getInstance(Locale.ITALY).apply {
            timeInMillis = _currentReferenceDate.value
        }
        val yearBefore = cal.get(Calendar.YEAR)

        when (filterType) {
            0 -> return // Filtro "Anno": blocco totale del cambio anno dal riepilogo
            1 -> cal.add(Calendar.MONTH, delta)
            2 -> cal.add(Calendar.WEEK_OF_YEAR, delta)
            3 -> cal.add(Calendar.DAY_OF_YEAR, delta)
        }

    // Double check: se la modifica ci ha portato fuori dall'anno di riferimento, annulliamo
        if (cal.get(Calendar.YEAR) == yearBefore) {
            _currentReferenceDate.value = cal.timeInMillis
        }
    }

    fun updateReferenceDate(date: Long) {
        _currentReferenceDate.value = date
        _selectedDate.value = date
    }

    fun setCalendarExpanded(expanded: Boolean) {
        _isCalendarExpanded.value = expanded
    }

    val yearlyStats: StateFlow<List<WorkerYearStats>> = _selectedYearId
        .flatMapLatest { yearId ->
            if (yearId == null) flowOf(emptyList())
            else configRepository.getWorkerStatsForYear(yearId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allLogs: StateFlow<List<WorkLog>> = _selectedYearId
        .flatMapLatest { yearId ->
            if (yearId == null) flowOf(emptyList())
            else workLogRepository.getLogsForYear(yearId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredLogs: StateFlow<List<WorkLog>> = combine(allLogs, _dateRange) { logs, range ->
        if (range == null) logs
        else logs.filter { it.date in range.first..range.second }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveLog(
        id: Long = 0,
        workerId: Long,
        yearId: Int,
        date: Long,
        morningStart: String?,
        morningEnd: String?,
        afternoonStart: String?,
        afternoonEnd: String?
    ) {
        viewModelScope.launch {
            val totalHours = calculateHours(morningStart, morningEnd) + calculateHours(afternoonStart, afternoonEnd)
            val config = configRepository.getConfig(workerId, yearId)
            val currentRate = config?.hourlyRate ?: 0.0
            val extraRate = config?.extraHourlyRate ?: 0.0
            val holidayRate = config?.holidayHourlyRate ?: 0.0

            val existingLog = if (id != 0L) workLogRepository.getLogById(id) else null
            
            if (existingLog != null && existingLog.hourlyRate != currentRate && existingLog.hourlyRate != 0.0) {
                _uiEvent.emit("Tariffa aggiornata: da ${formatCurrency(existingLog.hourlyRate)} a ${formatCurrency(currentRate)}")
            }

            val log = WorkLog(
                id = id,
                workerId = workerId,
                harvestYearId = yearId,
                date = date,
                morningStart = morningStart,
                morningEnd = morningEnd,
                afternoonStart = afternoonStart,
                afternoonEnd = afternoonEnd,
                totalHours = totalHours,
                hourlyRate = currentRate,
                extraHourlyRate = extraRate,
                holidayHourlyRate = holidayRate,
                isManualHoliday = existingLog?.isManualHoliday ?: false
            ).let { calculateAmounts(it) }

            if (id == 0L) {
                workLogRepository.insertLog(log)
            } else {
                workLogRepository.updateLog(log)
            }
        }
    }

    fun saveLogRange(
        workerId: Long,
        yearId: Int,
        startDate: Long,
        endDate: Long,
        morningStart: String?,
        morningEnd: String?,
        afternoonStart: String?,
        afternoonEnd: String?
    ) {
        viewModelScope.launch {
            val totalHours = calculateHours(morningStart, morningEnd) + calculateHours(afternoonStart, afternoonEnd)
            val config = configRepository.getConfig(workerId, yearId)
            val currentRate = config?.hourlyRate ?: 0.0
            val extraRate = config?.extraHourlyRate ?: 0.0
            val holidayRate = config?.holidayHourlyRate ?: 0.0
            
            // Carichiamo i log una volta sola per efficienza
            val logsInYear = workLogRepository.getLogsForYear(yearId).first()
            
            val calendar = Calendar.getInstance(Locale.ITALY)
            calendar.timeInMillis = startDate
            
            while (calendar.timeInMillis <= endDate) {
                val currentDate = calendar.timeInMillis
                val existingLog = logsInYear.find {
                    it.workerId == workerId && it.date == currentDate 
                }
                
                val log = WorkLog(
                    id = existingLog?.id ?: 0L,
                    workerId = workerId,
                    harvestYearId = yearId,
                    date = currentDate,
                    morningStart = morningStart,
                    morningEnd = morningEnd,
                    afternoonStart = afternoonStart,
                    afternoonEnd = afternoonEnd,
                    totalHours = totalHours,
                    hourlyRate = currentRate,
                    extraHourlyRate = extraRate,
                    holidayHourlyRate = holidayRate,
                    isManualHoliday = existingLog?.isManualHoliday ?: false
                ).let { calculateAmounts(it) }
                
                if (log.id == 0L) {
                    workLogRepository.insertLog(log)
                } else {
                    workLogRepository.updateLog(log)
                }
                
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
        }
    }

    private fun calculateAmounts(log: WorkLog): WorkLog {
        val context = com.example.gestbraccianti.GestBracciantiApplication.instance
        val prefs = context.getSharedPreferences("owner_prefs", android.content.Context.MODE_PRIVATE)
        
        val threshold = prefs.getFloat("extra_hours_threshold", 8.0f).toDouble()
        val festiveType = prefs.getInt("festive_days_type", 3)

        val isFestive = TimeUtils.isFestive(log.date, log.isManualHoliday, festiveType)

        var ordH = 0.0
        var extH = 0.0
        var holH = 0.0
        var ordA = 0.0
        var extA = 0.0
        var holA = 0.0

        if (isFestive) {
            holH = log.totalHours
            holA = log.totalHours * log.holidayHourlyRate
        } else {
            if (log.totalHours > threshold) {
                ordH = threshold
                extH = log.totalHours - threshold
                ordA = threshold * log.hourlyRate
                extA = extH * log.extraHourlyRate
            } else {
                ordH = log.totalHours
                ordA = log.totalHours * log.hourlyRate
            }
        }

        return log.copy(
            totalAmount = ordA + extA + holA,
            ordinaryHours = ordH,
            extraHours = extH,
            holidayHours = holH,
            ordinaryAmount = ordA,
            extraAmount = extA,
            holidayAmount = holA
        )
    }

    fun toggleManualHoliday(log: WorkLog) {
        viewModelScope.launch {
            val updatedLog = log.copy(isManualHoliday = !log.isManualHoliday).let { calculateAmounts(it) }
            workLogRepository.updateLog(updatedLog)
        }
    }

    fun toggleHolidayForDate(date: Long) {
        viewModelScope.launch {
            val context = com.example.gestbraccianti.GestBracciantiApplication.instance
            val prefs = context.getSharedPreferences("owner_prefs", android.content.Context.MODE_PRIVATE)
            val globalFestiveSet = prefs.getStringSet("global_festive_dates", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
            
            val yearId = _selectedYearId.value ?: return@launch
            val logsForDay = workLogRepository.getLogsByDate(date, yearId)
            
            val dateStr = date.toString()
            val isCurrentlyGlobalFestive = globalFestiveSet.contains(dateStr)
            
            if (isCurrentlyGlobalFestive) {
                globalFestiveSet.remove(dateStr)
            } else {
                globalFestiveSet.add(dateStr)
            }
            
            prefs.edit().putStringSet("global_festive_dates", globalFestiveSet).apply()
            _globalFestiveDates.value = globalFestiveSet.mapNotNull { it.toLongOrNull() }.toSet()

            // Aggiorniamo anche i log esistenti per coerenza
            val affectedCount = logsForDay.size
            if (affectedCount > 0) {
                val targetState = !isCurrentlyGlobalFestive
                logsForDay.forEach { log ->
                    val updatedLog = log.copy(isManualHoliday = targetState).let { calculateAmounts(it) }
                    workLogRepository.updateLog(updatedLog)
                }
            }
            
            val toastMsg = if (!isCurrentlyGlobalFestive) {
                if (affectedCount > 0) "Festivo impostato: $affectedCount braccianti ricalcolati"
                else "Giorno impostato come festivo"
            } else {
                if (affectedCount > 0) "Feriale ripristinato: $affectedCount braccianti ricalcolati"
                else "Ripristinato giorno feriale"
            }
            _uiEvent.emit(toastMsg)
        }
    }

    private fun calculateHours(start: String?, end: String?): Double {
        if (start.isNullOrBlank() || end.isNullOrBlank()) return 0.0
        return try {
            val startDate = TimeUtils.timeFormatter.parse(start)
            val endDate = TimeUtils.timeFormatter.parse(end)
            if ((startDate != null) && (endDate != null)) {
                val diff = endDate.time - startDate.time
                if (diff > 0) {
                    diff.toDouble() / (1000 * 60 * 60)
                } else 0.0
            } else 0.0
        } catch (_: Exception) {
            0.0
        }
    }
    
    fun deleteLog(log: WorkLog) {
        viewModelScope.launch {
            workLogRepository.deleteLog(log)
        }
    }
}

class WorkLogViewModelFactory(
    private val workLogRepository: WorkLogRepository,
    private val configRepository: WorkerYearConfigRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WorkLogViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WorkLogViewModel(workLogRepository, configRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
