package com.kotonosora.todolist.feature.flashcard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotonosora.todolist.data.repository.DeckProgressRepository
import com.kotonosora.todolist.data.repository.DeckWithMeta
import com.kotonosora.todolist.data.repository.FlashcardRepository
import com.kotonosora.todolist.data.repository.UserPreferencesRepository
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

class FlashcardDeckListViewModel(
    private val flashcardRepository: FlashcardRepository? = null,
    deckProgressRepository: DeckProgressRepository? = null,
    private val userPreferencesRepository: UserPreferencesRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeckListUiState())
    val uiState: StateFlow<DeckListUiState> = _uiState

    // Kept for backward compat with existing callers: deckId -> mastered count.
    val progress: StateFlow<Map<String, Int>> =
        if (flashcardRepository != null) {
            flashcardRepository.observeDecks()
                .map { decks -> decks.associate { it.id to it.mastered } }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
        } else if (deckProgressRepository != null) {
            deckProgressRepository
                .progressFor(demoDecks.map { it.id })
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
        } else {
            MutableStateFlow(emptyMap())
        }

    init {
        if (flashcardRepository == null) {
            _uiState.value = DeckListUiState(
                decks = demoDecks.map {
                    DeckWithMeta(it.id, it.name, it.description, true, it.cardCount, 0)
                },
                isLoading = false
            )
        } else {
            viewModelScope.launch {
                // Seed built-ins exactly once (persisted flag): inferring from
                // table emptiness would resurrect decks the user deleted.
                try {
                    val alreadySeeded =
                        userPreferencesRepository?.hasSeededFlashcards?.first() ?: false
                    if (!alreadySeeded) {
                        flashcardRepository.ensureSeeded()
                        userPreferencesRepository?.saveHasSeededFlashcards(true)
                    }
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        error = e.message ?: "Cannot load flashcard decks"
                    )
                }
                flashcardRepository.observeDecks().collect { decks ->
                    // error is intentionally preserved: a create/rename/delete
                    // failure must not be wiped by an unrelated DB emission.
                    _uiState.value = _uiState.value.copy(
                        decks = decks,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun createDeck(name: String, description: String, onDone: (String?) -> Unit = {}) {
        val repo = flashcardRepository ?: run { onDone(null); return }
        viewModelScope.launch {
            try {
                val id = repo.createDeck(name, description)
                _uiState.value = _uiState.value.copy(error = null)
                onDone(id)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Cannot create deck")
                onDone(null)
            }
        }
    }

    fun renameDeck(deckId: String, name: String, description: String) {
        val repo = flashcardRepository ?: return
        viewModelScope.launch {
            try {
                repo.renameDeck(deckId, name, description)
                _uiState.value = _uiState.value.copy(error = null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Cannot rename deck")
            }
        }
    }

    fun deleteDeck(deckId: String) {
        val repo = flashcardRepository ?: return
        viewModelScope.launch {
            try {
                repo.deleteDeck(deckId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Cannot delete deck")
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
