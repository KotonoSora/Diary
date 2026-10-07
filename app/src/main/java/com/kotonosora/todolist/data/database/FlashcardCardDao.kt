package com.kotonosora.todolist.data.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

data class DeckCardCount(
    val deckId: String,
    val count: Int
)

@Dao
interface FlashcardCardDao {

    @Query("SELECT * FROM flashcard_cards WHERE deckId = :deckId ORDER BY position ASC, createdAt ASC")
    fun observeByDeck(deckId: String): Flow<List<FlashcardCardEntity>>

    @Query("SELECT * FROM flashcard_cards WHERE deckId = :deckId ORDER BY position ASC, createdAt ASC")
    suspend fun getByDeckOnce(deckId: String): List<FlashcardCardEntity>

    @Query("SELECT deckId, COUNT(*) as count FROM flashcard_cards GROUP BY deckId")
    fun observeCounts(): Flow<List<DeckCardCount>>

    @Query("SELECT COALESCE(MAX(position), -1) FROM flashcard_cards WHERE deckId = :deckId")
    suspend fun maxPosition(deckId: String): Int

    @Upsert
    suspend fun upsert(card: FlashcardCardEntity)

    @Upsert
    suspend fun upsertAll(cards: List<FlashcardCardEntity>)

    @Query("DELETE FROM flashcard_cards WHERE id = :cardId")
    suspend fun deleteById(cardId: String)

    @Query("DELETE FROM flashcard_cards WHERE deckId = :deckId")
    suspend fun deleteByDeck(deckId: String)
}
