package com.kotonosora.todolist.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String, // Relative path from Vault root (e.g. "Work/Meeting.md")
    val title: String,
    val relativePath: String,
    val content: String,
    val fileFormat: String = "md",
    val updatedAt: Long = System.currentTimeMillis(),
    val sizeBytes: Long = 0L
)
