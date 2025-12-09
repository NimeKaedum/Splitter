package com.example.livesplitlike.data.repositories.impl

import com.example.livesplitlike.data.local.db.RunDao
import com.example.livesplitlike.data.local.model.RunEntity
import com.example.livesplitlike.data.local.model.RunWithSplits
import com.example.livesplitlike.data.repositories.RunRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RunRepositoryImpl @Inject constructor(
    private val runDao: RunDao
) : RunRepository {
    override suspend fun createRun(name: String, startedAtMillis: Long): Long {
        val run = RunEntity(
            name = name,
            startedAtMillis = startedAtMillis,
            createdAtMillis = startedAtMillis
        )
        return runDao.insertRun(run)
    }

    override suspend fun finishRun(runId: Long, finishedAtMillis: Long) {
        val existing = runDao.getRunById(runId) ?: return
        val updated = existing.copy(finishedAtMillis = finishedAtMillis, isFinished = true)
        runDao.updateRun(updated)
    }

    override suspend fun deleteRun(runId: Long) = runDao.deleteRunById(runId)

    override fun getAllRunsFlow(): Flow<List<RunEntity>> = runDao.getAllRunsFlow()

    override fun getRunWithSplitsFlow(runId: Long): Flow<RunWithSplits?> = runDao.getRunWithSplitsFlow(runId)

    override suspend fun getRunById(runId: Long): RunEntity? = runDao.getRunById(runId)

    override suspend fun updateRunStart(runId: Long, startedAtMillis: Long) {
        val existing = runDao.getRunById(runId) ?: return
        val updated = existing.copy(
            startedAtMillis = startedAtMillis,
            isFinished = false,
            finishedAtMillis = null
        )
        runDao.updateRun(updated)
    }

    override suspend fun resetRunStart(runId: Long) {
        val existing = runDao.getRunById(runId) ?: return
        val updated = existing.copy(
            startedAtMillis = 0L,
            isFinished = false,
            finishedAtMillis = null
        )
        runDao.updateRun(updated)
    }
}