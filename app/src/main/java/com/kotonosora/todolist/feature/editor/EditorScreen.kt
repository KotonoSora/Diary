package com.kotonosora.todolist.feature.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kotonosora.todolist.domain.model.NoteItem

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
    onContentChange: (String) -> Unit,
    onWikiLinkClick: (String) -> Unit,
    onSuggestionSelected: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.note.title.ifBlank { "Untitled Note" },
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSave) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save Note"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                LivePreviewEditor(
                    content = uiState.note.content,
                    onContentChange = onContentChange,
                    onWikiLinkClick = onWikiLinkClick,
                    modifier = Modifier.fillMaxSize()
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
    }
}

@Preview(showBackground = true, name = "Editor Screen Preview")
@Composable
fun EditorScreenPreview() {
    val sampleNote = NoteItem(
        id = "Projects/Roadmap.md",
        title = "Project Roadmap",
        relativePath = "Projects",
        content = "# Project Roadmap\n\n- [x] Phase 1 Storage\n- [ ] Phase 2 Editor\n\nSee [[Architecture]] for details."
    )

    MaterialTheme {
        EditorContent(
            uiState = EditorUiState(note = sampleNote),
            onBack = {},
            onSave = {},
            onContentChange = {},
            onWikiLinkClick = {},
            onSuggestionSelected = {}
        )
    }
}
