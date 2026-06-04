package com.example.gestbraccianti.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gestbraccianti.data.entity.Worker
import com.example.gestbraccianti.data.entity.WorkerGroup
import com.example.gestbraccianti.data.repository.WorkerGroupRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class WorkerGroupViewModel(private val repository: WorkerGroupRepository) : ViewModel() {

    private val _selectedYearId = MutableStateFlow<Int?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val groupsForYear: StateFlow<List<WorkerGroup>> = _selectedYearId
        .filterNotNull()
        .flatMapLatest { yearId ->
            repository.getGroupsForYear(yearId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSelectedYear(yearId: Int) {
        _selectedYearId.value = yearId
    }

    fun createGroup(name: String) {
        val yearId = _selectedYearId.value ?: return
        viewModelScope.launch {
            repository.createGroup(name, yearId)
        }
    }

    fun deleteGroup(group: WorkerGroup) {
        viewModelScope.launch {
            repository.deleteGroup(group)
        }
    }

    fun addWorkerToGroup(workerId: Long, groupId: Long) {
        viewModelScope.launch {
            repository.addWorkerToGroup(workerId, groupId)
        }
    }

    fun removeWorkerFromGroup(workerId: Long, groupId: Long) {
        viewModelScope.launch {
            repository.removeWorkerFromGroup(workerId, groupId)
        }
    }

    fun getWorkersInGroup(groupId: Long): Flow<List<Worker>> {
        return repository.getWorkersInGroup(groupId)
    }

    fun copyGroupsFromPreviousYear(currentYearId: Int, onResult: (Int) -> Unit) {
        viewModelScope.launch {
            val previousYearId = currentYearId - 1
            // 1. Prendi i gruppi dell'anno precedente
            val previousYearGroups = repository.getGroupsForYear(previousYearId).first()
            
            // 2. Prendi i gruppi già presenti nell'anno corrente
            val currentYearGroups = repository.getGroupsForYear(currentYearId).first()
            val currentGroupNames = currentYearGroups.map { it.name.lowercase() }.toSet()
            
            var copiedCount = 0
            
            // 3. Filtra e copia quelli che mancano
            previousYearGroups.forEach { oldGroup ->
                if (oldGroup.name.lowercase() !in currentGroupNames) {
                    // Crea il nuovo gruppo
                    val newGroupId = repository.createGroupAndReturnId(oldGroup.name, currentYearId)
                    
                    // Copia i membri
                    val members = repository.getWorkersInGroup(oldGroup.id).first()
                    members.forEach { worker ->
                        repository.addWorkerToGroup(worker.id, newGroupId)
                    }
                    copiedCount++
                }
            }
            
            // Forza refresh ricaricando l'anno selezionato
            _selectedYearId.value = null
            _selectedYearId.value = currentYearId
            
            onResult(copiedCount)
        }
    }
}

class WorkerGroupViewModelFactory(private val repository: WorkerGroupRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WorkerGroupViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WorkerGroupViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
