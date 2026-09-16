package com.kotonosora.todolist.feature.editor

import android.content.Intent
import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kotonosora.todolist.domain.model.NoteItem
import com.kotonosora.todolist.ui.theme.TodoListTheme

@Composable
fun EditorScreen(
    noteId: String,
    viewModel: EditorViewModel,
    onBack: () -> Unit,
    onNavigateToNote: (String) -> Unit
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
        onSuggestionSelected = { viewModel.onSuggestionSelected(it) }
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
    onSuggestionSelected: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var showRenameSheet by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var renameInput by remember { mutableStateOf("") }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AnimatedVisibility(visible = !uiState.isFocusMode) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                                        text = { Text("Rename Note") },
                                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                        onClick = {
                                            showOptionsMenu = false
                                            renameInput = uiState.note.title
                                            showRenameSheet = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Share Note") },
                                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                        onClick = {
                                            showOptionsMenu = false
                                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                                putExtra(Intent.EXTRA_TITLE, uiState.note.title)
                                                putExtra(Intent.EXTRA_TEXT, uiState.note.content)
                                                type = "text/plain"
                                            }
                                            context.startActivity(Intent.createChooser(sendIntent, "Share Note"))
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

                    if (uiState.openTabs.isNotEmpty()) {
                        EditorTabStrip(
                            openTabs = uiState.openTabs,
                            activeTabId = uiState.note.id,
                            onTabSelect = onTabSelect,
                            onTabClose = onTabClose,
                            onNewTab = {}
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                EditorMetadataBar(
                    relativePath = uiState.note.id,
                    content = uiState.note.content,
                    noteType = uiState.note.noteType
                )

                LivePreviewEditor(
                    content = uiState.note.content,
                    onContentChange = onContentChange,
                    onWikiLinkClick = onWikiLinkClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )

                BacklinkPanel(
                    incomingLinks = uiState.note.links,
                    onNoteClick = onWikiLinkClick,
                    modifier = Modifier.fillMaxWidth()
                )
            }

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
                        Spacer(Modifier.height(0.dp).padding(horizontal = 6.dp))
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
    }
}
}

// ── FULL CASE-BY-CASE PREVIEWS ──

@Preview(showBackground = true, name = "1. Editor Screen - Normal Mode (Dark)", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun EditorScreenPreview_NormalMode_Dark() {
    val sampleNote = NoteItem(
        id = "Projects/Roadmap.md",
        title = "Project Roadmap",
        relativePath = "Projects",
        content = "# Project Roadmap\n\n- [x] Phase 1 Storage\n- [ ] Phase 2 Focus Editor\n\nSee [[Architecture]] for details."
    )

    val sampleTabs = listOf(
        EditorTabItem("Projects/Roadmap.md", "Project Roadmap"),
        EditorTabItem("Ideas/Zettel.md", "Zettel Concept")
    )

    TodoListTheme(darkTheme = true) {
        EditorContent(
            uiState = EditorUiState(note = sampleNote, openTabs = sampleTabs, isFocusMode = false),
            onBack = {},
            onSave = {}
        )
    }
}

@Preview(showBackground = true, name = "2. Editor Screen - Focus Mode (Dark)", uiMode = Configuration.UI_MODE_NIGHT_YES)
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

