package com.kotonosora.todolist.feature.editor

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

data class EditorUiState(
    val note: NoteItem = NoteItem(id = "", title = "", relativePath = "", content = ""),
    val suggestions: List<String> = emptyList(),
    val showSuggestions: Boolean = false,
    val isLoading: Boolean = false
)

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val vaultRepository: VaultRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    fun loadNote(noteId: String) {
        if (noteId.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val note = vaultRepository.getNoteById(noteId)
            if (note != null) {
                _uiState.value = _uiState.value.copy(note = note, isLoading = false)
            } else {
                _uiState.value = _uiState.value.copy(
                    note = NoteItem(id = noteId, title = noteId.substringBeforeLast(".").substringAfterLast("/"), relativePath = "", content = ""),
                    isLoading = false
                )
            }
        }
    }

    fun onContentChange(newContent: String) {
        val currentNote = _uiState.value.note.copy(content = newContent, updatedAt = System.currentTimeMillis())
        _uiState.value = _uiState.value.copy(note = currentNote)

        // Check for WikiLink trigger [[
        checkWikiLinkAutoComplete(newContent)
    }

    private fun checkWikiLinkAutoComplete(content: String) {
        val lastOpenIndex = content.lastIndexOf("[[")
        if (lastOpenIndex != -1) {
            val afterOpen = content.substring(lastOpenIndex + 2)
            if (!afterOpen.contains("]]") && !afterOpen.contains("\n")) {
                val query = afterOpen.trim()
                viewModelScope.launch {
                    val searchResult = vaultRepository.searchNotes(query).firstOrNull() ?: emptyList()
                    val suggestions = searchResult.map { it.title }.filter { it.isNotBlank() }
                    _uiState.value = _uiState.value.copy(
                        suggestions = suggestions,
                        showSuggestions = suggestions.isNotEmpty()
                    )
                }
                return
            }
        }
        _uiState.value = _uiState.value.copy(suggestions = emptyList(), showSuggestions = false)
    }

    fun onSuggestionSelected(selectedTitle: String) {
        val content = _uiState.value.note.content
        val lastOpenIndex = content.lastIndexOf("[[")
        if (lastOpenIndex != -1) {
            val prefix = content.substring(0, lastOpenIndex)
            val newContent = "$prefix[[$selectedTitle]] "
            val currentNote = _uiState.value.note.copy(content = newContent)
            _uiState.value = _uiState.value.copy(
                note = currentNote,
                suggestions = emptyList(),
                showSuggestions = false
            )
        }
    }

    fun saveNote(onSaved: () -> Unit = {}) {
        val note = _uiState.value.note
        if (note.id.isBlank()) return
        viewModelScope.launch {
            vaultRepository.saveNote(note)
            onSaved()
        }
    }
}
