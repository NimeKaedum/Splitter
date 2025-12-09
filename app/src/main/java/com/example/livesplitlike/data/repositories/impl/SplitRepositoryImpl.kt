package com.example.livesplitlike.data.repositories.impl

import com.example.livesplitlike.data.local.db.SplitDao
import com.example.livesplitlike.data.local.model.SplitEntity
import com.example.livesplitlike.data.repositories.SplitRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SplitRepositoryImpl @Inject constructor(
    private val splitDao: SplitDao
) : SplitRepository {
    override suspend fun addSplit(split: SplitEntity): Long = splitDao.insertSplit(split)
    override fun getSplitsForRunFlow(runId: Long): Flow<List<SplitEntity>> = splitDao.getSplitsForRunFlow(runId)
    override suspend fun getSplitsForRun(runId: Long): List<SplitEntity> = splitDao.getSplitsForRun(runId)
    override suspend fun updateSplit(split: SplitEntity) = splitDao.updateSplit(split)
    override suspend fun deleteSplitsForRun(runId: Long) = splitDao.deleteSplitsForRun(runId)
    override suspend fun resetSplitsForRun(runId: Long) {
        val splits = splitDao.getSplitsForRun(runId)
        splits.forEach { s ->
            val reset = s.copy(timeFromStartMillis = -1L, recordedAtMillis = 0L)
            splitDao.updateSplit(reset)
        }
    }
}