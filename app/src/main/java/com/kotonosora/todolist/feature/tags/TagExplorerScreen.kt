package com.kotonosora.todolist.feature.tags

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotonosora.todolist.data.native.MdNativeHelper
import com.kotonosora.todolist.domain.model.NoteItem
import com.kotonosora.todolist.domain.repository.VaultRepository
import com.kotonosora.todolist.navigation.appViewModel
import com.kotonosora.todolist.ui.theme.AppTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

data class TagUiState(
    val tags: List<String> = emptyList(),
    val tagCounts: Map<String, Int> = emptyMap(),
    val selectedTag: String? = null,
    val taggedNotes: List<NoteItem> = emptyList()
)

class TagViewModel(
    private val vaultRepository: VaultRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TagUiState())
    val uiState: StateFlow<TagUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            vaultRepository.getAllTags().collect { tags ->
                _uiState.value = _uiState.value.copy(tags = tags)
            }
        }
        viewModelScope.launch {
            vaultRepository.getAllNotes().collect { notes ->
                val counts = mutableMapOf<String, Int>()
                for (note in notes) {
                    for (tag in noteTags(note.content)) {
                        counts[tag] = (counts[tag] ?: 0) + 1
                    }
                }
                _uiState.value = _uiState.value.copy(tagCounts = counts)
            }
        }
    }

    fun selectTag(tag: String) {
        viewModelScope.launch {
            val selected = if (_uiState.value.selectedTag == tag) null else tag
            _uiState.value = _uiState.value.copy(selectedTag = selected)
            if (selected != null) {
                val allNotes = vaultRepository.getAllNotes().firstOrNull() ?: emptyList()
                val matched = allNotes.filter { note -> selected in noteTags(note.content) }
                _uiState.value = _uiState.value.copy(taggedNotes = matched)
            } else {
                _uiState.value = _uiState.value.copy(taggedNotes = emptyList())
            }
        }
    }

    companion object {
        /** Exact tag match (body `#tag` + frontmatter `tags:`), not substring. */
        internal fun noteTags(content: String): Set<String> {
            return try {
                (MdNativeHelper.extractTags(content).toList() +
                    MdNativeHelper.parseFrontmatterTags(content)).toSet()
            } catch (_: Exception) {
                emptySet()
            }
        }
    }
}

@Composable
fun TagExplorerScreen(
    viewModel: TagViewModel = appViewModel { container -> TagViewModel(container.vaultRepository) },
    onNoteClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    TagExplorerContent(
        uiState = uiState,
        onSelectTag = { viewModel.selectTag(it) },
        onNoteClick = onNoteClick
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TagExplorerContent(
    uiState: TagUiState,
    onSelectTag: (String) -> Unit,
    onNoteClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (uiState.tags.isEmpty()) {
                Text(
                    text = "No tags yet — add #tags in your notes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            uiState.tags.forEach { tag ->
                val count = uiState.tagCounts[tag] ?: 0
                FilterChip(
                    selected = uiState.selectedTag == tag,
                    onClick = { onSelectTag(tag) },
                    label = { Text(if (count > 0) "$tag ($count)" else tag) }
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        if (uiState.selectedTag != null) {
            Text(
                text = if (uiState.taggedNotes.isEmpty())
                    "No notes with ${uiState.selectedTag}"
                else "Notes with ${uiState.selectedTag} (${uiState.taggedNotes.size})",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            LazyColumn {
                items(uiState.taggedNotes, key = { it.id }) { note ->
                    ListItem(
                        headlineContent = { Text(note.title) },
                        supportingContent = {
                            Text(note.content.take(120).replace("\n", " "))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNoteClick(note.id) }
                    )
                }
            }
        } else {
            Text(
                text = "Select a tag to see matching notes.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(
    showBackground = true,
    name = "1. Tag Explorer - Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun TagExplorerScreenPreview_Dark() {
    val sampleTags = listOf("#ideas", "#architecture", "#roadmap", "#meetings", "#personal")
    val sampleNotes = listOf(
        NoteItem("Ideas/Zettelkasten.md", "Zettelkasten Note", "Ideas", "Brainstorming #ideas"),
        NoteItem("Projects/Roadmap.md", "Project Roadmap", "Projects", "Important #ideas for Q1")
    )

    AppTheme(darkTheme = true) {
        TagExplorerContent(
            uiState = TagUiState(
                tags = sampleTags,
                selectedTag = "#ideas",
                taggedNotes = sampleNotes
            ),
            onSelectTag = {},
            onNoteClick = {}
        )
    }
}

@Preview(
    showBackground = true,
    name = "2. Tag Explorer - Light",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
fun TagExplorerScreenPreview_Light() {
    val sampleTags = listOf("#ideas", "#architecture", "#roadmap", "#meetings", "#personal")
    val sampleNotes = listOf(
        NoteItem("Ideas/Zettelkasten.md", "Zettelkasten Note", "Ideas", "Brainstorming #ideas"),
        NoteItem("Projects/Roadmap.md", "Project Roadmap", "Projects", "Important #ideas for Q1")
    )

    AppTheme(darkTheme = false) {
        TagExplorerContent(
            uiState = TagUiState(
                tags = sampleTags,
                selectedTag = "#ideas",
                taggedNotes = sampleNotes
            ),
            onSelectTag = {},
            onNoteClick = {}
        )
    }
}
