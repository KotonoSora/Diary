package com.kotonosora.todolist.feature.search

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotonosora.todolist.domain.model.NoteItem
import com.kotonosora.todolist.domain.usecase.VaultUseCases
import com.kotonosora.todolist.navigation.appViewModel
import com.kotonosora.todolist.ui.theme.AppTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val searchResults: List<NoteItem> = emptyList(),
    val isSearching: Boolean = false
)

class SearchViewModel(
    private val useCases: VaultUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(newQuery: String) {
        _uiState.value = _uiState.value.copy(query = newQuery)
        searchJob?.cancel()
        if (newQuery.isBlank()) {
            _uiState.value = _uiState.value.copy(searchResults = emptyList(), isSearching = false)
            return
        }
        // Debounce rapid typing: only the latest query hits the repository,
        // so stale results can't overwrite fresh ones.
        _uiState.value = _uiState.value.copy(isSearching = true)
        searchJob = viewModelScope.launch {
            delay(250)
            val results = useCases.searchNotes(newQuery).firstOrNull() ?: emptyList()
            // Drop results if the query changed while we were searching.
            if (_uiState.value.query != newQuery) return@launch
            _uiState.value = _uiState.value.copy(searchResults = results, isSearching = false)
        }
    }

    fun clearQuery() {
        searchJob?.cancel()
        _uiState.value = SearchUiState()
    }
}

@Composable
fun SearchScreen(
    viewModel: SearchViewModel = appViewModel { container -> SearchViewModel(container.vaultUseCases) },
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
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
            singleLine = true,
            shape = RoundedCornerShape(24.dp)
        )

        Spacer(Modifier.height(12.dp))

        if (uiState.query.isNotEmpty()) {
            Text(
                text = when {
                    uiState.isSearching -> "Searching…"
                    uiState.searchResults.isEmpty() -> "No results for \"${uiState.query}\""
                    else -> "Results (${uiState.searchResults.size})"
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            LazyColumn {
                items(uiState.searchResults, key = { it.id }) { note ->
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

@Preview(
    showBackground = true,
    name = "1. Search Screen - Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun SearchScreenPreview_Dark() {
    val results = listOf(
        NoteItem(
            "Projects/Roadmap.md",
            "Project Roadmap",
            "Projects",
            "Milestones for Q1 architecture and SAF integration."
        ),
        NoteItem(
            "Work/Meeting.md",
            "Meeting Notes",
            "Work",
            "Discussed roadmap timelines and FTS search indexing."
        )
    )

    AppTheme(darkTheme = true) {
        SearchContent(
            uiState = SearchUiState(query = "roadmap", searchResults = results),
            onQueryChange = {},
            onClearQuery = {},
            onNoteClick = {}
        )
    }
}

@Preview(
    showBackground = true,
    name = "2. Search Screen - Light",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
fun SearchScreenPreview_Light() {
    val results = listOf(
        NoteItem(
            "Projects/Roadmap.md",
            "Project Roadmap",
            "Projects",
            "Milestones for Q1 architecture and SAF integration."
        ),
        NoteItem(
            "Work/Meeting.md",
            "Meeting Notes",
            "Work",
            "Discussed roadmap timelines and FTS search indexing."
        )
    )

    AppTheme(darkTheme = false) {
        SearchContent(
            uiState = SearchUiState(query = "roadmap", searchResults = results),
            onQueryChange = {},
            onClearQuery = {},
            onNoteClick = {}
        )
    }
}
