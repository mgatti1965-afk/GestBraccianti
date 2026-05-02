package com.example.gestbraccianti.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gestbraccianti.data.entity.HarvestYear
import com.example.gestbraccianti.data.repository.HarvestRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HarvestViewModel(private val repository: HarvestRepository) : ViewModel() {

    val allYears: StateFlow<List<HarvestYear>> = repository.allYears
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentYear = MutableStateFlow<HarvestYear?>(null)
    val currentYear: StateFlow<HarvestYear?> = _currentYear.asStateFlow()

    init {
        refreshCurrentYear()
    }

    private fun refreshCurrentYear() {
        viewModelScope.launch {
            _currentYear.value = repository.getCurrentYear()
        }
    }

    fun createYear(year: Int, migrateFrom: Int? = null) {
        viewModelScope.launch {
            repository.createNewYear(year, migrateFrom)
            refreshCurrentYear()
        }
    }

    fun selectYear(yearId: Int) {
        viewModelScope.launch {
            repository.switchYear(yearId)
            refreshCurrentYear()
        }
    }

    fun deselectYear() {
        viewModelScope.launch {
            repository.deselectYear()
            refreshCurrentYear()
        }
    }

    fun deleteYear(yearId: Int) {
        viewModelScope.launch {
            repository.deleteYear(yearId)
            refreshCurrentYear()
        }
    }
}

class HarvestViewModelFactory(private val repository: HarvestRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HarvestViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HarvestViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
