package com.kotonosora.todolist.feature.flashcard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kotonosora.todolist.ui.theme.AppTheme

@Composable
fun FlashcardScreen(
    noteId: String,
    onNavigateBack: () -> Unit,
    viewModel: FlashcardViewModel,
    onManageCards: (String) -> Unit = {},
    isDemo: Boolean = false
) {
    val uiState by viewModel.uiState.collectAsState()
    val speakWord = rememberFlashcardSpeaker()

    FlashcardScreenContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onFlipCard = { viewModel.flipCard() },
        onSwipeLeft = { viewModel.markForReview() },
        onSwipeRight = { viewModel.markAsMastered() },
        onRestartDeck = { viewModel.restartDeck() },
        onPracticeReview = { viewModel.practiceReviewCards() },
        onSpeakWord = speakWord,
        onManageCards = { onManageCards(noteId) },
        onRetryLoad = { viewModel.loadNote(noteId, isDemo = isDemo) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardScreenContent(
    uiState: FlashcardUiState,
    onNavigateBack: () -> Unit,
    onFlipCard: () -> Unit = {},
    onSwipeLeft: () -> Unit = {},
    onSwipeRight: () -> Unit = {},
    onRestartDeck: () -> Unit = {},
    onPracticeReview: () -> Unit = {},
    onSpeakWord: (String) -> Unit = {},
    onManageCards: () -> Unit = {},
    onRetryLoad: () -> Unit = {}
) {
    val total = uiState.totalCardsCount
    val masteredFraction = if (total > 0) {
        (uiState.masteredCards.size.toFloat() / total).coerceIn(0f, 1f)
    } else 0f
    val reviewFraction = if (total > 0) {
        (uiState.reviewCards.size.toFloat() / total).coerceIn(0f, 1f)
    } else 0f

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = uiState.deckTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (!uiState.isFinished && uiState.totalCardsCount > 0) {
                                Text(
                                    text = "Card ${uiState.currentCardIndex + 1} of ${uiState.totalCardsCount} • ${uiState.masteredCards.size} mastered",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Navigate Back"
                            )
                        }
                    },
                    actions = {
                        if (uiState.canManageCards) {
                            IconButton(onClick = onManageCards) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.List,
                                    contentDescription = "Manage words"
                                )
                            }
                        }
                    }
                )
                if (!uiState.isFinished && uiState.totalCardsCount > 0) {
                    MasteryProgressBar(
                        masteredFraction = masteredFraction,
                        reviewFraction = reviewFraction
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            if (uiState.error != null) {
                FlashcardLoadErrorContent(
                    message = uiState.error,
                    onRetry = onRetryLoad,
                    onBack = onNavigateBack
                )
            } else if (uiState.isLoading) {
                CircularProgressIndicator()
            } else if (uiState.isFinished) {
                if (uiState.totalCardsCount == 0 && uiState.canManageCards) {
                    EmptyDeckContent(
                        onAddWords = onManageCards,
                        onBack = onNavigateBack
                    )
                } else {
                    FlashcardSummaryContent(
                        totalCount = uiState.totalCardsCount,
                        masteredCount = uiState.masteredCards.size,
                        reviewCount = uiState.reviewCards.size,
                        onRestartDeck = onRestartDeck,
                        onPracticeReview = onPracticeReview,
                        onFinish = onNavigateBack,
                        reviewWords = uiState.reviewCards,
                        completedRuns = uiState.completedRuns,
                        sessionMasteredCount = uiState.sessionMasteredCount
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        val cards = uiState.cards

                        if (cards.size > 1) {
                            val nextCard = cards[1]
                            SwipeableCard(
                                flashcard = nextCard,
                                isFlipped = false,
                                onFlip = {},
                                onSwipeLeft = {},
                                onSwipeRight = {},
                                modifier = Modifier
                                    .padding(top = 16.dp)
                                    .fillMaxSize()
                            )
                        }

                        if (cards.isNotEmpty()) {
                            val currentCard = cards[0]
                            key(currentCard.id) {
                                SwipeableCard(
                                    flashcard = currentCard,
                                    isFlipped = uiState.isCardFlipped,
                                    onFlip = onFlipCard,
                                    onSwipeLeft = onSwipeLeft,
                                    onSwipeRight = onSwipeRight,
                                    modifier = Modifier.fillMaxSize(),
                                    onSpeak = { onSpeakWord(currentCard.word) }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onSwipeLeft,
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color(0xFFFFEBEE), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Needs Practice",
                                tint = Color(0xFFC62828),
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        OutlinedButton(
                            onClick = onFlipCard,
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Flip,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(text = if (uiState.isCardFlipped) "Show Word" else "Reveal Meaning")
                        }

                        IconButton(
                            onClick = onSwipeRight,
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color(0xFFE8F5E9), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Mastered",
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private val MasteredGreen = Color(0xFF2E7D32)
private val ReviewRed = Color(0xFFC62828)
private val MasteredGreenBg = Color(0xFFE8F5E9)
private val ReviewRedBg = Color(0xFFFFEBEE)

@Composable
private fun MasteryProgressBar(
    masteredFraction: Float,
    reviewFraction: Float,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (masteredFraction > 0f) {
            Box(
                modifier = Modifier
                    .weight(masteredFraction)
                    .fillMaxHeight()
                    .background(MasteredGreen)
            )
        }
        if (reviewFraction > 0f) {
            Box(
                modifier = Modifier
                    .weight(reviewFraction)
                    .fillMaxHeight()
                    .background(ReviewRed)
            )
        }
    }
}

@Composable
private fun FlashcardLoadErrorContent(
    message: String,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Couldn't load this deck",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Retry")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Back to decks")
        }
    }
}

@Composable
fun EmptyDeckContent(
    onAddWords: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "This deck has no words yet",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Add words to start practicing.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onAddWords,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Add words")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Back")
        }
    }
}

@Composable
fun FlashcardSummaryContent(
    totalCount: Int,
    masteredCount: Int,
    reviewCount: Int,
    onRestartDeck: () -> Unit,
    onPracticeReview: () -> Unit,
    onFinish: () -> Unit,
    reviewWords: List<Flashcard> = emptyList(),
    completedRuns: Int = 0,
    sessionMasteredCount: Int = 0
) {
    val fraction = if (totalCount > 0) masteredCount.toFloat() / totalCount else 0f
    val message = when {
        fraction >= 1f -> "Flawless — every word mastered!"
        fraction >= 0.7f -> "Great job reviewing your vocabulary words."
        fraction >= 0.4f -> "Good progress — review the marked words and try again."
        else -> "Keep practicing — mastery comes with repetition."
    }
    // Session totals span full + review rounds; the round total can be
    // smaller than the session total after a focused review run.
    val sessionTotal = maxOf(totalCount, sessionMasteredCount)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(148.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 14.dp.toPx()
                drawArc(
                    color = MasteredGreenBg,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = stroke)
                )
                if (fraction > 0f) {
                    drawArc(
                        color = MasteredGreen,
                        startAngle = -90f,
                        sweepAngle = 360f * fraction,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = MasteredGreen,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "${(fraction * 100).toInt()}%",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "mastered",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Deck Completed!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (completedRuns > 1 && sessionTotal > 0) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Session: $sessionMasteredCount of $sessionTotal mastered across $completedRuns runs",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = totalCount.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Total Words",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MasteredGreenBg)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = masteredCount.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MasteredGreen
                    )
                    Text(
                        text = "Mastered",
                        style = MaterialTheme.typography.labelSmall,
                        color = MasteredGreen
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = ReviewRedBg)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = reviewCount.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = ReviewRed
                    )
                    Text(
                        text = "Review",
                        style = MaterialTheme.typography.labelSmall,
                        color = ReviewRed
                    )
                }
            }
        }

        if (reviewWords.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Text(
                text = "Words to review",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            reviewWords.forEach { card ->
                Surface(
                    color = ReviewRedBg,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = card.word,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (card.phonetic.isNotBlank()) {
                                Text(
                                    text = card.phonetic,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontStyle = FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }

        Spacer(Modifier.height(24.dp))

        if (reviewCount > 0) {
            Button(
                onClick = onPracticeReview,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Practice $reviewCount Unmastered Words")
            }
            Spacer(Modifier.height(12.dp))
        }

        FilledTonalButton(
            onClick = onRestartDeck,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Restart Full Deck")
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onFinish,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Done")
        }
    }
}

// ── FULL CASE-BY-CASE PREVIEWS ──

@Preview(showBackground = true, name = "1. Active Card Session (Dark)")
@Composable
fun FlashcardScreenPreview_Active_Dark() {
    val sampleCards = listOf(
        Flashcard(
            word = "Serendipity",
            phonetic = "/ˌser.ənˈdɪp.ə.ti/",
            definition = "Finding valuable or agreeable things by chance.",
            example = "Meeting an old friend was a stroke of serendipity."
        ),
        Flashcard(
            word = "Ephemeral",
            phonetic = "/ɪˈfem.ər.əl/",
            definition = "Lasting for a very short time."
        )
    )
    AppTheme(darkTheme = true) {
        FlashcardScreenContent(
            uiState = FlashcardUiState(
                deckTitle = "Basic Vocabulary",
                cards = sampleCards,
                totalCardsCount = 5,
                currentCardIndex = 2,
                isLoading = false
            ),
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true, name = "2. Completed Summary (Light)")
@Composable
fun FlashcardScreenPreview_Summary_Light() {
    AppTheme(darkTheme = false) {
        FlashcardScreenContent(
            uiState = FlashcardUiState(
                deckTitle = "Tech Terminology",
                totalCardsCount = 10,
                masteredCards = List(8) { Flashcard(word = "Word $it") },
                reviewCards = List(2) { Flashcard(word = "Review $it") },
                isFinished = true,
                isLoading = false
            ),
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true, name = "3. Active Card Session (Light)")
@Composable
fun FlashcardScreenPreview_Active_Light() {
    val sampleCards = listOf(
        Flashcard(
            word = "Serendipity",
            phonetic = "/ˌser.ənˈdɪp.ə.ti/",
            definition = "Finding valuable or agreeable things by chance.",
            example = "Meeting an old friend was a stroke of serendipity."
        ),
        Flashcard(
            word = "Ephemeral",
            phonetic = "/ɪˈfem.ər.əl/",
            definition = "Lasting for a very short time."
        )
    )
    AppTheme(darkTheme = false) {
        FlashcardScreenContent(
            uiState = FlashcardUiState(
                deckTitle = "Basic Vocabulary",
                cards = sampleCards,
                totalCardsCount = 5,
                currentCardIndex = 2,
                isLoading = false
            ),
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true, name = "4. Completed Summary (Dark)")
@Composable
fun FlashcardScreenPreview_Summary_Dark() {
    AppTheme(darkTheme = true) {
        FlashcardScreenContent(
            uiState = FlashcardUiState(
                deckTitle = "Tech Terminology",
                totalCardsCount = 10,
                masteredCards = List(8) { Flashcard(word = "Word $it") },
                reviewCards = List(2) { Flashcard(word = "Review $it") },
                isFinished = true,
                isLoading = false
            ),
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true, name = "5. Loading (Dark)")
@Composable
fun FlashcardScreenPreview_Loading_Dark() {
    AppTheme(darkTheme = true) {
        FlashcardScreenContent(
            uiState = FlashcardUiState(
                deckTitle = "Basic Vocabulary",
                isLoading = true
            ),
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true, name = "6. Loading (Light)")
@Composable
fun FlashcardScreenPreview_Loading_Light() {
    AppTheme(darkTheme = false) {
        FlashcardScreenContent(
            uiState = FlashcardUiState(
                deckTitle = "Basic Vocabulary",
                isLoading = true
            ),
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true, name = "7. Load Error (Dark)")
@Composable
fun FlashcardScreenPreview_Error_Dark() {
    AppTheme(darkTheme = true) {
        FlashcardScreenContent(
            uiState = FlashcardUiState(
                deckTitle = "Basic Vocabulary",
                isLoading = false,
                error = "Failed to load deck"
            ),
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true, name = "8. Load Error (Light)")
@Composable
fun FlashcardScreenPreview_Error_Light() {
    AppTheme(darkTheme = false) {
        FlashcardScreenContent(
            uiState = FlashcardUiState(
                deckTitle = "Basic Vocabulary",
                isLoading = false,
                error = "Failed to load deck"
            ),
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true, name = "9. Empty Deck (Dark)")
@Composable
fun FlashcardScreenPreview_Empty_Dark() {
    AppTheme(darkTheme = true) {
        FlashcardScreenContent(
            uiState = FlashcardUiState(
                deckTitle = "New Deck",
                totalCardsCount = 0,
                isLoading = false
            ),
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true, name = "10. Empty Deck (Light)")
@Composable
fun FlashcardScreenPreview_Empty_Light() {
    AppTheme(darkTheme = false) {
        FlashcardScreenContent(
            uiState = FlashcardUiState(
                deckTitle = "New Deck",
                totalCardsCount = 0,
                isLoading = false
            ),
            onNavigateBack = {}
        )
    }
}
