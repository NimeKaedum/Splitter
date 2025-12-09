package com.example.livesplitlike.data.local.db

import androidx.room.*
import com.example.livesplitlike.data.local.model.RunEntity
import com.example.livesplitlike.data.local.model.RunWithSplits
import kotlinx.coroutines.flow.Flow

@Dao
interface RunDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRun(run: RunEntity): Long

    @Update
    suspend fun updateRun(run: RunEntity)

    @Query("DELETE FROM runs WHERE id = :runId")
    suspend fun deleteRunById(runId: Long)

    @Query("SELECT * FROM runs ORDER BY createdAtMillis DESC")
    fun getAllRunsFlow(): Flow<List<RunEntity>>

    @Transaction
    @Query("SELECT * FROM runs WHERE id = :runId")
    fun getRunWithSplitsFlow(runId: Long): Flow<RunWithSplits?>

    @Query("SELECT * FROM runs WHERE id = :runId LIMIT 1")
    suspend fun getRunById(runId: Long): RunEntity?
}