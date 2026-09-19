package com.kotonosora.todolist.feature.editor

import android.content.Intent
import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kotonosora.todolist.data.native.MdNativeHelper
import com.kotonosora.todolist.domain.model.NoteItem
import com.kotonosora.todolist.domain.model.NoteType
import com.kotonosora.todolist.domain.model.description
import com.kotonosora.todolist.domain.model.displayName
import com.kotonosora.todolist.feature.flashcard.MarkdownParser
import com.kotonosora.todolist.feature.vault.formatCleanDisplayName
import com.kotonosora.todolist.ui.theme.TodoListTheme

@Composable
fun EditorScreen(
    noteId: String,
    viewModel: EditorViewModel,
    onBack: () -> Unit,
    onNavigateToNote: (String) -> Unit,
    onLearnFlashcards: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(noteId) {
        viewModel.loadNote(noteId)
    }

    EditorContent(
        uiState = uiState,
        onBack = onBack,
        onSave = { viewModel.saveNote() },
        onToggleFocusMode = { viewModel.toggleFocusMode() },
        onTabSelect = { id -> viewModel.loadNote(id) },
        onTabClose = { id -> viewModel.closeTab(id) },
        onRenameNote = { newTitle, onRenamed -> viewModel.renameNote(newTitle, onRenamed) },
        onContentChange = { viewModel.onContentChange(it) },
        onWikiLinkClick = { targetTitle -> onNavigateToNote("$targetTitle.md") },
        onSuggestionSelected = { viewModel.onSuggestionSelected(it) },
        onLearnFlashcards = onLearnFlashcards
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorContent(
    uiState: EditorUiState,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onToggleFocusMode: () -> Unit = {},
    onTabSelect: (String) -> Unit = {},
    onTabClose: (String) -> Unit = {},
    onRenameNote: (String, (String) -> Unit) -> Unit = { _, _ -> },
    onContentChange: (String) -> Unit = {},
    onWikiLinkClick: (String) -> Unit = {},
    onSuggestionSelected: (String) -> Unit = {},
    onLearnFlashcards: () -> Unit = {}
) {
    val context = LocalContext.current
    var showRenameSheet by remember { mutableStateOf(false) }
    var showTemplateSheet by remember { mutableStateOf(false) }
    var showInfoSheet by remember { mutableStateOf(false) }
    var showBacklinksSheet by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf(EditorViewMode.EDITING) }
    var renameInput by remember { mutableStateOf("") }

    val flashcardCount = remember(uiState.note.content) {
        MarkdownParser.parseMarkdownFlashcards(uiState.note.content).size
    }

    var noteTitleInput by remember(uiState.note.title) {
        mutableStateOf(formatCleanDisplayName(uiState.note.title))
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AnimatedVisibility(visible = !uiState.isFocusMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }

                    // Direct Editable File Title in Header Bar
                    OutlinedTextField(
                        value = noteTitleInput,
                        onValueChange = { newTitle ->
                            noteTitleInput = newTitle
                            if (newTitle.isNotBlank()) {
                                onRenameNote(newTitle) {}
                            }
                        },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                text = "Untitled Note",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        textStyle = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        singleLine = true
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // View Mode Toggle (Reading / Editing)
                        IconButton(
                            onClick = {
                                viewMode =
                                    if (viewMode == EditorViewMode.EDITING) EditorViewMode.READING else EditorViewMode.EDITING
                            }
                        ) {
                            Icon(
                                imageVector = if (viewMode == EditorViewMode.EDITING) Icons.AutoMirrored.Filled.MenuBook else Icons.Default.Edit,
                                contentDescription = if (viewMode == EditorViewMode.EDITING) "Switch to Reading View" else "Switch to Live Edit",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(onClick = onSave) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = "Save Note",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Box {
                            IconButton(onClick = { showOptionsMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Note Options"
                                )
                            }

                            DropdownMenu(
                                expanded = showOptionsMenu,
                                onDismissRequest = { showOptionsMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Note Info & Stats") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Info,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        showOptionsMenu = false
                                        showInfoSheet = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Linked Notes (${uiState.note.links.size})") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Link,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        showOptionsMenu = false
                                        showBacklinksSheet = true
                                    }
                                )
                                if (flashcardCount > 0) {
                                    DropdownMenuItem(
                                        text = { Text("Learn Flashcards ($flashcardCount)") },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Style,
                                                contentDescription = null
                                            )
                                        },
                                        onClick = {
                                            showOptionsMenu = false
                                            onLearnFlashcards()
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Insert Template") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.AutoAwesome,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        showOptionsMenu = false
                                        showTemplateSheet = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Rename Note") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        showOptionsMenu = false
                                        renameInput = uiState.note.title
                                        showRenameSheet = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Share Note") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Share,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        showOptionsMenu = false
                                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                            putExtra(Intent.EXTRA_TITLE, uiState.note.title)
                                            putExtra(Intent.EXTRA_TEXT, uiState.note.content)
                                            type = "text/plain"
                                        }
                                        context.startActivity(
                                            Intent.createChooser(
                                                sendIntent,
                                                "Share Note"
                                            )
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (uiState.isFocusMode) "Exit Focus Mode" else "Focus Mode") },
                                    leadingIcon = {
                                        Icon(
                                            if (uiState.isFocusMode) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        showOptionsMenu = false
                                        onToggleFocusMode()
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                LivePreviewEditor(
                    content = uiState.note.content,
                    onContentChange = onContentChange,
                    onWikiLinkClick = onWikiLinkClick,
                    viewMode = viewMode,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp)
                )

                if (uiState.showSuggestions) {
                    WikiLinkAutoCompleteOverlay(
                        suggestions = uiState.suggestions,
                        onSuggestionSelected = onSuggestionSelected,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                            .fillMaxWidth(0.9f)
                    )
                }
            }

            if (showTemplateSheet) {
                val sheetState = rememberModalBottomSheetState()
                ModalBottomSheet(
                    onDismissRequest = { showTemplateSheet = false },
                    sheetState = sheetState
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Text(
                            text = "Apply File Template",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Select a template to insert structured content into your note.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(NoteType.entries.toTypedArray()) { type ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val templateContent =
                                                ZettelTemplatePicker.generateContentForTemplate(
                                                    noteType = type,
                                                    title = uiState.note.title
                                                )
                                            onContentChange(templateContent)
                                            showTemplateSheet = false
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                            alpha = 0.5f
                                        )
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = type.displayName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = type.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = { showTemplateSheet = false },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }

            if (showRenameSheet) {
                val sheetState = rememberModalBottomSheetState()
                ModalBottomSheet(
                    onDismissRequest = { showRenameSheet = false },
                    sheetState = sheetState
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Text(
                            text = "Rename Note",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = renameInput,
                            onValueChange = { renameInput = it },
                            label = { Text("New Note Title") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(onClick = { showRenameSheet = false }) {
                                Text("Cancel")
                            }
                            Spacer(
                                Modifier
                                    .height(0.dp)
                                    .padding(horizontal = 6.dp)
                            )
                            Button(onClick = {
                                if (renameInput.isNotBlank()) {
                                    onRenameNote(renameInput) {
                                        showRenameSheet = false
                                    }
                                }
                            }) {
                                Text("Rename")
                            }
                        }
                    }
                }
            }

            if (showInfoSheet) {
                val sheetState = rememberModalBottomSheetState()
                val stats = remember(uiState.note.content) {
                    try {
                        MdNativeHelper.calculateTextStatsNative(uiState.note.content)
                    } catch (_: Throwable) {
                        val wc =
                            if (uiState.note.content.isBlank()) 0 else uiState.note.content.trim()
                                .split("\\s+".toRegex()).size
                        val cc = uiState.note.content.length
                        val rt = (wc / 200).coerceAtLeast(1)
                        intArrayOf(wc, cc, 0, rt)
                    }
                }
                val wordCount = stats[0]
                val charCount = stats[1]
                val readTime = stats[3]

                ModalBottomSheet(
                    onDismissRequest = { showInfoSheet = false },
                    sheetState = sheetState
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Note Details & Stats",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "File Path:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                uiState.note.id,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Note Type:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                uiState.note.noteType.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Word Count:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "$wordCount words",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Character Count:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "$charCount chars",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Est. Read Time:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "$readTime min read",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showInfoSheet = false },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Close")
                        }
                    }
                }
            }

            if (showBacklinksSheet) {
                val sheetState = rememberModalBottomSheetState()
                ModalBottomSheet(
                    onDismissRequest = { showBacklinksSheet = false },
                    sheetState = sheetState
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Text(
                            text = "Linked Notes (${uiState.note.links.size})",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Notes referencing this entry via [[WikiLinks]]",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))

                        if (uiState.note.links.isEmpty()) {
                            Text(
                                text = "No linked notes referencing this entry.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.heightIn(max = 280.dp)
                            ) {
                                items(uiState.note.links) { link ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                showBacklinksSheet = false
                                                onWikiLinkClick(link)
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Link,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = link,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = { showBacklinksSheet = false },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Close")
                        }
                    }
                }
            }
        }
    }
}

// ── FULL CASE-BY-CASE PREVIEWS ──

@Preview(
    showBackground = true,
    name = "1. Editor Screen - Normal Mode (Dark)",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun EditorScreenPreview_NormalMode_Dark() {
    val sampleNote = NoteItem(
        id = "Projects/Vocabulary.md",
        title = "Japanese Vocabulary",
        relativePath = "Projects",
        content = "# Japanese Vocabulary\n\n- Gakkou: School\n- Gakusei: Student\n- Sensei: Teacher\n\nSee [[Grammar]] for details."
    )

    val sampleTabs = listOf(
        EditorTabItem("Projects/Vocabulary.md", "Japanese Vocabulary")
    )

    TodoListTheme(darkTheme = true) {
        EditorContent(
            uiState = EditorUiState(note = sampleNote, openTabs = sampleTabs, isFocusMode = false),
            onBack = {},
            onSave = {}
        )
    }
}

@Preview(
    showBackground = true,
    name = "2. Editor Screen - Focus Mode (Dark)",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun EditorScreenPreview_FocusMode_Dark() {
    val sampleNote = NoteItem(
        id = "Projects/Roadmap.md",
        title = "Project Roadmap",
        relativePath = "Projects",
        content = "# Project Roadmap\n\nFocus mode hides top bars and headers for distraction-free writing."
    )

    TodoListTheme(darkTheme = true) {
        EditorContent(
            uiState = EditorUiState(note = sampleNote, openTabs = emptyList(), isFocusMode = true),
            onBack = {},
            onSave = {}
        )
    }
}
