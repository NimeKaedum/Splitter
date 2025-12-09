package com.example.livesplitlike.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "runs")
data class RunEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String = "",
    val startedAtMillis: Long,
    val finishedAtMillis: Long? = null,
    val isFinished: Boolean = false,
    val createdAtMillis: Long = startedAtMillis
)