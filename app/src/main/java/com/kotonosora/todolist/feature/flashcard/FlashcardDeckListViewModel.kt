package com.kotonosora.todolist.feature.flashcard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotonosora.todolist.domain.model.DeckWithMeta
import com.kotonosora.todolist.domain.usecase.FlashcardUseCases
import com.kotonosora.todolist.domain.usecase.PreferencesUseCases
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DeckListUiState(
    val decks: List<DeckWithMeta> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

/**
 * DDD: decks via [FlashcardUseCases], seed flag via [PreferencesUseCases] —
 * never repositories directly.
 */
class FlashcardDeckListViewModel(
    private val useCases: FlashcardUseCases,
    private val prefs: PreferencesUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeckListUiState())
    val uiState: StateFlow<DeckListUiState> = _uiState

    // Kept for backward compat with existing callers: deckId -> mastered count.
    val progress: StateFlow<Map<String, Int>> =
        useCases.observeDecks()
            .map { decks -> decks.associate { it.id to it.mastered } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    init {
        viewModelScope.launch {
            // Seed built-ins exactly once (persisted flag): inferring from
            // table emptiness would resurrect decks the user deleted.
            try {
                val alreadySeeded = prefs.observeHasSeededFlashcards().first()
                if (!alreadySeeded) {
                    useCases.ensureSeeded()
                    prefs.saveHasSeededFlashcards(true)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Cannot load flashcard decks"
                )
            }
            useCases.observeDecks().collect { decks ->
                // error is intentionally preserved: a create/rename/delete
                // failure must not be wiped by an unrelated DB emission.
                _uiState.value = _uiState.value.copy(
                    decks = decks,
                    isLoading = false
                )
            }
        }
    }

    fun createDeck(name: String, description: String, onDone: (String?) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val id = useCases.createDeck(name, description)
                _uiState.value = _uiState.value.copy(error = null)
                onDone(id)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Cannot create deck")
                onDone(null)
            }
        }
    }

    fun renameDeck(deckId: String, name: String, description: String) {
        viewModelScope.launch {
            try {
                useCases.renameDeck(deckId, name, description)
                _uiState.value = _uiState.value.copy(error = null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Cannot rename deck")
            }
        }
    }

    fun deleteDeck(deckId: String) {
        viewModelScope.launch {
            try {
                useCases.deleteDeck(deckId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Cannot delete deck")
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
