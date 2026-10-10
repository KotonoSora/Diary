package com.kotonosora.todolist.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "zettel_metadata",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("noteId")]
)
data class ZettelMetadataEntity(
    @PrimaryKey val noteId: String,
    val uid: String,
    val noteType: String = "PERMANENT",
    val author: String? = null,
    val sourceUrl: String? = null,
    val paraCategory: String? = null
)
