package com.kotonosora.todolist.data.repository

import android.net.Uri
import com.kotonosora.todolist.domain.model.NoteItem
import com.kotonosora.todolist.domain.model.VaultNode
import com.kotonosora.todolist.domain.repository.VaultRepository
import kotlinx.coroutines.flow.Flow

/**
 * Feature-Flagged Repository Abstraction (Phase 5 Risk Mitigation).
 * Allows toggling between new refactored Clean Architecture storage and legacy fallback implementation.
 */
class FeatureFlaggedVaultRepository(
    private val cleanRepository: VaultRepository,
    private val legacyRepository: VaultRepository,
    private val isCleanArchitectureEnabled: Boolean = true
) : VaultRepository {

    private val activeRepository: VaultRepository
        get() = if (isCleanArchitectureEnabled) cleanRepository else legacyRepository

    override fun getAllNotes(): Flow<List<NoteItem>> {
        return activeRepository.getAllNotes()
    }

    override fun getOutgoingLinks(sourceNoteId: String): Flow<List<String>> {
        return activeRepository.getOutgoingLinks(sourceNoteId)
    }

    override fun getIncomingLinks(targetTitle: String): Flow<List<String>> {
        return activeRepository.getIncomingLinks(targetTitle)
    }

    override fun getAllTags(): Flow<List<String>> {
        return activeRepository.getAllTags()
    }

    override suspend fun getNoteById(id: String): NoteItem? {
        return activeRepository.getNoteById(id)
    }

    override suspend fun getVaultTree(overrideUri: Uri?): VaultNode.FolderNode {
        return activeRepository.getVaultTree(overrideUri)
    }

    override suspend fun syncVaultFilesToDb(overrideUri: Uri?) {
        activeRepository.syncVaultFilesToDb(overrideUri)
    }

    override suspend fun createFolder(folderPath: String, overrideUri: Uri?): Boolean {
        return activeRepository.createFolder(folderPath, overrideUri)
    }

    override suspend fun saveNote(note: NoteItem, overrideUri: Uri?): Boolean {
        return activeRepository.saveNote(note, overrideUri)
    }

    override suspend fun renameNote(
        oldNoteId: String,
        newTitle: String,
        overrideUri: Uri?
    ): Boolean {
        return activeRepository.renameNote(oldNoteId, newTitle, overrideUri)
    }

    override suspend fun moveNote(
        oldRelativePath: String,
        destFolderPath: String,
        overrideUri: Uri?
    ): Boolean {
        return activeRepository.moveNote(oldRelativePath, destFolderPath, overrideUri)
    }

    override suspend fun moveFolder(
        oldRelativePath: String,
        destFolderPath: String,
        overrideUri: Uri?
    ): Boolean {
        return activeRepository.moveFolder(oldRelativePath, destFolderPath, overrideUri)
    }

    override suspend fun deleteNote(relativePath: String, overrideUri: Uri?): Boolean {
        return activeRepository.deleteNote(relativePath, overrideUri)
    }

    override suspend fun deleteFolder(folderPath: String, overrideUri: Uri?): Boolean {
        return activeRepository.deleteFolder(folderPath, overrideUri)
    }

    override suspend fun searchNotes(query: String): Flow<List<NoteItem>> {
        return activeRepository.searchNotes(query)
    }
}
