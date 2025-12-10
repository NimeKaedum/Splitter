package com.example.livesplitlike.data.local.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "split_templates",
    foreignKeys = [ForeignKey(
        entity = GroupEntity::class,
        parentColumns = ["id"],
        childColumns = ["groupId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("groupId")]
)
data class SplitTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val groupId: Long,
    val indexInGroup: Int,
    val name: String
)