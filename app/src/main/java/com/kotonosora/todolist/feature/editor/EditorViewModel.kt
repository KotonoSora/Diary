package com.kotonosora.todolist.feature.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotonosora.todolist.domain.model.ActionStamp
import com.kotonosora.todolist.domain.model.EmotionStamp
import com.kotonosora.todolist.domain.model.NoteItem
import com.kotonosora.todolist.domain.model.TEMPLATE_PLACEHOLDER_TITLE
import com.kotonosora.todolist.domain.model.renamedId
import com.kotonosora.todolist.domain.usecase.VaultUseCases
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

data class EditorUiState(
    val note: NoteItem = NoteItem(id = "", title = "", relativePath = "", content = ""),
    val openTabs: List<EditorTabItem> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val showSuggestions: Boolean = false,
    val isFocusMode: Boolean = false,
    val isLoading: Boolean = false,
    val customTemplates: List<String> = emptyList()
)

class EditorViewModel(
    private val useCases: VaultUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private var autocompleteJob: Job? = null

    fun loadNote(noteId: String) {
        if (noteId.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val note = useCases.getNoteById(noteId) ?: NoteItem(
                id = noteId,
                title = noteId.substringBeforeLast(".").substringAfterLast("/"),
                relativePath = "",
                content = ""
            )

            val currentTabs = _uiState.value.openTabs.toMutableList()
            if (currentTabs.none { it.id == note.id }) {
                currentTabs.add(EditorTabItem(id = note.id, title = note.title))
            }

            _uiState.value = _uiState.value.copy(
                note = note,
                openTabs = currentTabs,
                isLoading = false
            )
        }
    }

    fun toggleFocusMode() {
        _uiState.value = _uiState.value.copy(isFocusMode = !_uiState.value.isFocusMode)
    }

    fun closeTab(tabId: String) {
        val currentTabs = _uiState.value.openTabs.filterNot { it.id == tabId }
        _uiState.value = _uiState.value.copy(openTabs = currentTabs)
    }

    fun onContentChange(newContent: String) {
        val currentNote =
            _uiState.value.note.copy(content = newContent, updatedAt = System.currentTimeMillis())
        _uiState.value = _uiState.value.copy(note = currentNote)

        // Check for WikiLink trigger [[
        checkWikiLinkAutoComplete(newContent)
    }

    private fun checkWikiLinkAutoComplete(content: String) {
        autocompleteJob?.cancel()
        val lastOpenIndex = content.lastIndexOf("[[")
        if (lastOpenIndex == -1) {
            _uiState.value = _uiState.value.copy(suggestions = emptyList(), showSuggestions = false)
            return
        }
        val afterOpen = content.substring(lastOpenIndex + 2)
        if (afterOpen.contains("]]") || afterOpen.contains("\n")) {
            _uiState.value = _uiState.value.copy(suggestions = emptyList(), showSuggestions = false)
            return
        }
        val query = afterOpen.trim()
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(suggestions = emptyList(), showSuggestions = false)
            return
        }
        // Debounce keystrokes: one search per pause, never stacked.
        autocompleteJob = viewModelScope.launch {
            delay(250)
            val searchResult = try {
                useCases.searchNotes(query).firstOrNull() ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
            val suggestions = searchResult.map { it.title }.filter { it.isNotBlank() }
            _uiState.value = _uiState.value.copy(
                suggestions = suggestions,
                showSuggestions = suggestions.isNotEmpty()
            )
        }
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

    fun renameNote(newTitle: String, onRenamed: (String) -> Unit = {}) {
        val currentNote = _uiState.value.note
        if (newTitle.isBlank() || currentNote.id.isBlank()) return

        viewModelScope.launch {
            val success = useCases.renameNote(currentNote.id, newTitle)
            if (success) {
                val newNoteId = currentNote.renamedId(newTitle)
                loadNote(newNoteId)
                onRenamed(newNoteId)
            }
        }
    }

    fun saveNote(onSaved: () -> Unit = {}) {
        val note = _uiState.value.note
        if (note.id.isBlank()) return
        viewModelScope.launch {
            useCases.saveNote(note)
            onSaved()
        }
    }

    /**
     * Mood-stamp CRUD for the open note: rewrites the frontmatter stamps via
     * [com.kotonosora.todolist.domain.usecase.UpdateNoteStampsUseCase] and
     * reloads so the metadata bar reflects the change.
     */
    fun updateStamps(emotion: EmotionStamp?, actions: List<ActionStamp> = emptyList()) {
        val noteId = _uiState.value.note.id
        if (noteId.isBlank()) return
        viewModelScope.launch {
            if (useCases.updateNoteStamps(noteId, emotion, actions)) {
                loadNote(noteId)
            }
        }
    }

    fun loadCustomTemplates() {
        viewModelScope.launch {
            val names = try {
                useCases.templateNames()
            } catch (e: Exception) {
                emptyList()
            }
            _uiState.value = _uiState.value.copy(customTemplates = names)
        }
    }

    fun applyCustomTemplate(name: String) {
        viewModelScope.launch {
            val raw = try {
                useCases.templateContent(name)
            } catch (e: Exception) {
                null
            } ?: return@launch
            val title = _uiState.value.note.title.ifBlank { "Untitled Note" }
            val templateContent = raw.replace(
                TEMPLATE_PLACEHOLDER_TITLE,
                title
            )
            // Never destroy an unsaved draft: only replace empty notes, otherwise append.
            val current = _uiState.value.note.content
            val content = if (current.isBlank()) {
                templateContent
            } else {
                current.trimEnd() + "\n\n" + templateContent
            }
            _uiState.value = _uiState.value.copy(
                note = _uiState.value.note.copy(
                    content = content,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun saveCurrentNoteAsTemplate(name: String, onSaved: () -> Unit = {}) {
        if (name.isBlank()) return
        val content = _uiState.value.note.content
        if (content.isBlank()) return
        viewModelScope.launch {
            val success = try {
                useCases.saveTemplate(name, content)
            } catch (e: Exception) {
                false
            }
            if (success) {
                loadCustomTemplates()
                onSaved()
            }
        }
    }

    fun deleteCustomTemplate(name: String) {
        viewModelScope.launch {
            try {
                useCases.deleteTemplate(name)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            loadCustomTemplates()
        }
    }
}
