package com.example.livesplitlike.data.local.db

import androidx.room.*
import com.example.livesplitlike.data.local.model.ComparisonEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ComparisonDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComparison(cmp: ComparisonEntity): Long

    @Query("SELECT * FROM comparisons ORDER BY createdAtMillis DESC")
    fun getComparisonsFlow(): Flow<List<ComparisonEntity>>

    @Query("DELETE FROM comparisons WHERE id = :id")
    suspend fun deleteComparison(id: Long)
}