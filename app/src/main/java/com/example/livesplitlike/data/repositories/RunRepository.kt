package com.example.livesplitlike.data.repositories

import com.example.livesplitlike.data.local.model.RunEntity
import com.example.livesplitlike.data.local.model.RunWithSplits
import kotlinx.coroutines.flow.Flow

interface RunRepository {
    suspend fun createRun(name: String, startedAtMillis: Long = System.currentTimeMillis()): Long
    suspend fun finishRun(runId: Long, finishedAtMillis: Long = System.currentTimeMillis())
    suspend fun deleteRun(runId: Long)
    fun getAllRunsFlow(): Flow<List<RunEntity>>
    fun getRunWithSplitsFlow(runId: Long): Flow<RunWithSplits?>
    suspend fun getRunById(runId: Long): RunEntity?
    suspend fun updateRunStart(runId: Long, startedAtMillis: Long)
    suspend fun resetRunStart(runId: Long)
}