package com.kotonosora.todolist.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "links",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceNoteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sourceNoteId"), Index("targetTitle")]
)
data class LinkEntity(
    @PrimaryKey val id: String, // "sourceNoteId->targetTitle"
    val sourceNoteId: String,
    val targetTitle: String,
    val targetNoteId: String? = null,
    val isResolved: Boolean = false
)
