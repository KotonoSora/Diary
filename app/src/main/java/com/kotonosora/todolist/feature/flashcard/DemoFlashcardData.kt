package com.kotonosora.todolist.feature.flashcard

import com.kotonosora.todolist.data.flashcard.FlashcardSeedData

/**
 * Built-in deck seed data for demo playback. Thin wrapper over
 * `FlashcardSeedData` (data): the raw content lives there so the repository
 * can seed without depending on feature — this object only maps it onto the
 * session [Flashcard] model. Single source of truth for demo decks.
 */
object DemoFlashcardData {
    val seedDecks = FlashcardSeedData.seedDecks

    fun demoCards(deckId: String): Pair<String, List<Flashcard>> {
        val (title, cards) = FlashcardSeedData.demoCards(deckId)
        return title to cards.map { it.toUi() }
    }

    private fun FlashcardSeedData.SeedCard.toUi(): Flashcard = Flashcard(
        word = word,
        definition = definition,
        phonetic = phonetic,
        example = example
    )
}
