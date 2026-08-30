package com.praytracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasbih_items")
data class TasbihItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val arabicText: String,
    val englishText: String,
    val currentCount: Int = 0,
    val targetCount: Int = 33,
    val isCustom: Boolean = false
)
