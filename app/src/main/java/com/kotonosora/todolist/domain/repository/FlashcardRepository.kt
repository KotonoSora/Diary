package com.kotonosora.todolist.domain.repository

import com.kotonosora.todolist.domain.model.DeckInfo
import com.kotonosora.todolist.domain.model.DeckWithMeta
import com.kotonosora.todolist.domain.model.FlashcardCard
import kotlinx.coroutines.flow.Flow

/**
 * Flashcard decks, cards and run progress. Implemented by
 * `FlashcardRepositoryImpl` over the deck database; ViewModels depend on this
 * interface, never on the implementation or entities directly.
 */
interface FlashcardRepository {
    fun observeDecks(): Flow<List<DeckWithMeta>>

    fun observeCards(deckId: String): Flow<List<FlashcardCard>>

    fun observeDeck(deckId: String): Flow<DeckWithMeta?>

    suspend fun getCardsOnce(deckId: String): List<FlashcardCard>

    suspend fun getDeck(deckId: String): DeckInfo?

    /** One-shot seeding of the built-in decks. */
    suspend fun ensureSeeded()

    suspend fun createDeck(name: String, description: String): String

    suspend fun renameDeck(deckId: String, name: String, description: String)

    suspend fun deleteDeck(deckId: String)

    suspend fun addCard(
        deckId: String,
        word: String,
        definition: String = "",
        phonetic: String = "",
        example: String = ""
    ): String

    suspend fun updateCard(
        cardId: String,
        deckId: String,
        word: String,
        definition: String = "",
        phonetic: String = "",
        example: String = ""
    )

    suspend fun deleteCard(cardId: String)

    suspend fun saveFullRun(deckId: String, mastered: Int, total: Int)

    fun progressFor(deckIds: List<String>): Flow<Map<String, Int>>
}
