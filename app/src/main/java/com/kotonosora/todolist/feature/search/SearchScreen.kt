package com.kotonosora.todolist.feature.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
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

data class SearchUiState(
    val query: String = "",
    val searchResults: List<NoteItem> = emptyList()
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val vaultRepository: VaultRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    fun onQueryChange(newQuery: String) {
        _uiState.value = _uiState.value.copy(query = newQuery)
        if (newQuery.isBlank()) {
            _uiState.value = _uiState.value.copy(searchResults = emptyList())
            return
        }
        viewModelScope.launch {
            val results = vaultRepository.searchNotes(newQuery).firstOrNull() ?: emptyList()
            _uiState.value = _uiState.value.copy(searchResults = results)
        }
    }

    fun clearQuery() {
        onQueryChange("")
    }
}

@Composable
fun SearchScreen(
    viewModel: SearchViewModel = hiltViewModel(),
    onNoteClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    SearchContent(
        uiState = uiState,
        onQueryChange = { viewModel.onQueryChange(it) },
        onClearQuery = { viewModel.clearQuery() },
        onNoteClick = onNoteClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchContent(
    uiState: SearchUiState,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onNoteClick: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Full-Text Search") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search titles, notes, or #tags...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null
                    )
                },
                trailingIcon = {
                    if (uiState.query.isNotEmpty()) {
                        IconButton(onClick = onClearQuery) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search"
                            )
                        }
                    }
                },
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            if (uiState.query.isNotEmpty()) {
                Text(
                    text = "Results (${uiState.searchResults.size})",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                LazyColumn {
                    items(uiState.searchResults) { note ->
                        ListItem(
                            headlineContent = { Text(note.title) },
                            supportingContent = {
                                Text(
                                    text = note.content.take(120).replace("\n", " "),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
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

@Preview(showBackground = true, name = "Search Screen Preview")
@Composable
fun SearchScreenPreview() {
    val results = listOf(
        NoteItem("Projects/Roadmap.md", "Project Roadmap", "Projects", "Milestones for Q1 architecture and SAF integration."),
        NoteItem("Work/Meeting.md", "Meeting Notes", "Work", "Discussed roadmap timelines and FTS search indexing.")
    )

    MaterialTheme {
        SearchContent(
            uiState = SearchUiState(query = "roadmap", searchResults = results),
            onQueryChange = {},
            onClearQuery = {},
            onNoteClick = {}
        )
    }
}
