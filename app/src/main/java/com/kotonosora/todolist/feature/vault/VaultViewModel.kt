package com.kotonosora.todolist.feature.vault

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotonosora.todolist.data.repository.UserPreferencesRepository
import com.kotonosora.todolist.domain.model.ActionStamp
import com.kotonosora.todolist.domain.model.EmotionStamp
import com.kotonosora.todolist.domain.model.NoteItem
import com.kotonosora.todolist.domain.model.NoteType
import com.kotonosora.todolist.domain.model.VaultNode
import com.kotonosora.todolist.domain.model.ZettelUidGenerator
import com.kotonosora.todolist.domain.repository.VaultRepository
import com.kotonosora.todolist.feature.editor.ZettelTemplatePicker
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

data class VaultUiState(
    val rootNode: VaultNode.FolderNode = VaultNode.FolderNode(name = "Vault", relativePath = ""),
    val notes: List<NoteItem> = emptyList(),
    val isLoading: Boolean = false
)

class VaultViewModel(
    private val vaultRepository: VaultRepository,
    private val userPreferencesRepository: UserPreferencesRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(VaultUiState())
    val uiState: StateFlow<VaultUiState> = _uiState.asStateFlow()

    private var currentVaultUri: Uri? = null

    init {
        viewModelScope.launch {
            val savedUri = userPreferencesRepository?.customStorageFolderUri?.firstOrNull()
            if (!savedUri.isNullOrBlank()) {
                currentVaultUri = Uri.parse(savedUri)
            }
            vaultRepository.getAllNotes().collect { notes ->
                _uiState.value = _uiState.value.copy(notes = notes)
            }
        }
        loadVault()
    }

    fun loadVault(overrideUri: Uri? = null) {
        if (overrideUri != null) {
            currentVaultUri = overrideUri
            viewModelScope.launch {
                userPreferencesRepository?.saveCustomStorageFolderUri(overrideUri.toString())
            }
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val startTime = System.currentTimeMillis()
            if (currentVaultUri == null) {
                val savedUriStr = userPreferencesRepository?.customStorageFolderUri?.firstOrNull()
                if (!savedUriStr.isNullOrBlank()) {
                    currentVaultUri = Uri.parse(savedUriStr)
                }
            }
            vaultRepository.syncVaultFilesToDb(currentVaultUri)
            val tree = vaultRepository.getVaultTree(currentVaultUri)
            val elapsedTime = System.currentTimeMillis() - startTime
            if (elapsedTime < 600) {
                delay(600 - elapsedTime)
            }
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
            val success = vaultRepository.saveNote(note, currentVaultUri)
            if (success) {
                loadVault()
                onCreated(relativePath)
            }
        }
    }

    fun createFolder(parentFolderPath: String, folderName: String) {
        if (folderName.isBlank()) return
        val folderPath =
            if (parentFolderPath.isBlank()) folderName.trim() else "$parentFolderPath/${folderName.trim()}"
        viewModelScope.launch {
            val success = vaultRepository.createFolder(folderPath, currentVaultUri)
            if (success) {
                loadVault()
            }
        }
    }

    fun moveNote(oldRelativePath: String, destFolderPath: String) {
        viewModelScope.launch {
            val success = vaultRepository.moveNote(oldRelativePath, destFolderPath, currentVaultUri)
            if (success) {
                loadVault()
            }
        }
    }

    fun moveFolder(oldRelativePath: String, destFolderPath: String) {
        viewModelScope.launch {
            val success =
                vaultRepository.moveFolder(oldRelativePath, destFolderPath, currentVaultUri)
            if (success) {
                loadVault()
            }
        }
    }

    fun deleteNote(relativePath: String) {
        viewModelScope.launch {
            val success = vaultRepository.deleteNote(relativePath, currentVaultUri)
            if (success) {
                loadVault()
            }
        }
    }

    fun deleteFolder(folderPath: String) {
        if (folderPath.isBlank()) return
        viewModelScope.launch {
            val success = vaultRepository.deleteFolder(folderPath, currentVaultUri)
            if (success) {
                loadVault()
            }
        }
    }
}
