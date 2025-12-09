package com.example.livesplitlike.domain.mapper

import com.example.livesplitlike.data.local.model.RunEntity
import com.example.livesplitlike.data.local.model.SplitEntity
import com.example.livesplitlike.data.local.model.ComparisonEntity
import com.example.livesplitlike.domain.models.Run
import com.example.livesplitlike.domain.models.Split
import com.example.livesplitlike.domain.models.Comparison
import java.time.Instant

// Run <-> RunEntity
fun RunEntity.toDomain(): Run = Run(
    id = this.id,
    name = this.name,
    startedAt = Instant.ofEpochMilli(this.startedAtMillis),
    finishedAt = this.finishedAtMillis?.let { Instant.ofEpochMilli(it) },
    isFinished = this.isFinished,
    createdAt = Instant.ofEpochMilli(this.createdAtMillis)
)

fun Run.toEntity(): RunEntity = RunEntity(
    id = this.id,
    name = this.name,
    startedAtMillis = this.startedAt.toEpochMilli(),
    finishedAtMillis = this.finishedAt?.toEpochMilli(),
    isFinished = this.isFinished,
    createdAtMillis = this.createdAt.toEpochMilli()
)

// Split <-> SplitEntity
fun SplitEntity.toDomain(): Split = Split(
    id = this.id,
    runId = this.runId,
    indexInRun = this.indexInRun,
    name = this.name,
    timeFromStartMillis = this.timeFromStartMillis,
    recordedAt = Instant.ofEpochMilli(this.recordedAtMillis)
)

fun Split.toEntity(): SplitEntity = SplitEntity(
    id = this.id,
    runId = this.runId,
    indexInRun = this.indexInRun,
    name = this.name,
    timeFromStartMillis = this.timeFromStartMillis,
    recordedAtMillis = this.recordedAt.toEpochMilli()
)

// Comparison <-> ComparisonEntity
fun ComparisonEntity.toDomain(): Comparison = Comparison(
    id = this.id,
    name = this.name,
    runIdRef = this.runIdRef,
    createdAt = Instant.ofEpochMilli(this.createdAtMillis)
)

fun Comparison.toEntity(): ComparisonEntity = ComparisonEntity(
    id = this.id,
    name = this.name,
    runIdRef = this.runIdRef,
    createdAtMillis = this.createdAt.toEpochMilli()
)