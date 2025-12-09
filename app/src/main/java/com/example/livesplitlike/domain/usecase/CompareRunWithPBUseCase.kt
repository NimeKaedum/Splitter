package com.example.livesplitlike.domain.usecase

import com.example.livesplitlike.data.repositories.RunRepository
import com.example.livesplitlike.data.repositories.SplitRepository
import com.example.livesplitlike.domain.mapper.toDomain
import com.example.livesplitlike.domain.models.Split
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CompareRunWithPBUseCase @Inject constructor(
    private val runRepository: RunRepository,
    private val splitRepository: SplitRepository,
    private val getBestSegmentsUseCase: GetBestSegmentsUseCase
) {
    fun invoke(runId: Long): Flow<List<Pair<Split, Long?>>> {
        val runFlow = runRepository.getRunWithSplitsFlow(runId)
        val bestFlow = getBestSegmentsUseCase.invoke()

        return runFlow.combine(bestFlow) { runWithSplits, bestSegments ->
            val splits = runWithSplits?.splits?.map { it.toDomain() } ?: emptyList()
            val bestMap = bestSegments.associateBy { it.indexInRun }

            splits.map { split ->
                val best = bestMap[split.indexInRun]?.bestSegmentMillis
                Pair(split, best)
            }
        }
    }
}