package com.kotonosora.todolist.domain.usecase

import com.kotonosora.todolist.domain.model.DeckInfo
import com.kotonosora.todolist.domain.model.DeckWithMeta
import com.kotonosora.todolist.domain.model.FlashcardCard
import com.kotonosora.todolist.domain.repository.FlashcardRepository
import kotlinx.coroutines.flow.Flow

/**
 * Flashcard bounded context — application layer (DDD).
 *
 * ViewModels depend on [FlashcardUseCases], never on [FlashcardRepository]
 * directly. Name/word validation lives here so every caller shares it.
 * Mirrors the [TaskUseCases] bundle pattern.
 */

class ObserveDecksUseCase(private val repository: FlashcardRepository) {
    operator fun invoke(): Flow<List<DeckWithMeta>> = repository.observeDecks()
}

class ObserveCardsUseCase(private val repository: FlashcardRepository) {
    operator fun invoke(deckId: String): Flow<List<FlashcardCard>> =
        repository.observeCards(deckId)
}

class ObserveDeckUseCase(private val repository: FlashcardRepository) {
    operator fun invoke(deckId: String): Flow<DeckWithMeta?> = repository.observeDeck(deckId)
}

class GetCardsOnceUseCase(private val repository: FlashcardRepository) {
    suspend operator fun invoke(deckId: String): List<FlashcardCard> {
        if (deckId.isBlank()) return emptyList()
        return repository.getCardsOnce(deckId)
    }
}

class GetDeckUseCase(private val repository: FlashcardRepository) {
    suspend operator fun invoke(deckId: String): DeckInfo? {
        if (deckId.isBlank()) return null
        return repository.getDeck(deckId)
    }
}

class EnsureFlashcardsSeededUseCase(private val repository: FlashcardRepository) {
    suspend operator fun invoke() = repository.ensureSeeded()
}

class CreateDeckUseCase(private val repository: FlashcardRepository) {
    suspend operator fun invoke(name: String, description: String): String {
        require(name.isNotBlank()) { "Deck name must not be blank" }
        return repository.createDeck(name.trim(), description)
    }
}

class RenameDeckUseCase(private val repository: FlashcardRepository) {
    suspend operator fun invoke(deckId: String, name: String, description: String) {
        require(deckId.isNotBlank()) { "deckId must not be blank" }
        require(name.isNotBlank()) { "Deck name must not be blank" }
        repository.renameDeck(deckId, name.trim(), description)
    }
}

class DeleteDeckUseCase(private val repository: FlashcardRepository) {
    suspend operator fun invoke(deckId: String) {
        if (deckId.isBlank()) return
        repository.deleteDeck(deckId)
    }
}

class AddCardUseCase(private val repository: FlashcardRepository) {
    suspend operator fun invoke(
        deckId: String,
        word: String,
        definition: String = "",
        phonetic: String = "",
        example: String = ""
    ): String {
        require(deckId.isNotBlank()) { "deckId must not be blank" }
        require(word.isNotBlank()) { "Card word must not be blank" }
        return repository.addCard(deckId, word.trim(), definition, phonetic, example)
    }
}

class UpdateCardUseCase(private val repository: FlashcardRepository) {
    suspend operator fun invoke(
        cardId: String,
        deckId: String,
        word: String,
        definition: String = "",
        phonetic: String = "",
        example: String = ""
    ) {
        require(cardId.isNotBlank()) { "cardId must not be blank" }
        require(deckId.isNotBlank()) { "deckId must not be blank" }
        require(word.isNotBlank()) { "Card word must not be blank" }
        repository.updateCard(cardId, deckId, word.trim(), definition, phonetic, example)
    }
}

class DeleteCardUseCase(private val repository: FlashcardRepository) {
    suspend operator fun invoke(cardId: String) {
        if (cardId.isBlank()) return
        repository.deleteCard(cardId)
    }
}

class SaveFullRunUseCase(private val repository: FlashcardRepository) {
    suspend operator fun invoke(deckId: String, mastered: Int, total: Int) {
        if (deckId.isBlank()) return
        repository.saveFullRun(deckId, mastered.coerceAtLeast(0), total.coerceAtLeast(0))
    }
}

class ProgressForDecksUseCase(private val repository: FlashcardRepository) {
    operator fun invoke(deckIds: List<String>): Flow<Map<String, Int>> =
        repository.progressFor(deckIds)
}

data class FlashcardUseCases(
    val observeDecks: ObserveDecksUseCase,
    val observeCards: ObserveCardsUseCase,
    val observeDeck: ObserveDeckUseCase,
    val getCardsOnce: GetCardsOnceUseCase,
    val getDeck: GetDeckUseCase,
    val ensureSeeded: EnsureFlashcardsSeededUseCase,
    val createDeck: CreateDeckUseCase,
    val renameDeck: RenameDeckUseCase,
    val deleteDeck: DeleteDeckUseCase,
    val addCard: AddCardUseCase,
    val updateCard: UpdateCardUseCase,
    val deleteCard: DeleteCardUseCase,
    val saveFullRun: SaveFullRunUseCase,
    val progressFor: ProgressForDecksUseCase
) {
    companion object {
        fun from(repository: FlashcardRepository): FlashcardUseCases = FlashcardUseCases(
            observeDecks = ObserveDecksUseCase(repository),
            observeCards = ObserveCardsUseCase(repository),
            observeDeck = ObserveDeckUseCase(repository),
            getCardsOnce = GetCardsOnceUseCase(repository),
            getDeck = GetDeckUseCase(repository),
            ensureSeeded = EnsureFlashcardsSeededUseCase(repository),
            createDeck = CreateDeckUseCase(repository),
            renameDeck = RenameDeckUseCase(repository),
            deleteDeck = DeleteDeckUseCase(repository),
            addCard = AddCardUseCase(repository),
            updateCard = UpdateCardUseCase(repository),
            deleteCard = DeleteCardUseCase(repository),
            saveFullRun = SaveFullRunUseCase(repository),
            progressFor = ProgressForDecksUseCase(repository)
        )
    }
}
