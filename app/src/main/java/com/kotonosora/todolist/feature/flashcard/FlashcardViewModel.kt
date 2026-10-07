package com.kotonosora.todolist.feature.flashcard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotonosora.todolist.data.repository.FlashcardRepository
import com.kotonosora.todolist.domain.repository.VaultRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FlashcardViewModel(
    private val vaultRepository: VaultRepository,
    private val flashcardRepository: FlashcardRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(FlashcardUiState())
    val uiState: StateFlow<FlashcardUiState> = _uiState.asStateFlow()

    private var currentDeckId: String? = null
    private var fullDeckSize: Int = 0
    private var originalCards: List<Flashcard> = emptyList()

    fun loadNote(noteId: String, isDemo: Boolean = false) {
        currentDeckId = noteId
        viewModelScope.launch {
            // DB is source of truth — covers seeded built-ins + user decks.
            // Check deck existence FIRST so a newly created (still empty) deck
            // shows its own empty state instead of falling through to demo data.
            // Exceptions are surfaced as a load error, not silently downgraded to
            // "no such deck" (which would show unrelated demo cards).
            val deck = try {
                flashcardRepository?.getDeckById(noteId)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Cannot load deck")
                }
                return@launch
            }
            if (deck != null) {
                val dbCards = try {
                    flashcardRepository?.getCardsOnce(noteId).orEmpty()
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(isLoading = false, error = e.message ?: "Cannot load cards")
                    }
                    return@launch
                }
                setSession(title = deck.name, cards = dbCards, fromDb = true)
                return@launch
            }
            if (isDemo) {
                val (deckTitle, demoCards) = DemoFlashcardData.demoCards(noteId)
                setSession(title = deckTitle, cards = demoCards)
            } else {
                val note = vaultRepository.getNoteById(noteId)
                val markdownText = note?.content ?: ""
                val cards = MarkdownParser.parseMarkdownFlashcards(markdownText)
                setSession(title = note?.title ?: "Note Flashcards", cards = cards)
            }
        }
    }

    private fun setSession(title: String, cards: List<Flashcard>, fromDb: Boolean = false) {
        fullDeckSize = cards.size
        originalCards = cards
        _uiState.update {
            it.copy(
                deckTitle = title,
                cards = cards,
                totalCardsCount = cards.size,
                currentCardIndex = 0,
                masteredCards = emptyList(),
                reviewCards = emptyList(),
                isCardFlipped = false,
                isFinished = cards.isEmpty(),
                canManageCards = fromDb,
                isLoading = false,
                error = null
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    // Only used by loadNote's demo fallback for deck ids with no DB row.
    private fun generateDemoCards(deckId: String): Pair<String, List<Flashcard>> =
        DemoFlashcardData.demoCards(deckId)

    fun flipCard() {
        _uiState.update { it.copy(isCardFlipped = !it.isCardFlipped) }
    }

    fun markAsMastered() {
        _uiState.update { currentState ->
            if (currentState.cards.isNotEmpty()) {
                val current = currentState.cards.first()
                val remaining = currentState.cards.drop(1)
                val newMastered = currentState.masteredCards + current.copy(isMastered = true)
                currentState.copy(
                    cards = remaining,
                    masteredCards = newMastered,
                    currentCardIndex = currentState.currentCardIndex + 1,
                    isCardFlipped = false,
                    isFinished = remaining.isEmpty()
                )
            } else currentState
        }
        persistProgressIfFinished()
    }

    fun markForReview() {
        _uiState.update { currentState ->
            if (currentState.cards.isNotEmpty()) {
                val current = currentState.cards.first()
                val remaining = currentState.cards.drop(1)
                val newReview = currentState.reviewCards + current
                currentState.copy(
                    cards = remaining,
                    reviewCards = newReview,
                    currentCardIndex = currentState.currentCardIndex + 1,
                    isCardFlipped = false,
                    isFinished = remaining.isEmpty()
                )
            } else currentState
        }
        persistProgressIfFinished()
    }

    private fun persistProgressIfFinished() {
        val deckId = currentDeckId ?: return
        val state = _uiState.value
        if (!state.isFinished) return
        if (state.totalCardsCount < fullDeckSize) return
        viewModelScope.launch {
            try {
                flashcardRepository?.saveFullRun(
                    deckId,
                    state.masteredCards.size,
                    state.totalCardsCount
                )
            } catch (_: Exception) {
            }
        }
    }

    fun restartDeck() {
        _uiState.update { currentState ->
            // Restore the full original session so a focused review round
            // (practiceReviewCards) never permanently drops mastered cards.
            val allCards = if (originalCards.isNotEmpty()) {
                originalCards
            } else {
                currentState.masteredCards + currentState.reviewCards + currentState.cards
            }
            currentState.copy(
                cards = allCards,
                totalCardsCount = allCards.size,
                currentCardIndex = 0,
                masteredCards = emptyList(),
                reviewCards = emptyList(),
                isCardFlipped = false,
                isFinished = allCards.isEmpty()
            )
        }
    }

    fun practiceReviewCards() {
        _uiState.update { currentState ->
            val reviewOnly = currentState.reviewCards
            currentState.copy(
                cards = reviewOnly,
                totalCardsCount = reviewOnly.size,
                currentCardIndex = 0,
                masteredCards = emptyList(),
                reviewCards = emptyList(),
                isCardFlipped = false,
                isFinished = reviewOnly.isEmpty()
            )
        }
    }

    // NOTE: no "undo" affordance exists in the practice screen, and SwipeableCard
    // owns its own swipe animation; kept as the programmatic entry point for
    // treating the visible card as mastered.
    fun swipeCard() {
        markAsMastered()
    }
}

data class FlashcardUiState(
    val deckTitle: String = "Flashcard Deck",
    val cards: List<Flashcard> = emptyList(),
    val totalCardsCount: Int = 0,
    val currentCardIndex: Int = 0,
    val masteredCards: List<Flashcard> = emptyList(),
    val reviewCards: List<Flashcard> = emptyList(),
    val isCardFlipped: Boolean = false,
    val isFinished: Boolean = false,
    val canManageCards: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null
)
