package com.example.livesplitlike.data.local.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "splits",
    foreignKeys = [ForeignKey(
        entity = RunEntity::class,
        parentColumns = ["id"],
        childColumns = ["runId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("runId")]
)
data class SplitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val runId: Long,
    val indexInRun: Int,
    val name: String = "",
    /**
     * elapsed desde inicio del run hasta este split
     */
    val timeFromStartMillis: Long,
    /**
     * timestamp cuando se registró el split
     */
    val recordedAtMillis: Long = System.currentTimeMillis()
)