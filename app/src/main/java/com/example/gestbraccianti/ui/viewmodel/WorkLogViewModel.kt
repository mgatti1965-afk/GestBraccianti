package com.example.gestbraccianti.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gestbraccianti.data.entity.WorkLog
import com.example.gestbraccianti.data.repository.WorkLogRepository
import com.example.gestbraccianti.data.repository.WorkerYearConfigRepository
import com.example.gestbraccianti.data.model.WorkerYearStats
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
class WorkLogViewModel(
    private val workLogRepository: WorkLogRepository,
    private val configRepository: WorkerYearConfigRepository
) : ViewModel() {

    private val _selectedYearId = MutableStateFlow<Int?>(null)
    private val _dateRange = MutableStateFlow<Pair<Long, Long>?>(null)
    private val _currentReferenceDate = MutableStateFlow(Calendar.getInstance(Locale.ITALY).timeInMillis)

    val currentReferenceDate: StateFlow<Long> = _currentReferenceDate

    fun setSelectedYear(yearId: Int) {
        _selectedYearId.value = yearId
        // Inizializza la data di riferimento all'inizio dell'anno selezionato
        val cal = Calendar.getInstance(Locale.ITALY).apply {
            set(Calendar.YEAR, yearId)
            set(Calendar.MONTH, Calendar.JANUARY)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        _currentReferenceDate.value = cal.timeInMillis
    }

    fun setDateRange(start: Long?, end: Long?) {
        if (start == null || end == null) {
            _dateRange.value = null
        } else {
            _dateRange.value = Pair(start, end)
        }
    }

    fun moveReferenceDate(filterType: Int, delta: Int) {
        val cal = Calendar.getInstance(Locale.ITALY).apply {
            timeInMillis = _currentReferenceDate.value
        }
        when (filterType) {
            1 -> cal.add(Calendar.MONTH, delta)
            2 -> cal.add(Calendar.WEEK_OF_YEAR, delta)
            3 -> cal.add(Calendar.DAY_OF_YEAR, delta)
        }
        _currentReferenceDate.value = cal.timeInMillis
    }

    val workerStats: StateFlow<List<WorkerYearStats>> = combine(_selectedYearId, _dateRange) { yearId, range ->
        Pair(yearId, range)
    }.flatMapLatest { (yearId, range) ->
        if (yearId == null) {
            MutableStateFlow(emptyList())
        } else if (range == null) {
            configRepository.getWorkerStatsForYear(yearId)
        } else {
            configRepository.getWorkerStatsForRange(yearId, range.first, range.second)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allLogs: StateFlow<List<WorkLog>> = _selectedYearId
        .flatMapLatest { yearId ->
            if (yearId == null) flowOf(emptyList())
            else workLogRepository.getLogsForYear(yearId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
            val log = WorkLog(
                id = id,
                workerId = workerId,
                harvestYearId = yearId,
                date = date,
                morningStart = morningStart,
                morningEnd = morningEnd,
                afternoonStart = afternoonStart,
                afternoonEnd = afternoonEnd,
                totalHours = totalHours
            )
            if (id == 0L) {
                workLogRepository.insertLog(log)
            } else {
                workLogRepository.updateLog(log)
            }
        }
    }

    private fun calculateHours(start: String?, end: String?): Double {
        if (start.isNullOrBlank() || end.isNullOrBlank()) return 0.0
        return try {
            val sdf = SimpleDateFormat("HH:mm", Locale.ITALY)
            val startDate = sdf.parse(start)
            val endDate = sdf.parse(end)
            if (startDate != null && endDate != null) {
                val diff = endDate.time - startDate.time
                if (diff > 0) {
                    val hours = diff.toDouble() / (1000 * 60 * 60)
                    hours
                } else 0.0
            } else 0.0
        } catch (e: Exception) {
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
