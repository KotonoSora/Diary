package com.kotonosora.todolist.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "tags",
    primaryKeys = ["noteId", "tagName"],
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("noteId"), Index("tagName")]
)
data class TagEntity(
    val noteId: String,
    val tagName: String
)
