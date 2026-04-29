package com.example.gestbraccianti.data.repository

import com.example.gestbraccianti.data.dao.WorkerDao
import com.example.gestbraccianti.data.entity.Worker
import kotlinx.coroutines.flow.Flow

class WorkerRepository(private val workerDao: WorkerDao) {
    val activeWorkers: Flow<List<Worker>> = workerDao.getActiveWorkers()
    val allWorkers: Flow<List<Worker>> = workerDao.getAllWorkers()

    suspend fun insertWorker(worker: Worker): Long = workerDao.insertWorker(worker)
    suspend fun updateWorker(worker: Worker) = workerDao.updateWorker(worker)
    suspend fun deleteWorker(worker: Worker) = workerDao.deleteWorker(worker)
    suspend fun getWorkerById(id: Long): Worker? = workerDao.getWorkerById(id)
}
