package com.kotonosora.todolist.feature.tags

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotonosora.todolist.domain.model.NoteItem
import com.kotonosora.todolist.domain.repository.VaultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TagUiState(
    val tags: List<String> = emptyList(),
    val selectedTag: String? = null,
    val taggedNotes: List<NoteItem> = emptyList()
)

@HiltViewModel
class TagViewModel @Inject constructor(
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
    }

    fun selectTag(tag: String) {
        viewModelScope.launch {
            val selected = if (_uiState.value.selectedTag == tag) null else tag
            _uiState.value = _uiState.value.copy(selectedTag = selected)
            if (selected != null) {
                val allNotes = vaultRepository.getAllNotes().firstOrNull() ?: emptyList()
                val matched = allNotes.filter { note -> note.content.contains(selected) }
                _uiState.value = _uiState.value.copy(taggedNotes = matched)
            } else {
                _uiState.value = _uiState.value.copy(taggedNotes = emptyList())
            }
        }
    }
}

@Composable
fun TagExplorerScreen(
    viewModel: TagViewModel = hiltViewModel(),
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
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tag Explorer") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "Tags in Vault",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.tags.forEach { tag ->
                    FilterChip(
                        selected = uiState.selectedTag == tag,
                        onClick = { onSelectTag(tag) },
                        label = { Text(tag) }
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            if (uiState.selectedTag != null) {
                Text(
                    text = "Notes with ${uiState.selectedTag}",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyColumn {
                    items(uiState.taggedNotes) { note ->
                        ListItem(
                            headlineContent = { Text(note.title) },
                            supportingContent = { Text(note.id) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNoteClick(note.id) }
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Tag Explorer Preview")
@Composable
fun TagExplorerScreenPreview() {
    val sampleTags = listOf("#ideas", "#architecture", "#roadmap", "#meetings", "#personal")
    val sampleNotes = listOf(
        NoteItem("Ideas/Zettelkasten.md", "Zettelkasten Note", "Ideas", "Brainstorming #ideas"),
        NoteItem("Projects/Roadmap.md", "Project Roadmap", "Projects", "Important #ideas for Q1")
    )

    MaterialTheme {
        TagExplorerContent(
            uiState = TagUiState(tags = sampleTags, selectedTag = "#ideas", taggedNotes = sampleNotes),
            onSelectTag = {},
            onNoteClick = {}
        )
    }
}
