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

    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent = _uiEvent.asSharedFlow()

    fun setSelectedYear(yearId: Int) {
        _selectedYearId.value = yearId
        viewModelScope.launch {
            workLogRepository.fillMissingRates()
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
            val currentRate = configRepository.getConfig(workerId, yearId)?.hourlyRate ?: 0.0

            if (id != 0L) {
                val oldLog = workLogRepository.getLogById(id)
                if (oldLog != null && oldLog.hourlyRate != currentRate && oldLog.hourlyRate != 0.0) {
                    _uiEvent.emit("Tariffa aggiornata: da ${formatCurrency(oldLog.hourlyRate)} a ${formatCurrency(currentRate)}")
                }
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
                hourlyRate = currentRate
            )
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
            val currentRate = configRepository.getConfig(workerId, yearId)?.hourlyRate ?: 0.0
            
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
                    hourlyRate = currentRate
                )
                
                if (log.id == 0L) {
                    workLogRepository.insertLog(log)
                } else {
                    workLogRepository.updateLog(log)
                }
                
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
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
