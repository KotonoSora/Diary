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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kotonosora.todolist.ui.theme.AppTheme

@Composable
fun FlashcardDeckDetailScreen(
    viewModel: FlashcardDeckDetailViewModel,
    onNavigateBack: () -> Unit,
    onPractice: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    FlashcardDeckDetailContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onNavigateBack = onNavigateBack,
        onPractice = { onPractice(uiState.deckId) },
        onRenameDeck = { name, description -> viewModel.renameDeck(name, description) },
        onAddCard = { w, d, p, e -> viewModel.addCard(w, d, p, e) },
        onUpdateCard = { viewModel.updateCard(it) },
        onDeleteCard = { viewModel.deleteCard(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardDeckDetailContent(
    uiState: DeckDetailUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onNavigateBack: () -> Unit = {},
    onPractice: () -> Unit = {},
    onRenameDeck: (String, String) -> Unit = { _, _ -> },
    onAddCard: (String, String, String, String) -> Unit = { _, _, _, _ -> },
    onUpdateCard: (Flashcard) -> Unit = {},
    onDeleteCard: (String) -> Unit = {}
) {
    var showRenameDialog by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var cardToEdit by remember { mutableStateOf<Flashcard?>(null) }
    var cardToDelete by remember { mutableStateOf<Flashcard?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.name.ifBlank { "Deck" },
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (!uiState.isLoading && !uiState.notFound) {
                        IconButton(onClick = { showRenameDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Rename deck"
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!uiState.isLoading && !uiState.notFound) {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add word")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.notFound -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Deck not found. It may have been deleted.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        top = 8.dp,
                        bottom = 88.dp
                    )
                ) {
                    item {
                        DeckDetailHeader(
                            description = uiState.description,
                            cardCount = uiState.cards.size,
                            onPractice = onPractice
                        )
                    }
                    if (uiState.cards.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
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
                                        imageVector = Icons.Default.Style,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        text = "No words yet",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "Tap + to add your first word to this deck.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(uiState.cards, key = { it.id }) { card ->
                            CardRow(
                                card = card,
                                onEdit = { cardToEdit = card },
                                onDelete = { cardToDelete = card }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showRenameDialog) {
        DeckRenameDialog(
            initialName = uiState.name,
            initialDescription = uiState.description,
            onDismiss = { showRenameDialog = false },
            onConfirm = { name, description ->
                showRenameDialog = false
                onRenameDeck(name, description)
            }
        )
    }
    if (showAddDialog) {
        CardEditDialog(
            title = "Add word",
            onDismiss = { showAddDialog = false },
            onConfirm = { w, d, p, e ->
                showAddDialog = false
                onAddCard(w, d, p, e)
            }
        )
    }
    cardToEdit?.let { card ->
        CardEditDialog(
            title = "Edit word",
            initial = card,
            onDismiss = { cardToEdit = null },
            onConfirm = { w, d, p, e ->
                cardToEdit = null
                onUpdateCard(card.copy(word = w, definition = d, phonetic = p, example = e))
            }
        )
    }
    cardToDelete?.let { card ->
        AlertDialog(
            onDismissRequest = { cardToDelete = null },
            title = { Text("Delete word?") },
            text = { Text("\"${card.word}\" will be removed from this deck.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        cardToDelete = null
                        onDeleteCard(card.id)
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { cardToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun DeckDetailHeader(
    description: String,
    cardCount: Int,
    onPractice: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (description.isNotBlank()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.height(8.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$cardCount ${if (cardCount == 1) "word" else "words"}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f)
                )
                if (cardCount > 0) {
                    Button(onClick = onPractice) {
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
}

@Composable
private fun CardRow(
    card: Flashcard,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = card.word,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (card.phonetic.isNotBlank()) {
                    Text(
                        text = card.phonetic,
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                if (card.definition.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = card.definition,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3
                    )
                }
                if (card.example.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "\"${card.example}\"",
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }
            IconButton(onClick = onEdit) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit word")
            }
            IconButton(onClick = onDelete) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete word")
            }
        }
    }
}

@Composable
private fun DeckRenameDialog(
    initialName: String,
    initialDescription: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    var description by remember(initialDescription) { mutableStateOf(initialDescription) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename deck") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, description) },
                enabled = name.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun CardEditDialog(
    title: String,
    initial: Flashcard? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String) -> Unit
) {
    var word by remember(initial) { mutableStateOf(initial?.word.orEmpty()) }
    var definition by remember(initial) { mutableStateOf(initial?.definition.orEmpty()) }
    var phonetic by remember(initial) { mutableStateOf(initial?.phonetic.orEmpty()) }
    var example by remember(initial) { mutableStateOf(initial?.example.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = word,
                    onValueChange = { word = it },
                    label = { Text("Word *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = definition,
                    onValueChange = { definition = it },
                    label = { Text("Definition") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phonetic,
                    onValueChange = { phonetic = it },
                    label = { Text("Phonetic") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = example,
                    onValueChange = { example = it },
                    label = { Text("Example") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(word, definition, phonetic, example) },
                enabled = word.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Preview(showBackground = true, name = "1. Deck Detail - Dark")
@Composable
fun FlashcardDeckDetailPreview_Dark() {
    AppTheme(darkTheme = true) {
        FlashcardDeckDetailContent(
            uiState = DeckDetailUiState(
                deckId = "deck_1",
                name = "My Japanese Deck",
                description = "Words I am learning",
                isLoading = false,
                cards = listOf(
                    Flashcard(
                        word = "Gakkou",
                        definition = "School",
                        phonetic = "/ɡak.koː/",
                        example = "They walk to gakkou every morning."
                    ),
                    Flashcard(word = "Hon", definition = "Book")
                )
            )
        )
    }
}

@Preview(showBackground = true, name = "2. Deck Detail - Light")
@Composable
fun FlashcardDeckDetailPreview_Light() {
    AppTheme(darkTheme = false) {
        FlashcardDeckDetailContent(
            uiState = DeckDetailUiState(
                deckId = "deck_1",
                name = "New Deck",
                description = "",
                isLoading = false,
                cards = emptyList()
            )
        )
    }
}
