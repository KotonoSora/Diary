package com.kotonosora.todolist.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kotonosora.todolist.domain.model.ZettelUidGenerator

@Entity(
    tableName = "notes",
    indices = [
        Index(value = ["updatedAt"]),
        Index(value = ["title"]),
        Index(value = ["relativePath"])
    ]
)
data class NoteEntity(
    @PrimaryKey val id: String, // Relative path from Vault root (e.g. "Work/202603011200-Meeting.md")
    val uid: String = ZettelUidGenerator.generateUid(),
    val title: String,
    val noteType: String = "PERMANENT",
    val relativePath: String,
    val content: String,
    val fileFormat: String = "md",
    val updatedAt: Long = System.currentTimeMillis(),
    val sizeBytes: Long = 0L
)
