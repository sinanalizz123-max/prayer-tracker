package com.praytracker.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasbih")
data class TasbihEntity(
    @PrimaryKey val id: Long,
    val arabic: String,
    val translation: String? = null,
    val target: Int = 33,
    val count: Int = 0,
    val isCustom: Boolean = false,
    val orderIndex: Int = 0,
)