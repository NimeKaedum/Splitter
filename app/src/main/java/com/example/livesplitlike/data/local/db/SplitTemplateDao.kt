package com.example.livesplitlike.data.local.db

import androidx.room.*
import com.example.livesplitlike.data.local.model.SplitTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SplitTemplateDao {
    @Query("SELECT * FROM split_templates WHERE groupId = :groupId ORDER BY indexInGroup ASC")
    fun getTemplatesForGroupFlow(groupId: Long): Flow<List<SplitTemplateEntity>>

    @Query("SELECT * FROM split_templates WHERE groupId = :groupId ORDER BY indexInGroup ASC")
    suspend fun getTemplatesForGroup(groupId: Long): List<SplitTemplateEntity>

    // Insert para prepopulate; no se usa en runtime salvo en callback
    @Insert
    suspend fun insertTemplate(template: SplitTemplateEntity): Long

    @Delete
    suspend fun deleteTemplate(template: SplitTemplateEntity)
}