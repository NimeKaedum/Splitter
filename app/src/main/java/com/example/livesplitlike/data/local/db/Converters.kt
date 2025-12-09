package com.example.livesplitlike.data.local.db

import androidx.room.TypeConverter
import java.time.Instant

class Converters {
    @TypeConverter
    fun fromInstantToLong(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun fromLongToInstant(value: Long?): Instant? = value?.let { Instant.ofEpochMilli(it) }
}