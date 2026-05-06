package com.kotonosora.todolist.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "media_attachments",
    foreignKeys = [
        ForeignKey(
            entity = TodoEntity::class,
            parentColumns = ["id"],
            childColumns = ["todoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("todoId")]
)
data class MediaEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val todoId: String?,          // null = standalone gallery item (no parent todo)
    val type: String,             // "photo" or "audio"
    val filePath: String,
    val createdAt: Long = System.currentTimeMillis()
)
