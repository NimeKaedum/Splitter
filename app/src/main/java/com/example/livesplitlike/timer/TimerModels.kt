package com.example.livesplitlike.timer

data class ComparisonItem(
    val indexInGroup: Int,
    val name: String,
    val currentMillis: Long?,
    val bestMillis: Long,
    val diffMillis: Long?,
    val status: ComparisonStatus
)

enum class ComparisonStatus {
    NONE,
    GOLD,
    GAIN_GAINING,
    GAIN_LOSING,
    LOSS_GAINING,
    LOSS_LOSING
}