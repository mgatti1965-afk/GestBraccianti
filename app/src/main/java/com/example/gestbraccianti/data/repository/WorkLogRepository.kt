package com.example.gestbraccianti.data.repository

import com.example.gestbraccianti.data.dao.WorkLogDao
import com.example.gestbraccianti.data.entity.WorkLog
import kotlinx.coroutines.flow.Flow

class WorkLogRepository(private val workLogDao: WorkLogDao) {
    fun getLogsForYear(yearId: Int): Flow<List<WorkLog>> = workLogDao.getLogsForYear(yearId)

    suspend fun insertLog(workLog: WorkLog) = workLogDao.insertLog(workLog)
    suspend fun updateLog(workLog: WorkLog) = workLogDao.updateLog(workLog)
    suspend fun deleteLog(workLog: WorkLog) = workLogDao.deleteLog(workLog)

    suspend fun getLogById(id: Long): WorkLog? = workLogDao.getLogById(id)

    suspend fun fillMissingRates() = workLogDao.fillMissingRates()
}
