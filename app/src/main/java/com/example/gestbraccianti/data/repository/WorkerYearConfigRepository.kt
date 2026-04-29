package com.example.gestbraccianti.data.repository

import com.example.gestbraccianti.data.dao.WorkerYearConfigDao
import com.example.gestbraccianti.data.entity.Worker
import com.example.gestbraccianti.data.entity.WorkerYearConfig
import com.example.gestbraccianti.data.model.WorkerYearStats
import kotlinx.coroutines.flow.Flow

class WorkerYearConfigRepository(private val workerYearConfigDao: WorkerYearConfigDao) {
    fun getWorkersForYear(yearId: Int): Flow<List<Worker>> = 
        workerYearConfigDao.getWorkersForYear(yearId)

    suspend fun getConfig(workerId: Long, yearId: Int): WorkerYearConfig? = 
        workerYearConfigDao.getConfig(workerId, yearId)

    suspend fun insertConfig(config: WorkerYearConfig) = 
        workerYearConfigDao.insertConfig(config)
        
    suspend fun getConfigsForYear(yearId: Int): List<WorkerYearConfig> =
        workerYearConfigDao.getConfigsForYear(yearId)

    fun getWorkerStatsForYear(yearId: Int): Flow<List<WorkerYearStats>> =
        workerYearConfigDao.getWorkerStatsForYear(yearId)
}
