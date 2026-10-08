package com.kotonosora.todolist.domain.model

/**
 * Deck list row: deck fields plus live card/mastered counts. Persistence
 * lives in the flashcard tables; this is what the deck list UI observes.
 */
data class DeckWithMeta(
    val id: String,
    val name: String,
    val description: String,
    val isBuiltIn: Boolean,
    val cardCount: Int,
    val mastered: Int
)

/** One persisted flashcard. Session mastery (`isMastered`) stays on the UI model. */
data class FlashcardCard(
    val id: String,
    val deckId: String,
    val word: String,
    val definition: String = "",
    val phonetic: String = "",
    val example: String = "",
    val position: Int = 0
)

/** Basic deck fields for one-shot lookups (no live counts). */
data class DeckInfo(
    val id: String,
    val name: String,
    val description: String,
    val isBuiltIn: Boolean
)
