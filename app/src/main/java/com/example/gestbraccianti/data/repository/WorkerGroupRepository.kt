package com.example.gestbraccianti.data.repository

import com.example.gestbraccianti.data.dao.WorkerGroupDao
import com.example.gestbraccianti.data.entity.Worker
import com.example.gestbraccianti.data.entity.WorkerGroup
import com.example.gestbraccianti.data.entity.WorkerGroupCrossRef
import kotlinx.coroutines.flow.Flow

class WorkerGroupRepository(private val workerGroupDao: WorkerGroupDao) {
    fun getGroupsForYear(yearId: Int): Flow<List<WorkerGroup>> = workerGroupDao.getGroupsForYear(yearId)
    
    fun getWorkersInGroup(groupId: Long): Flow<List<Worker>> = workerGroupDao.getWorkersInGroup(groupId)

    suspend fun createGroup(name: String, yearId: Int) {
        workerGroupDao.insertGroup(WorkerGroup(name = name, yearId = yearId))
    }

    suspend fun deleteGroup(group: WorkerGroup) {
        workerGroupDao.deleteAllWorkersFromGroup(group.id)
        workerGroupDao.deleteGroup(group)
    }

    suspend fun addWorkerToGroup(workerId: Long, groupId: Long) {
        workerGroupDao.insertWorkerToGroup(WorkerGroupCrossRef(workerId, groupId))
    }

    suspend fun removeWorkerFromGroup(workerId: Long, groupId: Long) {
        workerGroupDao.removeWorkerFromGroup(WorkerGroupCrossRef(workerId, groupId))
    }
}
