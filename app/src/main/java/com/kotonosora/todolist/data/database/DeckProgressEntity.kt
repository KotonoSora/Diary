package com.kotonosora.todolist.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "deck_progress")
data class DeckProgressEntity(
    @PrimaryKey val deckId: String,
    val mastered: Int,
    val total: Int,
    val updatedAt: Long = System.currentTimeMillis()
)
