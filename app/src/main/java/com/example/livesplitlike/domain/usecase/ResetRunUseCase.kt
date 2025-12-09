package com.example.livesplitlike.domain.usecase

import com.example.livesplitlike.data.repositories.RunRepository
import com.example.livesplitlike.data.repositories.SplitRepository
import javax.inject.Inject

class ResetRunUseCase @Inject constructor(
    private val runRepository: RunRepository,
    private val splitRepository: SplitRepository
) {
    suspend operator fun invoke(runId: Long) {
        splitRepository.resetSplitsForRun(runId)
        runRepository.resetRunStart(runId)
    }
}