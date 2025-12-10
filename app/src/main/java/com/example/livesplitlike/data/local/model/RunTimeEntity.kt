package com.example.livesplitlike.data.local.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "run_times",
    indices = [Index(value = ["runId","groupId","splitIndex"], unique = true)]
)
data class RunTimeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val runId: Long,
    val groupId: Long,
    val splitIndex: Int,
    val timeFromStartMillis: Long,
    val recordedAtMillis: Long = System.currentTimeMillis()
)