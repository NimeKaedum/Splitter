package com.example.livesplitlike.data.local.db

import androidx.room.*
import com.example.livesplitlike.data.local.model.GroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {
    @Query("SELECT * FROM groups ORDER BY id ASC")
    fun getAllGroupsFlow(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM groups WHERE id = :id LIMIT 1")
    suspend fun getGroupById(id: Long): GroupEntity?

    @Insert
    suspend fun insertGroup(group: GroupEntity): Long

    @Delete
    suspend fun deleteGroup(group: GroupEntity)
}