package com.example.livesplitlike.domain.usecase

import com.example.livesplitlike.data.repositories.RunRepository
import javax.inject.Inject

class StartRunUseCase @Inject constructor(
    private val runRepository: RunRepository
) {
    suspend operator fun invoke(runId: Long?): Long {
        val now = System.currentTimeMillis()
        return if (runId != null) {
            runRepository.updateRunStart(runId, now)
            runId
        } else {
            runRepository.createRun(name = "Run (auto)", startedAtMillis = now)
        }
    }
}