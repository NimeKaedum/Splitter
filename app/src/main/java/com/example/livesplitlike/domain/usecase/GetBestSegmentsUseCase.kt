package com.example.livesplitlike.domain.usecase

import com.example.livesplitlike.data.local.db.SplitDao
import com.example.livesplitlike.domain.models.BestSegment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetBestSegmentsUseCase @Inject constructor(
    private val splitDao: SplitDao
) {
    fun invoke(): Flow<List<BestSegment>> {
        return splitDao.getBestSegmentTimesFlow().map { list ->
            list.map { BestSegment(indexInRun = it.indexInRun, bestSegmentMillis = it.bestSegmentMillis) }
        }
    }
}