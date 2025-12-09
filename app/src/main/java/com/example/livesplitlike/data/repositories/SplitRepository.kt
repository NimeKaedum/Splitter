package com.example.livesplitlike.data.repositories

import com.example.livesplitlike.data.local.model.SplitEntity
import kotlinx.coroutines.flow.Flow

interface SplitRepository {
    suspend fun addSplit(split: SplitEntity): Long
    fun getSplitsForRunFlow(runId: Long): Flow<List<SplitEntity>>
    suspend fun getSplitsForRun(runId: Long): List<SplitEntity>
    suspend fun updateSplit(split: SplitEntity)
    suspend fun deleteSplitsForRun(runId: Long)
    suspend fun resetSplitsForRun(runId: Long)
}