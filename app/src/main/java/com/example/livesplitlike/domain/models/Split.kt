package com.example.livesplitlike.domain.models

import java.time.Instant

data class Split(
    val id: Long,
    val runId: Long,
    val indexInRun: Int,
    val name: String,
    val timeFromStartMillis: Long,
    val recordedAt: Instant
)