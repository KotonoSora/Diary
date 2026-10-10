package com.kotonosora.todolist.data.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface FlashcardDeckDao {

    @Query("SELECT * FROM flashcard_decks ORDER BY isBuiltIn DESC, createdAt ASC")
    fun observeAll(): Flow<List<FlashcardDeckEntity>>

    @Query("SELECT * FROM flashcard_decks ORDER BY isBuiltIn DESC, createdAt ASC")
    suspend fun getAllOnce(): List<FlashcardDeckEntity>

    @Query("SELECT * FROM flashcard_decks WHERE id = :deckId LIMIT 1")
    suspend fun getById(deckId: String): FlashcardDeckEntity?

    @Upsert
    suspend fun upsert(deck: FlashcardDeckEntity)

    @Query("DELETE FROM flashcard_decks WHERE id = :deckId")
    suspend fun deleteById(deckId: String)
}
