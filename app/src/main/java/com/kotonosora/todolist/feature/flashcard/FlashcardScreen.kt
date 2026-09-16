package com.kotonosora.todolist.feature.flashcard

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kotonosora.todolist.ui.theme.TodoListTheme

@Composable
fun FlashcardScreen(
    noteId: String,
    onNavigateBack: () -> Unit,
    viewModel: FlashcardViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    FlashcardScreenContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onFlipCard = { viewModel.flipCard() },
        onSwipeLeft = { viewModel.markForReview() },
        onSwipeRight = { viewModel.markAsMastered() },
        onRestartDeck = { viewModel.restartDeck() },
        onPracticeReview = { viewModel.practiceReviewCards() }
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
    onPracticeReview: () -> Unit = {}
) {
    val progress = if (uiState.totalCardsCount > 0) {
        (uiState.currentCardIndex.toFloat() / uiState.totalCardsCount).coerceIn(0f, 1f)
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
                                    text = "Card ${uiState.currentCardIndex + 1} of ${uiState.totalCardsCount}",
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
                    }
                )
                if (!uiState.isFinished && uiState.totalCardsCount > 0) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
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
            if (uiState.isFinished) {
                // Summary Screen
                FlashcardSummaryContent(
                    totalCount = uiState.totalCardsCount,
                    masteredCount = uiState.masteredCards.size,
                    reviewCount = uiState.reviewCards.size,
                    onRestartDeck = onRestartDeck,
                    onPracticeReview = onPracticeReview,
                    onFinish = onNavigateBack
                )
            } else {
                // Active Card Session
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

                        // Bottom background card preview
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

                        // Top active card
                        if (cards.isNotEmpty()) {
                            val currentCard = cards[0]
                            SwipeableCard(
                                flashcard = currentCard,
                                isFlipped = uiState.isCardFlipped,
                                onFlip = onFlipCard,
                                onSwipeLeft = onSwipeLeft,
                                onSwipeRight = onSwipeRight,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    // Interactive Action Controls
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Practice / Needs Review button (Left)
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

                        // Flip Card button (Center)
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

                        // Mastered button (Right)
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

@Composable
fun FlashcardSummaryContent(
    totalCount: Int,
    masteredCount: Int,
    reviewCount: Int,
    onRestartDeck: () -> Unit,
    onPracticeReview: () -> Unit,
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = CircleShape,
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = "Deck Completed!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "Great job reviewing your vocabulary words.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(28.dp))

        // Stats Row
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
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = masteredCount.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                    Text(
                        text = "Mastered",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF2E7D32)
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = reviewCount.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC62828)
                    )
                    Text(
                        text = "Review",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFC62828)
                    )
                }
            }
        }

        Spacer(Modifier.height(36.dp))

        if (reviewCount > 0) {
            Button(
                onClick = onPracticeReview,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Practice $reviewCount Unmastered Words")
            }
            Spacer(Modifier.height(12.dp))
        }

        FilledTonalButton(
            onClick = onRestartDeck,
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Restart Full Deck")
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth().height(50.dp)
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
    TodoListTheme(darkTheme = true) {
        FlashcardScreenContent(
            uiState = FlashcardUiState(
                deckTitle = "Basic Vocabulary",
                cards = sampleCards,
                totalCardsCount = 5,
                currentCardIndex = 2
            ),
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true, name = "2. Completed Summary (Light)")
@Composable
fun FlashcardScreenPreview_Summary_Light() {
    TodoListTheme(darkTheme = false) {
        FlashcardScreenContent(
            uiState = FlashcardUiState(
                deckTitle = "Tech Terminology",
                totalCardsCount = 10,
                masteredCards = List(8) { Flashcard(word = "Word $it") },
                reviewCards = List(2) { Flashcard(word = "Review $it") },
                isFinished = true
            ),
            onNavigateBack = {}
        )
    }
}
