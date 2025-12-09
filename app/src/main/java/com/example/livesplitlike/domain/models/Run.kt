package com.example.livesplitlike.domain.models

import java.time.Instant

data class Run(
    val id: Long,
    val name: String,
    val startedAt: Instant,
    val finishedAt: Instant?,
    val isFinished: Boolean,
    val createdAt: Instant
)