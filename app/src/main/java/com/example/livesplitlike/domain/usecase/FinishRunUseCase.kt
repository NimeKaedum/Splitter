package com.example.livesplitlike.domain.usecase

import com.example.livesplitlike.data.repositories.RunRepository
import javax.inject.Inject

class FinishRunUseCase @Inject constructor(
    private val runRepository: RunRepository
) {
    suspend operator fun invoke(runId: Long) {
        runRepository.finishRun(runId = runId)
    }
}