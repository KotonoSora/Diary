package com.kotonosora.todolist.feature.flashcard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kotonosora.todolist.ui.components.ConfirmActionSheet
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
        DeckRenameBottomSheet(
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
        CardEditBottomSheet(
            title = "Add word",
            onDismiss = { showAddDialog = false },
            onConfirm = { w, d, p, e ->
                showAddDialog = false
                onAddCard(w, d, p, e)
            }
        )
    }
    cardToEdit?.let { card ->
        CardEditBottomSheet(
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
        ConfirmActionSheet(
            title = "Delete word?",
            message = "\"${card.word}\" will be removed from this deck.",
            confirmLabel = "Delete",
            onDismiss = { cardToDelete = null },
            onConfirm = {
                cardToDelete = null
                onDeleteCard(card.id)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeckRenameBottomSheet(
    initialName: String,
    initialDescription: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        DeckRenameSheetContent(
            initialName = initialName,
            initialDescription = initialDescription,
            onDismiss = onDismiss,
            onConfirm = onConfirm
        )
    }
}

@Composable
private fun DeckRenameSheetContent(
    initialName: String,
    initialDescription: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by rememberSaveable(initialName) { mutableStateOf(initialName) }
    var description by rememberSaveable(initialDescription) { mutableStateOf(initialDescription) }
    val focusManager = LocalFocusManager.current
    val canSave = name.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Rename deck",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Update the deck name and description",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close"
                )
            }
        }
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name *") },
            placeholder = { Text("e.g. Japanese Vocabulary") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Style,
                    contentDescription = null
                )
            },
            supportingText = if (name.isBlank()) {
                { Text("Required — shown in the deck list") }
            } else null,
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            placeholder = { Text("e.g. Words I am learning this month") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null
                )
            },
            shape = RoundedCornerShape(16.dp),
            minLines = 2,
            maxLines = 4,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            ) { Text("Cancel") }
            Button(
                onClick = { onConfirm(name, description) },
                enabled = canSave,
                modifier = Modifier.weight(1f)
            ) { Text("Save") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CardEditBottomSheet(
    title: String,
    initial: Flashcard? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        CardEditSheetContent(
            title = title,
            initial = initial,
            onDismiss = onDismiss,
            onConfirm = onConfirm
        )
    }
}

@Composable
private fun CardEditSheetContent(
    title: String,
    initial: Flashcard? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String) -> Unit
) {
    var word by rememberSaveable(initial) { mutableStateOf(initial?.word.orEmpty()) }
    var definition by rememberSaveable(initial) { mutableStateOf(initial?.definition.orEmpty()) }
    var phonetic by rememberSaveable(initial) { mutableStateOf(initial?.phonetic.orEmpty()) }
    var example by rememberSaveable(initial) { mutableStateOf(initial?.example.orEmpty()) }
    val focusManager = LocalFocusManager.current
    val isEdit = initial != null
    val canSave = word.isNotBlank()
    val showPreview = word.isNotBlank() ||
            definition.isNotBlank() ||
            phonetic.isNotBlank() ||
            example.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = if (isEdit) Icons.Default.Edit else Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isEdit) "Update both sides of the card" else "Front shows the word, back shows the meaning",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close"
                )
            }
        }

        CardEditSectionLabel(text = "Front of card")
        OutlinedTextField(
            value = word,
            onValueChange = { word = it },
            label = { Text("Word *") },
            placeholder = { Text("e.g. Serendipity") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Translate,
                    contentDescription = null
                )
            },
            supportingText = if (word.isBlank()) {
                { Text("Required — shown on the front of the card") }
            } else null,
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = phonetic,
            onValueChange = { phonetic = it },
            label = { Text("Phonetic") },
            placeholder = { Text("/ˌser.ənˈdɪp.ə.ti/") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.RecordVoiceOver,
                    contentDescription = null
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            modifier = Modifier.fillMaxWidth()
        )

        HorizontalDivider()

        CardEditSectionLabel(text = "Back of card — meaning")
        OutlinedTextField(
            value = definition,
            onValueChange = { definition = it },
            label = { Text("Definition") },
            placeholder = { Text("e.g. Finding something good by chance") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null
                )
            },
            shape = RoundedCornerShape(16.dp),
            minLines = 2,
            maxLines = 4,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = example,
            onValueChange = { example = it },
            label = { Text("Example sentence") },
            placeholder = { Text("e.g. Finding the key was pure serendipity.") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.FormatQuote,
                    contentDescription = null
                )
            },
            shape = RoundedCornerShape(16.dp),
            minLines = 2,
            maxLines = 4,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            ),
            modifier = Modifier.fillMaxWidth()
        )

        if (showPreview) {
            HorizontalDivider()
            CardEditSectionLabel(text = "Preview")
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = word.ifBlank { "Your word" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (word.isBlank()) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    if (phonetic.isNotBlank()) {
                        Text(
                            text = phonetic,
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (definition.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = definition,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (example.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "\"$example\"",
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            ) { Text("Cancel") }
            Button(
                onClick = { onConfirm(word, definition, phonetic, example) },
                enabled = canSave,
                modifier = Modifier.weight(1f)
            ) { Text(if (isEdit) "Save" else "Add") }
        }
    }
}

@Composable
private fun CardEditSectionLabel(text: String) {
    Text(
        text = text.uppercase(java.util.Locale.ROOT),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
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

@Preview(showBackground = true, name = "3. Card Sheet Add - Dark")
@Composable
fun FlashcardCardAddSheetPreview_Dark() {
    AppTheme(darkTheme = true) {
        Surface {
            CardEditSheetContent(
                title = "Add word",
                onDismiss = {},
                onConfirm = { _, _, _, _ -> }
            )
        }
    }
}

@Preview(showBackground = true, name = "4. Card Sheet Edit - Light")
@Composable
fun FlashcardCardEditSheetPreview_Light() {
    AppTheme(darkTheme = false) {
        Surface {
            CardEditSheetContent(
                title = "Edit word",
                initial = Flashcard(
                    word = "Serendipity",
                    definition = "Finding something good by chance",
                    phonetic = "/ˌser.ənˈdɪp.ə.ti/",
                    example = "Finding the key was pure serendipity."
                ),
                onDismiss = {},
                onConfirm = { _, _, _, _ -> }
            )
        }
    }
}

@Preview(showBackground = true, name = "5. Deck Rename Sheet - Dark")
@Composable
fun FlashcardDetailDeckRenameSheetPreview_Dark() {
    AppTheme(darkTheme = true) {
        Surface {
            DeckRenameSheetContent(
                initialName = "My Japanese Deck",
                initialDescription = "Words I am learning",
                onDismiss = {},
                onConfirm = { _, _ -> }
            )
        }
    }
}

@Preview(showBackground = true, name = "6. Deck Rename Sheet - Light")
@Composable
fun FlashcardDetailDeckRenameSheetPreview_Light() {
    AppTheme(darkTheme = false) {
        Surface {
            DeckRenameSheetContent(
                initialName = "My Japanese Deck",
                initialDescription = "Words I am learning",
                onDismiss = {},
                onConfirm = { _, _ -> }
            )
        }
    }
}
