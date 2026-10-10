package com.kotonosora.todolist.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "mood_entries",
    indices = [Index(value = ["createdAt"])]
)
data class MoodEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val emotion: String,
    val note: String = "",
    val linkedNoteId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
