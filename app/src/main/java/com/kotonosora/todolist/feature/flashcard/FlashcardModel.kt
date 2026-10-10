package com.kotonosora.todolist.feature.flashcard

import com.kotonosora.todolist.domain.model.FlashcardCard
import java.util.UUID

data class Flashcard(
    val id: String = UUID.randomUUID().toString(),
    val word: String,
    val definition: String = "",
    val phonetic: String = "",
    val example: String = "",
    val isMastered: Boolean = false
)

/** Maps a persisted domain card onto the session model (unmastered). */
fun FlashcardCard.toUi(): Flashcard = Flashcard(
    id = id,
    word = word,
    definition = definition,
    phonetic = phonetic,
    example = example
)
