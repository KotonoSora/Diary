package com.kotonosora.todolist.feature.flashcard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotonosora.todolist.domain.usecase.FlashcardUseCases
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

data class DeckDetailUiState(
    val deckId: String = "",
    val name: String = "",
    val description: String = "",
    val cards: List<Flashcard> = emptyList(),
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    val error: String? = null
)

/**
 * DDD: depends on [FlashcardUseCases] (application layer). The
 * repository-based secondary constructor is kept for backward compatibility.
 */
class FlashcardDeckDetailViewModel(
    private val deckId: String,
    private val useCases: FlashcardUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeckDetailUiState(deckId = deckId))
    val uiState: StateFlow<DeckDetailUiState> = _uiState

    init {
        viewModelScope.launch {
            combine(
                useCases.observeDeck(deckId),
                useCases.observeCards(deckId).map { cards ->
                    cards.map { it.toUi() }
                }
            ) { deck, cards ->
                if (deck == null) {
                    DeckDetailUiState(deckId = deckId, isLoading = false, notFound = true)
                } else {
                    DeckDetailUiState(
                        deckId = deckId,
                        name = deck.name,
                        description = deck.description,
                        cards = cards,
                        isLoading = false
                    )
                }
            }.collect { _uiState.value = it }
        }
    }

    fun renameDeck(name: String, description: String) {
        viewModelScope.launch {
            try {
                useCases.renameDeck(deckId, name, description)
                _uiState.value = _uiState.value.copy(error = null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Cannot rename deck")
            }
        }
    }

    fun addCard(word: String, definition: String, phonetic: String, example: String) {
        viewModelScope.launch {
            try {
                useCases.addCard(deckId, word, definition, phonetic, example)
                _uiState.value = _uiState.value.copy(error = null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Cannot add card")
            }
        }
    }

    fun updateCard(card: Flashcard) {
        viewModelScope.launch {
            try {
                useCases.updateCard(
                    card.id, deckId, card.word, card.definition, card.phonetic, card.example
                )
                _uiState.value = _uiState.value.copy(error = null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Cannot update card")
            }
        }
    }

    fun deleteCard(cardId: String) {
        viewModelScope.launch {
            try {
                useCases.deleteCard(cardId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Cannot delete card")
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
