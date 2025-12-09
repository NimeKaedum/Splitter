package com.example.livesplitlike.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.livesplitlike.data.local.model.SplitEntity
import com.example.livesplitlike.data.local.model.RunEntity
import com.example.livesplitlike.data.local.model.ComparisonEntity

@Database(
    entities = [RunEntity::class, SplitEntity::class, ComparisonEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun runDao(): RunDao
    abstract fun splitDao(): SplitDao
    abstract fun comparisonDao(): ComparisonDao
}