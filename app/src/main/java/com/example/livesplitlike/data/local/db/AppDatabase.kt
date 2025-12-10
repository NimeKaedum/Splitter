package com.example.livesplitlike.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.livesplitlike.data.local.model.*

@Database(
    entities = [
        GroupEntity::class,
        SplitTemplateEntity::class,
        RunEntity::class,
        RunTimeEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun groupDao(): GroupDao
    abstract fun splitTemplateDao(): SplitTemplateDao
    abstract fun runDao(): RunDao
    abstract fun runTimeDao(): RunTimeDao
}