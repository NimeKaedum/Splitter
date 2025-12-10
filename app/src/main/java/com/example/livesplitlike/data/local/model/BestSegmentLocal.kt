package com.example.livesplitlike.data.local.model

import androidx.room.ColumnInfo

/**
 * DTO para resultados de la query de mejores segmentos por índice.
 * No es @Entity.
 */
data class BestSegmentLocal(
    @ColumnInfo(name = "indexInGroup") val indexInGroup: Int,
    @ColumnInfo(name = "bestSegmentMillis") val bestSegmentMillis: Long?
)