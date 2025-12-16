package com.example.livesplitlike.data.local.db

import androidx.room.*
import com.example.livesplitlike.data.local.model.RunEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RunDao {
    @Query("SELECT * FROM runs WHERE groupId = :groupId ORDER BY createdAtMillis DESC")
    fun getRunsForGroupFlow(groupId: Long): Flow<List<RunEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRun(run: RunEntity): Long

    @Delete
    suspend fun deleteRun(run: RunEntity)

    @Query("SELECT * FROM runs WHERE id = :runId LIMIT 1")
    suspend fun getRunById(runId: Long): RunEntity?

    @Query("DELETE FROM runs WHERE id = :runId")
    suspend fun deleteRunById(runId: Long)

    @Query("SELECT * FROM runs")
    suspend fun getAllSync(): List<RunEntity>

    @Query("DELETE FROM runs")
    suspend fun deleteAll()
}