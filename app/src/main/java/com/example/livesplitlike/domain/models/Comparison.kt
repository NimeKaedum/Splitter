package com.example.livesplitlike.domain.models

import java.time.Instant

data class Comparison(
    val id: Long,
    val name: String,
    val runIdRef: Long?,
    val createdAt: Instant
)