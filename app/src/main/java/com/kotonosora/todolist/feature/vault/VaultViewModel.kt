package com.kotonosora.todolist.feature.vault

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotonosora.todolist.domain.model.ActionStamp
import com.kotonosora.todolist.domain.model.EmotionStamp
import com.kotonosora.todolist.domain.model.NoteItem
import com.kotonosora.todolist.domain.model.NoteType
import com.kotonosora.todolist.domain.model.VaultNode
import com.kotonosora.todolist.domain.model.ZettelUidGenerator
import com.kotonosora.todolist.domain.repository.VaultRepository
import com.kotonosora.todolist.feature.editor.ZettelTemplatePicker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class VaultUiState(
    val rootNode: VaultNode.FolderNode = VaultNode.FolderNode(name = "Vault", relativePath = ""),
    val notes: List<NoteItem> = emptyList(),
    val isLoading: Boolean = false
)

class VaultViewModel(
    private val vaultRepository: VaultRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VaultUiState())
    val uiState: StateFlow<VaultUiState> = _uiState.asStateFlow()

    init {
        loadVault()
    }

    fun loadVault(overrideUri: Uri? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            vaultRepository.syncVaultFilesToDb(overrideUri)
            val tree = vaultRepository.getVaultTree(overrideUri)
            _uiState.value = _uiState.value.copy(rootNode = tree, isLoading = false)
        }
    }

    fun createZettelNoteInFolder(
        folderPath: String,
        title: String,
        noteType: NoteType,
        author: String? = null,
        sourceUrl: String? = null,
        emotion: EmotionStamp? = null,
        actions: List<ActionStamp> = emptyList(),
        onCreated: (String) -> Unit
    ) {
        if (title.isBlank()) return
        val uid = ZettelUidGenerator.generateUid()
        val filename = "$uid-$title.md"
        val relativePath = if (folderPath.isBlank()) filename else "$folderPath/$filename"
        val content = ZettelTemplatePicker.generateContentForTemplate(
            noteType = noteType,
            title = title,
            author = author,
            sourceUrl = sourceUrl,
            emotion = emotion,
            actions = actions
        )

        val note = NoteItem(
            id = relativePath,
            uid = uid,
            title = title,
            noteType = noteType,
            relativePath = folderPath,
            content = content,
            author = author,
            sourceUrl = sourceUrl,
            emotion = emotion,
            actions = actions
        )

        viewModelScope.launch {
            val success = vaultRepository.saveNote(note)
            if (success) {
                loadVault()
                onCreated(relativePath)
            }
        }
    }

    fun deleteNote(relativePath: String) {
        viewModelScope.launch {
            val success = vaultRepository.deleteNote(relativePath)
            if (success) {
                loadVault()
            }
        }
    }
}
