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
