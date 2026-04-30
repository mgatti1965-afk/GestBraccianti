package com.example.gestbraccianti.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gestbraccianti.data.entity.Worker
import com.example.gestbraccianti.data.entity.WorkerYearConfig
import com.example.gestbraccianti.data.repository.WorkerRepository
import com.example.gestbraccianti.data.repository.WorkerYearConfigRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class WorkerViewModel(
    private val workerRepository: WorkerRepository,
    private val configRepository: WorkerYearConfigRepository
) : ViewModel() {

    private val _selectedYearId = MutableStateFlow<Int?>(null)
    
    val workersForCurrentYear: StateFlow<List<Worker>> = _selectedYearId
        .flatMapLatest { yearId ->
            if (yearId != null) {
                configRepository.getWorkersForYear(yearId)
            } else {
                MutableStateFlow(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSelectedYear(yearId: Int) {
        _selectedYearId.value = yearId
    }

    fun addWorkerToYear(name: String, surname: String, phoneNumber: String, hourlyRate: Double, yearId: Int) {
        viewModelScope.launch {
            val workerId = workerRepository.insertWorker(Worker(name = name, surname = surname, phoneNumber = phoneNumber))
            configRepository.insertConfig(
                WorkerYearConfig(workerId = workerId, harvestYearId = yearId, hourlyRate = hourlyRate)
            )
        }
    }
    
    suspend fun getWorkerConfig(workerId: Long, yearId: Int): WorkerYearConfig? {
        return configRepository.getConfig(workerId, yearId)
    }

    fun updateWorkerInfo(workerId: Long, name: String, surname: String, phoneNumber: String, yearId: Int, newRate: Double) {
        viewModelScope.launch {
            workerRepository.updateWorker(Worker(id = workerId, name = name, surname = surname, phoneNumber = phoneNumber))
            configRepository.insertConfig(
                WorkerYearConfig(workerId = workerId, harvestYearId = yearId, hourlyRate = newRate)
            )
        }
    }
}

class WorkerViewModelFactory(
    private val workerRepository: WorkerRepository,
    private val configRepository: WorkerYearConfigRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WorkerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WorkerViewModel(workerRepository, configRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
