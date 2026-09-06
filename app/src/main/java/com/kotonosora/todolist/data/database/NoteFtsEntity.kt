package com.kotonosora.todolist.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

@Fts4
@Entity(tableName = "notes_fts")
data class NoteFtsEntity(
    @PrimaryKey @ColumnInfo(name = "rowid") val rowid: Int,
    val noteId: String,
    val title: String,
    val content: String,
    val tags: String
)
