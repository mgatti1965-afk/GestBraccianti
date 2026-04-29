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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
class WorkLogViewModel(
    private val workLogRepository: WorkLogRepository,
    private val configRepository: WorkerYearConfigRepository
) : ViewModel() {

    private val _selectedYearId = MutableStateFlow<Int?>(null)

    fun setSelectedYear(yearId: Int) {
        _selectedYearId.value = yearId
    }

    val workerStats: StateFlow<List<WorkerYearStats>> = _selectedYearId
        .flatMapLatest { yearId ->
            if (yearId == null) {
                MutableStateFlow(emptyList())
            } else {
                configRepository.getWorkerStatsForYear(yearId)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            val startDate = sdf.parse(start)
            val endDate = sdf.parse(end)
            if (startDate != null && endDate != null) {
                val diff = endDate.time - startDate.time
                if (diff > 0) diff.toDouble() / (1000 * 60 * 60) else 0.0
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
