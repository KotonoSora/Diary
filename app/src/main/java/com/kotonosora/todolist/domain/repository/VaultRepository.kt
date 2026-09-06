package com.kotonosora.todolist.domain.repository

import android.net.Uri
import com.kotonosora.todolist.domain.model.NoteItem
import com.kotonosora.todolist.domain.model.VaultNode
import kotlinx.coroutines.flow.Flow

interface VaultRepository {
    fun getAllNotes(): Flow<List<NoteItem>>
    fun getOutgoingLinks(sourceNoteId: String): Flow<List<String>>
    fun getIncomingLinks(targetTitle: String): Flow<List<String>>
    fun getAllTags(): Flow<List<String>>

    suspend fun getNoteById(id: String): NoteItem?
    suspend fun getVaultTree(overrideUri: Uri? = null): VaultNode.FolderNode
    suspend fun syncVaultFilesToDb(overrideUri: Uri? = null)
    suspend fun saveNote(note: NoteItem, overrideUri: Uri? = null): Boolean
    suspend fun deleteNote(relativePath: String, overrideUri: Uri? = null): Boolean
    suspend fun searchNotes(query: String): Flow<List<NoteItem>>
}
