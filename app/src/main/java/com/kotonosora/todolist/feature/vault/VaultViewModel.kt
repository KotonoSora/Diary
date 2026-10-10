package com.kotonosora.todolist.feature.vault

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotonosora.todolist.domain.model.ActionStamp
import com.kotonosora.todolist.domain.model.EmotionStamp
import com.kotonosora.todolist.domain.model.NoteItem
import com.kotonosora.todolist.domain.model.NoteType
import com.kotonosora.todolist.domain.model.VaultNode
import com.kotonosora.todolist.domain.usecase.PreferencesUseCases
import com.kotonosora.todolist.domain.usecase.VaultUseCases
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

/**
 * DDD: depends on [VaultUseCases] (application layer), never on the
 * repository directly. The repository-based secondary constructor is kept
 * for backward compatibility and delegates to the use-case bundle.
 */
class VaultViewModel(
    private val useCases: VaultUseCases,
    private val prefs: PreferencesUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(VaultUiState())
    val uiState: StateFlow<VaultUiState> = _uiState.asStateFlow()

    private var currentVaultUri: Uri? = null

    init {
        viewModelScope.launch {
            val savedUri = prefs.observeCustomStorageFolder().firstOrNull()
            if (!savedUri.isNullOrBlank()) {
                currentVaultUri = Uri.parse(savedUri)
            }
            useCases.observeNotes().collect { notes ->
                _uiState.value = _uiState.value.copy(notes = notes)
            }
        }
        loadVault()
    }

    fun loadVault(overrideUri: Uri? = null) {
        if (overrideUri != null) {
            currentVaultUri = overrideUri
            viewModelScope.launch {
                prefs.saveCustomStorageFolder(overrideUri.toString())
            }
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val startTime = System.currentTimeMillis()
            if (currentVaultUri == null) {
                val savedUriStr = prefs.observeCustomStorageFolder().firstOrNull()
                if (!savedUriStr.isNullOrBlank()) {
                    currentVaultUri = Uri.parse(savedUriStr)
                }
            }
            useCases.syncVault(currentVaultUri)
            val tree = useCases.getVaultTree(currentVaultUri)
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
        viewModelScope.launch {
            val relativePath = useCases.createZettelNote(
                folderPath = folderPath,
                title = title,
                noteType = noteType,
                author = author,
                sourceUrl = sourceUrl,
                emotion = emotion,
                actions = actions,
                overrideUri = currentVaultUri
            )
            if (relativePath != null) {
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
            val success = useCases.createFolder(folderPath, currentVaultUri)
            if (success) {
                loadVault()
            }
        }
    }

    fun moveNote(oldRelativePath: String, destFolderPath: String) {
        viewModelScope.launch {
            val success = useCases.moveNote(oldRelativePath, destFolderPath, currentVaultUri)
            if (success) {
                loadVault()
            }
        }
    }

    fun moveFolder(oldRelativePath: String, destFolderPath: String) {
        viewModelScope.launch {
            val success =
                useCases.moveFolder(oldRelativePath, destFolderPath, currentVaultUri)
            if (success) {
                loadVault()
            }
        }
    }

    fun deleteNote(relativePath: String) {
        viewModelScope.launch {
            val success = useCases.deleteNote(relativePath, currentVaultUri)
            if (success) {
                loadVault()
            }
        }
    }

    fun deleteFolder(folderPath: String) {
        if (folderPath.isBlank()) return
        viewModelScope.launch {
            val success = useCases.deleteFolder(folderPath, currentVaultUri)
            if (success) {
                loadVault()
            }
        }
    }
}
