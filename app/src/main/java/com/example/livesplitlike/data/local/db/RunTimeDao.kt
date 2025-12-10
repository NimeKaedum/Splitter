package com.example.livesplitlike.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.livesplitlike.data.local.model.BestSegmentLocal
import com.example.livesplitlike.data.local.model.RunTimeEntity
import com.example.livesplitlike.data.local.model.RunTotal
import kotlinx.coroutines.flow.Flow

@Dao
interface RunTimeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRunTime(rt: RunTimeEntity): Long

    @Query("SELECT * FROM run_times WHERE runId = :runId AND groupId = :groupId ORDER BY splitIndex ASC")
    suspend fun getTimesForRun(runId: Long, groupId: Long): List<RunTimeEntity>

    @Query("SELECT * FROM run_times WHERE runId = :runId AND groupId = :groupId ORDER BY splitIndex ASC")
    fun getTimesForRunFlow(runId: Long, groupId: Long): Flow<List<RunTimeEntity>>

    @Query("""
        SELECT runId, MAX(timeFromStartMillis) as total
        FROM run_times
        WHERE groupId = :groupId
        GROUP BY runId
    """)
    suspend fun getTotalsByRun(groupId: Long): List<RunTotal>

    /**
     * SPLITBEST: para cada splitIndex (indexInGroup) calcula el segmentMillis por run
     * (timeFromStartMillis - previous timeFromStartMillis, prev = 0 para index 0),
     * y luego toma MIN(segmentMillis) entre todas las runs.
     *
     * Devuelve BestSegmentLocal(indexInGroup, bestSegmentMillis).
     */
    @Query("""
        WITH cumulative AS (
            SELECT runId, splitIndex, timeFromStartMillis
            FROM run_times
            WHERE groupId = :groupId
        ),
        segs AS (
            SELECT c.runId, c.splitIndex,
                   (c.timeFromStartMillis - COALESCE(p.timeFromStartMillis, 0)) AS segmentMillis
            FROM cumulative c
            LEFT JOIN cumulative p
              ON p.runId = c.runId AND p.splitIndex = c.splitIndex - 1
        )
        SELECT splitIndex AS indexInGroup, MIN(segmentMillis) AS bestSegmentMillis
        FROM segs
        GROUP BY splitIndex
        ORDER BY splitIndex ASC
    """)
    fun getBestSegmentTimesFlow(groupId: Long): Flow<List<BestSegmentLocal>>
}