package com.example.livesplitlike.domain.usecase

import com.example.livesplitlike.data.repositories.SplitRepository
import javax.inject.Inject

class RecordSplitUseCase @Inject constructor(
    private val splitRepository: SplitRepository
) {
    suspend operator fun invoke(runId: Long, elapsedMillis: Long, name: String? = null): Long? {
        val splits = splitRepository.getSplitsForRun(runId)
        val next = splits.firstOrNull { it.timeFromStartMillis < 0 }
        return if (next != null) {
            val updated = next.copy(
                timeFromStartMillis = elapsedMillis,
                recordedAtMillis = System.currentTimeMillis(),
                name = name ?: next.name
            )
            splitRepository.updateSplit(updated)
            updated.id
        } else {
            null
        }
    }
}