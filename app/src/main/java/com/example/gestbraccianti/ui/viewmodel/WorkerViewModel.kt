package com.example.gestbraccianti.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gestbraccianti.data.entity.Worker
import com.example.gestbraccianti.data.entity.WorkerYearConfig
import com.example.gestbraccianti.data.model.WorkerWithRate
import com.example.gestbraccianti.data.repository.WorkerRepository
import com.example.gestbraccianti.data.repository.WorkerYearConfigRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class WorkerViewModel(
    private val workerRepository: WorkerRepository,
    private val configRepository: WorkerYearConfigRepository
) : ViewModel() {

    private val _selectedYearId = MutableStateFlow<Int?>(null)
    
    val workersWithRateForCurrentYear: StateFlow<List<WorkerWithRate>> = _selectedYearId
        .filterNotNull()
        .flatMapLatest { yearId ->
            workerRepository.getWorkersWithRateForYear(yearId)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

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

    fun addWorkerToYear(name: String, surname: String, phoneNumber: String, hourlyRate: Double, extraRate: Double, holidayRate: Double, yearId: Int) {
        viewModelScope.launch {
            val workerId = workerRepository.insertWorker(Worker(name = name, surname = surname, phoneNumber = phoneNumber))
            configRepository.insertConfig(
                WorkerYearConfig(
                    workerId = workerId,
                    harvestYearId = yearId,
                    hourlyRate = hourlyRate,
                    extraHourlyRate = extraRate,
                    holidayHourlyRate = holidayRate
                )
            )
        }
    }
    
    suspend fun getWorkerConfig(workerId: Long, yearId: Int): WorkerYearConfig? {
        return configRepository.getConfig(workerId, yearId)
    }

    fun updateWorkerInfo(workerId: Long, name: String, surname: String, phoneNumber: String, yearId: Int, newRate: Double, extraRate: Double, holidayRate: Double) {
        viewModelScope.launch {
            // Recupera il lavoratore esistente per preservare i campi non modificati (es. isArchived)
            val existingWorker = workerRepository.getWorkerById(workerId)
            if (existingWorker != null) {
                workerRepository.updateWorker(
                    existingWorker.copy(
                        name = name,
                        surname = surname,
                        phoneNumber = phoneNumber
                    )
                )
            }
            configRepository.insertConfig(
                WorkerYearConfig(
                    workerId = workerId,
                    harvestYearId = yearId,
                    hourlyRate = newRate,
                    extraHourlyRate = extraRate,
                    holidayHourlyRate = holidayRate
                )
            )
            
            // Forza il refresh ricaricando l'anno selezionato
            val currentYear = _selectedYearId.value
            _selectedYearId.value = null
            _selectedYearId.value = currentYear
        }
    }

    fun deleteWorker(worker: Worker) {
        viewModelScope.launch {
            workerRepository.deleteWorker(worker)
            // Refresh
            val currentYear = _selectedYearId.value
            _selectedYearId.value = null
            _selectedYearId.value = currentYear
        }
    }

    fun copyWorkersFromPreviousYear(currentYearId: Int, onResult: (Int) -> Unit) {
        viewModelScope.launch {
            val previousYearId = currentYearId - 1
            // 1. Prendi i braccianti dell'anno precedente
            val previousYearConfigs = configRepository.getConfigsForYear(previousYearId)
            
            // 2. Prendi i braccianti già presenti nell'anno corrente
            val currentYearConfigs = configRepository.getConfigsForYear(currentYearId)
            val currentWorkerIds = currentYearConfigs.map { it.workerId }.toSet()
            
            // 3. Filtra quelli che mancano
            val missingConfigs = previousYearConfigs.filter { it.workerId !in currentWorkerIds }
            
            // 4. Copia le configurazioni
            missingConfigs.forEach { config ->
                configRepository.insertConfig(
                    WorkerYearConfig(
                        workerId = config.workerId,
                        harvestYearId = currentYearId,
                        hourlyRate = config.hourlyRate,
                        extraHourlyRate = config.extraHourlyRate,
                        holidayHourlyRate = config.holidayHourlyRate
                    )
                )
            }
            
            // Forza refresh
            _selectedYearId.value = null
            _selectedYearId.value = currentYearId
            
            onResult(missingConfigs.size)
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
