package com.example.gestbraccianti.data.repository

import com.example.gestbraccianti.data.dao.WorkLogDao
import com.example.gestbraccianti.data.entity.WorkLog
import kotlinx.coroutines.flow.Flow

class WorkLogRepository(private val workLogDao: WorkLogDao) {
    fun getLogsForYear(yearId: Int): Flow<List<WorkLog>> = workLogDao.getLogsForYear(yearId)

    fun getLogsForWorkerInYear(workerId: Long, yearId: Int): Flow<List<WorkLog>> = 
        workLogDao.getLogsForWorkerInYear(workerId, yearId)

    suspend fun insertLog(workLog: WorkLog) = workLogDao.insertLog(workLog)
    suspend fun updateLog(workLog: WorkLog) = workLogDao.updateLog(workLog)
    suspend fun deleteLog(workLog: WorkLog) = workLogDao.deleteLog(workLog)

    fun getTotalHoursForWorkerInYear(workerId: Long, yearId: Int): Flow<Double?> = 
        workLogDao.getTotalHoursForWorkerInYear(workerId, yearId)

    suspend fun getLogsByDate(date: Long, yearId: Int): List<WorkLog> = 
        workLogDao.getLogsByDate(date, yearId)
}
