package com.example.gestbraccianti.data.dao

import androidx.room.*
import com.example.gestbraccianti.data.entity.Worker
import com.example.gestbraccianti.data.entity.WorkerGroup
import com.example.gestbraccianti.data.entity.WorkerGroupCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkerGroupDao {
    @Query("SELECT * FROM worker_groups WHERE yearId = :yearId")
    fun getGroupsForYear(yearId: Int): Flow<List<WorkerGroup>>

    @Query("SELECT * FROM worker_groups")
    suspend fun getAllGroupsStatic(): List<WorkerGroup>

    @Query("SELECT * FROM worker_group_cross_ref")
    suspend fun getAllCrossRefsStatic(): List<WorkerGroupCrossRef>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: WorkerGroup): Long

    @Delete
    suspend fun deleteGroup(group: WorkerGroup)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkerToGroup(crossRef: WorkerGroupCrossRef)

    @Delete
    suspend fun removeWorkerFromGroup(crossRef: WorkerGroupCrossRef)

    @Query("""
        SELECT workers.* FROM workers 
        INNER JOIN worker_group_cross_ref ON workers.id = worker_group_cross_ref.workerId 
        WHERE worker_group_cross_ref.groupId = :groupId
    """)
    fun getWorkersInGroup(groupId: Long): Flow<List<Worker>>

    @Query("DELETE FROM worker_group_cross_ref WHERE groupId = :groupId")
    suspend fun deleteAllWorkersFromGroup(groupId: Long)
}
