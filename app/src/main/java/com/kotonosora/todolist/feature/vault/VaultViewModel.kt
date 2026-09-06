package com.kotonosora.todolist.feature.vault

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotonosora.todolist.domain.model.NoteItem
import com.kotonosora.todolist.domain.model.NoteType
import com.kotonosora.todolist.domain.model.VaultNode
import com.kotonosora.todolist.domain.model.ZettelUidGenerator
import com.kotonosora.todolist.domain.repository.VaultRepository
import com.kotonosora.todolist.feature.editor.ZettelTemplatePicker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VaultUiState(
    val rootNode: VaultNode.FolderNode = VaultNode.FolderNode(name = "Vault", relativePath = ""),
    val notes: List<NoteItem> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class VaultViewModel @Inject constructor(
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
            sourceUrl = sourceUrl
        )

        val note = NoteItem(
            id = relativePath,
            uid = uid,
            title = title,
            noteType = noteType,
            relativePath = folderPath,
            content = content,
            author = author,
            sourceUrl = sourceUrl
        )

        viewModelScope.launch {
            val success = vaultRepository.saveNote(note)
            if (success) {
                loadVault()
                onCreated(relativePath)
            }
        }
    }
}
