package com.example.gestbraccianti.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.gestbraccianti.data.entity.WorkLog
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkLogDao {
    @Query("SELECT * FROM work_logs WHERE harvestYearId = :yearId ORDER BY date DESC")
    fun getLogsForYear(yearId: Int): Flow<List<WorkLog>>

    @Query("SELECT * FROM work_logs WHERE workerId = :workerId AND harvestYearId = :yearId ORDER BY date DESC")
    fun getLogsForWorkerInYear(workerId: Long, yearId: Int): Flow<List<WorkLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(workLog: WorkLog)

    @Update
    suspend fun updateLog(workLog: WorkLog)

    @Delete
    suspend fun deleteLog(workLog: WorkLog)

    @Query("SELECT SUM(totalHours) FROM work_logs WHERE workerId = :workerId AND harvestYearId = :yearId")
    fun getTotalHoursForWorkerInYear(workerId: Long, yearId: Int): Flow<Double?>

    @Query("SELECT * FROM work_logs WHERE date = :date AND harvestYearId = :yearId")
    suspend fun getLogsByDate(date: Long, yearId: Int): List<WorkLog>

    @Query("SELECT * FROM work_logs")
    suspend fun getAllLogsStatic(): List<WorkLog>
}
