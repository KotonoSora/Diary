package com.kotonosora.todolist.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "flashcard_cards",
    foreignKeys = [
        ForeignKey(
            entity = FlashcardDeckEntity::class,
            parentColumns = ["id"],
            childColumns = ["deckId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("deckId"), Index(value = ["deckId", "position"])]
)
data class FlashcardCardEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val deckId: String,
    val word: String,
    val definition: String = "",
    val phonetic: String = "",
    val example: String = "",
    val position: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
