package com.kotonosora.todolist.feature.flashcard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotonosora.todolist.domain.repository.VaultRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FlashcardViewModel(
    private val vaultRepository: VaultRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FlashcardUiState())
    val uiState: StateFlow<FlashcardUiState> = _uiState.asStateFlow()

    fun loadNote(noteId: String, isDemo: Boolean = false) {
        viewModelScope.launch {
            if (isDemo) {
                val (deckTitle, demoCards) = generateDemoCards(noteId)
                _uiState.update {
                    it.copy(
                        deckTitle = deckTitle,
                        cards = demoCards,
                        totalCardsCount = demoCards.size,
                        currentCardIndex = 0,
                        masteredCards = emptyList(),
                        reviewCards = emptyList(),
                        isCardFlipped = false,
                        isFinished = demoCards.isEmpty()
                    )
                }
            } else {
                val note = vaultRepository.getNoteById(noteId)
                val markdownText = note?.content ?: ""
                val cards = MarkdownParser.parseMarkdownFlashcards(markdownText)
                val deckTitle = note?.title ?: "Note Flashcards"
                _uiState.update {
                    it.copy(
                        deckTitle = deckTitle,
                        cards = cards,
                        totalCardsCount = cards.size,
                        currentCardIndex = 0,
                        masteredCards = emptyList(),
                        reviewCards = emptyList(),
                        isCardFlipped = false,
                        isFinished = cards.isEmpty()
                    )
                }
            }
        }
    }

    private fun generateDemoCards(deckId: String): Pair<String, List<Flashcard>> {
        return when (deckId) {
            "demo_basic" -> "Basic Vocabulary" to listOf(
                Flashcard(word = "Student", definition = "A person who is studying at a school or college.", phonetic = "/ˈstjuː.dənt/", example = "She is a top student in her class."),
                Flashcard(word = "School", definition = "An institution for educating children or adults.", phonetic = "/skuːl/", example = "They walk to school every morning."),
                Flashcard(word = "Afternoon", definition = "The time from noon or lunchtime to evening.", phonetic = "/ˌɑːf.təˈnuːn/", example = "We had tea in the afternoon."),
                Flashcard(word = "Teacher", definition = "A person who teaches, especially in a school.", phonetic = "/ˈtiː.tʃər/", example = "The teacher explained the lesson clearly."),
                Flashcard(word = "Library", definition = "A building containing collections of books for reading or borrowing.", phonetic = "/ˈlaɪ.brər.i/", example = "Quiet studying is required in the library.")
            )
            "demo_advanced" -> "Advanced Vocabulary" to listOf(
                Flashcard(word = "Serendipity", definition = "The occurrence of events by chance in a happy or beneficial way.", phonetic = "/ˌser.ənˈdɪp.ə.ti/", example = "Finding the lost key was pure serendipity."),
                Flashcard(word = "Ephemeral", definition = "Lasting for a very short time; fleeting.", phonetic = "/ɪˈfem.ər.əl/", example = "Fame in the digital age can be ephemeral."),
                Flashcard(word = "Ubiquitous", definition = "Present, appearing, or found everywhere.", phonetic = "/juːˈbɪk.wɪ.təs/", example = "Smartphones have become ubiquitous in daily life."),
                Flashcard(word = "Mellifluous", definition = "Sweet or musical; pleasant to hear.", phonetic = "/məˈlɪf.lu.əs/", example = "Her mellifluous voice relaxed the audience."),
                Flashcard(word = "Ineffable", definition = "Too great or extreme to be expressed or described in words.", phonetic = "/ɪnˈef.ə.bəl/", example = "The beauty of the sunset was ineffable.")
            )
            "demo_tech" -> "Tech Terminology" to listOf(
                Flashcard(word = "Algorithm", definition = "A process or set of rules to be followed in calculations or problem-solving.", phonetic = "/ˈæl.ɡə.rɪ.ðəm/", example = "Sorting algorithms optimize search times."),
                Flashcard(word = "Database", definition = "An organized collection of structured information or data stored electronically.", phonetic = "/ˈdeɪ.tə.beɪs/", example = "Room SQLite database manages offline app state."),
                Flashcard(word = "Encryption", definition = "The process of converting information or data into code to prevent unauthorized access.", phonetic = "/ɪnˈkrɪp.ʃən/", example = "End-to-end encryption keeps messages private."),
                Flashcard(word = "Polymorphism", definition = "The condition of occurring in several different forms in OOP.", phonetic = "/ˌpɒl.iˈmɔː.fɪ.zəm/", example = "Method overriding demonstrates runtime polymorphism."),
                Flashcard(word = "Recursion", definition = "A method where the solution depends on solutions to smaller instances of the same problem.", phonetic = "/rɪˈkɜː.ʃən/", example = "Factorial logic is cleanly solved using recursion.")
            )
            else -> "Sample Flashcards" to listOf(
                Flashcard(word = "Apple", definition = "A round red or green fruit with firm white flesh.", phonetic = "/ˈæp.əl/"),
                Flashcard(word = "Banana", definition = "A long curved fruit with a yellow skin.", phonetic = "/bəˈnɑː.nə/"),
                Flashcard(word = "Cherry", definition = "A small, round, bright or dark red fruit.", phonetic = "/ˈtʃer.i/")
            )
        }
    }

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
    }

    fun restartDeck() {
        _uiState.update { currentState ->
            val allCards = currentState.masteredCards + currentState.reviewCards + currentState.cards
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
    val isFinished: Boolean = false
)
