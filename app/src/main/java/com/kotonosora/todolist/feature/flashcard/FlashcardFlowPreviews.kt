package com.kotonosora.todolist.feature.flashcard

import android.content.res.Configuration
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kotonosora.todolist.domain.model.NoteItem
import com.kotonosora.todolist.feature.editor.EditorContent
import com.kotonosora.todolist.feature.editor.EditorTabItem
import com.kotonosora.todolist.feature.editor.EditorUiState
import com.kotonosora.todolist.ui.theme.TodoListTheme

/**
 * Interactive Flow Preview for Case 1: Flow starting from Home Page
 */
@Composable
fun FlashcardFlowFromHomeContent() {
    var currentStep by remember { mutableIntStateOf(0) }

    val sampleCards = listOf(
        Flashcard(
            word = "Serendipity",
            phonetic = "/ˌser.ənˈdɪp.ə.ti/",
            definition = "Finding valuable things by chance in a happy or beneficial way.",
            example = "Finding the lost key was pure serendipity."
        ),
        Flashcard(
            word = "Ephemeral",
            phonetic = "/ɪˈfem.ər.əl/",
            definition = "Lasting for a very short time; fleeting."
        )
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // Step Navigation Bar
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "FLOW CASE 1: START FROM HOME PAGE",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val steps = listOf("1. Home", "2. Deck Selection", "3. Flashcard Session", "4. Completed")
                    steps.forEachIndexed { index, title ->
                        Surface(
                            color = if (currentStep == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            contentColor = if (currentStep == index) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { currentStep = index }
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (currentStep) {
                0 -> {
                    // Step 1: Home Page / Deck List Entrance
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Home Page / Navigation Drawer",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Tap 'Flashcards' from Home Navigation Drawer or Action List to view decks.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(24.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { currentStep = 1 },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Style, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(12.dp))
                                Text("Open Flashcards Feature", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                            }
                        }
                    }
                }
                1 -> {
                    // Step 2: Deck Selection Screen
                    FlashcardDeckSelectionScreen(
                        onOpenDrawer = {},
                        onDeckSelected = { currentStep = 2 }
                    )
                }
                2 -> {
                    // Step 3: Active Flashcard Session
                    FlashcardScreenContent(
                        uiState = FlashcardUiState(
                            deckTitle = "Basic Vocabulary",
                            cards = sampleCards,
                            totalCardsCount = 5,
                            currentCardIndex = 0
                        ),
                        onNavigateBack = { currentStep = 1 },
                        onSwipeRight = { currentStep = 3 },
                        onSwipeLeft = { currentStep = 3 }
                    )
                }
                3 -> {
                    // Step 4: Summary Screen
                    FlashcardScreenContent(
                        uiState = FlashcardUiState(
                            deckTitle = "Basic Vocabulary",
                            totalCardsCount = 5,
                            masteredCards = sampleCards,
                            reviewCards = emptyList(),
                            isFinished = true
                        ),
                        onNavigateBack = { currentStep = 1 },
                        onRestartDeck = { currentStep = 2 }
                    )
                }
            }
        }
    }
}

/**
 * Interactive Flow Preview for Case 2: Flow starting from Editor file type support convert flashcard
 */
@Composable
fun FlashcardFlowFromEditorContent() {
    var currentStep by remember { mutableIntStateOf(0) }

    val editorNote = NoteItem(
        id = "Japanese_Vocabulary.md",
        title = "Japanese Vocabulary",
        relativePath = "Language",
        content = """
            # Japanese Vocabulary List
            
            - Gakkou: School (e.g. They walk to gakkou every morning)
            - Gakusei: Student
            - Sensei: Teacher
            - Hon: Book
            - Toshokan: Library
        """.trimIndent()
    )

    val sampleCards = MarkdownParser.parseMarkdownFlashcards(editorNote.content)

    Column(modifier = Modifier.fillMaxSize()) {
        // Step Navigation Bar
        Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "FLOW CASE 2: CONVERT EDITOR FILE TO FLASHCARDS",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val steps = listOf("1. Open Editor File", "2. Flashcard Session", "3. Completed")
                    steps.forEachIndexed { index, title ->
                        Surface(
                            color = if (currentStep == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            contentColor = if (currentStep == index) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { currentStep = index }
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (currentStep) {
                0 -> {
                    // Step 1: Editor Screen with Convert Banner
                    EditorContent(
                        uiState = EditorUiState(
                            note = editorNote,
                            openTabs = listOf(EditorTabItem("Japanese_Vocabulary.md", "Japanese Vocabulary"))
                        ),
                        onBack = {},
                        onSave = {},
                        onLearnFlashcards = { currentStep = 1 }
                    )
                }
                1 -> {
                    // Step 2: Converted Flashcards Session Screen
                    FlashcardScreenContent(
                        uiState = FlashcardUiState(
                            deckTitle = "Japanese Vocabulary",
                            cards = sampleCards,
                            totalCardsCount = sampleCards.size,
                            currentCardIndex = 0
                        ),
                        onNavigateBack = { currentStep = 0 },
                        onSwipeRight = { currentStep = 2 },
                        onSwipeLeft = { currentStep = 2 }
                    )
                }
                2 -> {
                    // Step 3: Summary
                    FlashcardScreenContent(
                        uiState = FlashcardUiState(
                            deckTitle = "Japanese Vocabulary",
                            totalCardsCount = sampleCards.size,
                            masteredCards = sampleCards,
                            reviewCards = emptyList(),
                            isFinished = true
                        ),
                        onNavigateBack = { currentStep = 0 },
                        onRestartDeck = { currentStep = 1 }
                    )
                }
            }
        }
    }
}

// ── PREVIEW COMPOSE FUNCTIONS ──

@Preview(showBackground = true, name = "Flow 1: Start from Home Page - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun FlashcardHomeFlowPreview_Dark() {
    TodoListTheme(darkTheme = true) {
        FlashcardFlowFromHomeContent()
    }
}

@Preview(showBackground = true, name = "Flow 1: Start from Home Page - Light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun FlashcardHomeFlowPreview_Light() {
    TodoListTheme(darkTheme = false) {
        FlashcardFlowFromHomeContent()
    }
}

@Preview(showBackground = true, name = "Flow 2: Convert from Editor File - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun FlashcardEditorFlowPreview_Dark() {
    TodoListTheme(darkTheme = true) {
        FlashcardFlowFromEditorContent()
    }
}

@Preview(showBackground = true, name = "Flow 2: Convert from Editor File - Light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun FlashcardEditorFlowPreview_Light() {
    TodoListTheme(darkTheme = false) {
        FlashcardFlowFromEditorContent()
    }
}
