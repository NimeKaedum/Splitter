package com.example.livesplitlike.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "comparisons")
data class ComparisonEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    /**
     * referencia a un run (opcional)
     */
    val runIdRef: Long?,
    val createdAtMillis: Long = System.currentTimeMillis()
)