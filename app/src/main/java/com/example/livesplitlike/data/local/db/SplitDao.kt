package com.example.livesplitlike.data.local.db

import androidx.room.*
import com.example.livesplitlike.data.local.model.SplitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SplitDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSplit(split: SplitEntity): Long

    @Update
    suspend fun updateSplit(split: SplitEntity)

    @Query("DELETE FROM splits WHERE runId = :runId")
    suspend fun deleteSplitsForRun(runId: Long)

    @Query("SELECT * FROM splits WHERE runId = :runId ORDER BY indexInRun ASC")
    fun getSplitsForRunFlow(runId: Long): Flow<List<SplitEntity>>

    @Query("SELECT * FROM splits WHERE runId = :runId ORDER BY indexInRun ASC")
    suspend fun getSplitsForRun(runId: Long): List<SplitEntity>

    @Query("SELECT * FROM splits WHERE id = :splitId LIMIT 1")
    suspend fun getSplitById(splitId: Long): SplitEntity?

    /**
     * Best segments por índice: calcula el mejor tiempo del segmento
     * como la diferencia entre el elapsed del split y el elapsed del split anterior.
     * Considera sólo splits con timeFromStartMillis >= 0.
     */
    @Query("""
        WITH ordered AS (
            SELECT runId, indexInRun, timeFromStartMillis
            FROM splits
            WHERE timeFromStartMillis >= 0
        ),
        segs AS (
            SELECT o.indexInRun AS indexInRun,
                   (o.timeFromStartMillis - COALESCE(p.timeFromStartMillis, 0)) AS segmentMillis
            FROM ordered o
            LEFT JOIN ordered p
              ON p.runId = o.runId
             AND p.indexInRun = o.indexInRun - 1
        )
        SELECT indexInRun AS indexInRun,
               MIN(segmentMillis) AS bestSegmentMillis
        FROM segs
        GROUP BY indexInRun
        ORDER BY indexInRun ASC
    """)
    fun getBestSegmentTimesFlow(): Flow<List<BestSegmentLocal>>
}

/**
 * DTO local para mejor segmento por índice.
 */
data class BestSegmentLocal(
    val indexInRun: Int,
    val bestSegmentMillis: Long?
)