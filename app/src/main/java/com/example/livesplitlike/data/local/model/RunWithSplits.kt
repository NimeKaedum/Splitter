package com.example.livesplitlike.data.local.model

import androidx.room.Embedded
import androidx.room.Relation

data class RunWithSplits(
    @Embedded val run: RunEntity,
    @Relation(parentColumn = "id", entityColumn = "runId")
    val splits: List<SplitEntity>
)