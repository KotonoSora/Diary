package com.kotonosora.todolist.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "day_markers")
data class DayMarkerEntity(
    @PrimaryKey val date: String,
    val taskCount: Int = 0,
    val noteCount: Int = 0,
    val moodCount: Int = 0,
    val moodEmotion: String? = null
)
