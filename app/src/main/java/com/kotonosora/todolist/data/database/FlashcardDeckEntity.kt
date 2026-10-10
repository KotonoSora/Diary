package com.kotonosora.todolist.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "flashcard_decks")
data class FlashcardDeckEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String = "",
    val isBuiltIn: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
