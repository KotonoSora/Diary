package com.kotonosora.todolist.feature.flashcard

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kotonosora.todolist.ui.theme.AppTheme

data class FlashcardDeck(
    val id: String,
    val name: String,
    val description: String,
    val cardCount: Int = 30
)

val demoDecks = listOf(
    FlashcardDeck(
        "demo_basic",
        "Basic Vocabulary",
        "Common everyday words like student, school, afternoon.",
        30
    ),
    FlashcardDeck(
        "demo_advanced",
        "Advanced Vocabulary",
        "Complex words to expand your vocabulary.",
        30
    ),
    FlashcardDeck(
        "demo_tech",
        "Tech Terminology",
        "Words used in software engineering and technology.",
        30
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardDeckSelectionScreen(
    onOpenDrawer: () -> Unit,
    onDeckSelected: (String) -> Unit,
    progress: Map<String, Int> = emptyMap(),
    decks: List<FlashcardDeck>? = null,
    isLoading: Boolean = false,
    crudEnabled: Boolean = false,
    error: String? = null,
    onCreateDeck: (String, String) -> Unit = { _, _ -> },
    onRenameDeck: (String, String, String) -> Unit = { _, _, _ -> },
    onDeleteDeck: (String) -> Unit = {},
    builtInDeckIds: Set<String> = emptySet(),
    onManageDeck: (String) -> Unit = {},
    onDismissError: () -> Unit = {}
) {
    var showCreateDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var deckToRename by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<FlashcardDeck?>(null) }
    var deckToDelete by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<FlashcardDeck?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Flashcard Decks", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Open Navigation Drawer"
                        )
                    }
                }
            )
        },
        snackbarHost = {
            val hostState = androidx.compose.runtime.remember { SnackbarHostState() }
            LaunchedEffect(error) {
                error?.let {
                    hostState.showSnackbar(it)
                    onDismissError()
                }
            }
            SnackbarHost(hostState)
        },
        floatingActionButton = {
            if (crudEnabled) {
                androidx.compose.material3.FloatingActionButton(onClick = { showCreateDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create deck"
                    )
                }
            }
        }
    ) { innerPadding ->
        FlashcardDeckSelectionContent(
            decks = decks ?: demoDecks,
            onDeckSelected = onDeckSelected,
            modifier = Modifier.padding(innerPadding),
            progress = progress,
            isLoading = isLoading,
            crudEnabled = crudEnabled,
            builtInDeckIds = builtInDeckIds,
            onCreateDeck = { showCreateDialog = true },
            onRenameDeck = { deckToRename = it },
            onDeleteDeck = { deckToDelete = it },
            onManageDeck = onManageDeck
        )
    }

    if (showCreateDialog) {
        DeckEditDialog(
            title = "New deck",
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, description ->
                showCreateDialog = false
                onCreateDeck(name, description)
            }
        )
    }
    deckToRename?.let { deck ->
        DeckEditDialog(
            title = "Rename deck",
            initialName = deck.name,
            initialDescription = deck.description,
            onDismiss = { deckToRename = null },
            onConfirm = { name, description ->
                deckToRename = null
                onRenameDeck(deck.id, name, description)
            }
        )
    }
    deckToDelete?.let { deck ->
        AlertDialog(
            onDismissRequest = { deckToDelete = null },
            title = { Text("Delete deck?") },
            text = { Text("\"${deck.name}\" and its ${deck.cardCount} cards will be removed.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        deckToDelete = null
                        onDeleteDeck(deck.id)
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deckToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun DeckEditDialog(
    title: String,
    initialName: String = "",
    initialDescription: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by androidx.compose.runtime.remember(initialName) {
        androidx.compose.runtime.mutableStateOf(initialName)
    }
    var description by androidx.compose.runtime.remember(initialDescription) {
        androidx.compose.runtime.mutableStateOf(initialDescription)
    }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                androidx.compose.material3.OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                androidx.compose.material3.OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = { onConfirm(name, description) },
                enabled = name.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun FlashcardDeckSelectionContent(
    decks: List<FlashcardDeck>,
    onDeckSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    progress: Map<String, Int> = emptyMap(),
    isLoading: Boolean = false,
    crudEnabled: Boolean = false,
    builtInDeckIds: Set<String> = emptySet(),
    onCreateDeck: () -> Unit = {},
    onRenameDeck: (FlashcardDeck) -> Unit = {},
    onDeleteDeck: (FlashcardDeck) -> Unit = {},
    onManageDeck: (String) -> Unit = {}
) {
    val totalWords = decks.sumOf { it.cardCount }
    if (isLoading && decks.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 8.dp, bottom = 24.dp)
    ) {
        item {
            DeckListHeader(
                deckCount = decks.size,
                totalWords = totalWords
            )
        }

        if (decks.isEmpty() && crudEnabled) {
            item {
                EmptyDeckListCard(onCreateDeck = onCreateDeck)
            }
        }

        items(decks, key = { it.id }) { deck ->
            DeckCard(
                deck = deck,
                mastered = (progress[deck.id] ?: 0).coerceIn(0, deck.cardCount.coerceAtLeast(0)),
                onStart = { onDeckSelected(deck.id) },
                crudEnabled = crudEnabled,
                isBuiltIn = deck.id in builtInDeckIds,
                onRename = { onRenameDeck(deck) },
                onDelete = { onDeleteDeck(deck) },
                onManage = { onManageDeck(deck.id) }
            )
        }
    }
}

@Composable
private fun EmptyDeckListCard(onCreateDeck: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "No decks yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Create your first deck and add words to start learning.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onCreateDeck) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Create deck")
            }
        }
    }
}

@Composable
private fun DeckListHeader(deckCount: Int, totalWords: Int) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(12.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Practice Built-in Decks",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "$deckCount decks • $totalWords words — pick one to start",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun DeckCard(
    deck: FlashcardDeck,
    mastered: Int,
    onStart: () -> Unit,
    crudEnabled: Boolean = false,
    isBuiltIn: Boolean = false,
    onRename: () -> Unit = {},
    onDelete: () -> Unit = {},
    onManage: () -> Unit = {}
) {
    val (icon, container, onContainer) = deckStyle(deck.id)
    val minutes = (deck.cardCount / 6).coerceAtLeast(1)
    val fraction = if (deck.cardCount > 0) mastered.toFloat() / deck.cardCount else 0f
    Card(
        onClick = onStart,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = container, shape = RoundedCornerShape(14.dp)) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = onContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = deck.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${deck.cardCount} words • ~$minutes min",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (crudEnabled) {
                    IconButton(onClick = onRename) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Rename deck"
                        )
                    }
                    if (!isBuiltIn) {
                        IconButton(onClick = onDelete) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete deck"
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = deck.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$mastered of ${deck.cardCount} mastered",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (mastered >= deck.cardCount && deck.cardCount > 0) {
                    Text(
                        text = "Completed",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (crudEnabled) {
                    OutlinedButton(onClick = onManage) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Words")
                    }
                }
                FilledTonalButton(onClick = onStart) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Practice")
                }
            }
        }
    }
}

@Composable
private fun deckStyle(deckId: String): Triple<ImageVector, androidx.compose.ui.graphics.Color, androidx.compose.ui.graphics.Color> {
    return when (deckId) {
        "demo_basic" -> Triple(
            Icons.Default.School,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )

        "demo_advanced" -> Triple(
            Icons.Default.Psychology,
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )

        "demo_tech" -> Triple(
            Icons.Default.Code,
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )

        else -> Triple(
            Icons.Default.School,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

// ── FULL CASE-BY-CASE PREVIEWS ──

@Preview(showBackground = true, name = "1. Deck Selection - Dark")
@Composable
fun FlashcardDeckSelectionPreview_Dark() {
    AppTheme(darkTheme = true) {
        FlashcardDeckSelectionContent(
            decks = demoDecks,
            onDeckSelected = {},
            progress = mapOf("demo_basic" to 30, "demo_advanced" to 12)
        )
    }
}

@Preview(showBackground = true, name = "2. Deck Selection - Light")
@Composable
fun FlashcardDeckSelectionPreview_Light() {
    AppTheme(darkTheme = false) {
        FlashcardDeckSelectionContent(
            decks = demoDecks,
            onDeckSelected = {},
            progress = mapOf("demo_basic" to 30, "demo_advanced" to 12)
        )
    }
}
