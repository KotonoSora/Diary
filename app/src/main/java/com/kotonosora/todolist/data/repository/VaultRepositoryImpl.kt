package com.kotonosora.todolist.data.repository

import android.net.Uri
import com.kotonosora.todolist.data.database.LinkDao
import com.kotonosora.todolist.data.database.LinkEntity
import com.kotonosora.todolist.data.database.NoteDao
import com.kotonosora.todolist.data.database.NoteEntity
import com.kotonosora.todolist.data.database.TagDao
import com.kotonosora.todolist.data.database.TagEntity
import com.kotonosora.todolist.data.file.VaultManager
import com.kotonosora.todolist.data.native.MdNativeHelper
import com.kotonosora.todolist.domain.model.NoteItem
import com.kotonosora.todolist.domain.model.VaultNode
import com.kotonosora.todolist.domain.repository.VaultRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultRepositoryImpl @Inject constructor(
    private val vaultManager: VaultManager,
    private val noteDao: NoteDao,
    private val linkDao: LinkDao,
    private val tagDao: TagDao
) : VaultRepository {

    override fun getAllNotes(): Flow<List<NoteItem>> {
        return noteDao.getAllNotes().map { entities ->
            entities.map { entityToDomain(it) }
        }
    }

    override fun getOutgoingLinks(sourceNoteId: String): Flow<List<String>> {
        return linkDao.getOutgoingLinksForNote(sourceNoteId).map { links ->
            links.map { it.targetTitle }
        }
    }

    override fun getIncomingLinks(targetTitle: String): Flow<List<String>> {
        return linkDao.getIncomingLinksForNoteTitle(targetTitle).map { links ->
            links.map { it.sourceNoteId }
        }
    }

    override fun getAllTags(): Flow<List<String>> {
        return tagDao.getAllTags()
    }

    override suspend fun getNoteById(id: String): NoteItem? = withContext(Dispatchers.IO) {
        val entity = noteDao.getNoteById(id) ?: return@withContext null
        entityToDomain(entity)
    }

    override suspend fun getVaultTree(overrideUri: Uri?): VaultNode.FolderNode {
        return vaultManager.getVaultTree(overrideUri)
    }

    override suspend fun syncVaultFilesToDb(overrideUri: Uri?) = withContext(Dispatchers.IO) {
        val notesFromFiles = vaultManager.readAllNotes(overrideUri)
        for (note in notesFromFiles) {
            indexNoteToDb(note)
        }
    }

    override suspend fun saveNote(note: NoteItem, overrideUri: Uri?): Boolean = withContext(Dispatchers.IO) {
        val success = vaultManager.saveNote(note, overrideUri)
        if (success) {
            indexNoteToDb(note)
        }
        success
    }

    override suspend fun deleteNote(relativePath: String, overrideUri: Uri?): Boolean = withContext(Dispatchers.IO) {
        val success = vaultManager.deleteNote(relativePath, overrideUri)
        if (success) {
            noteDao.deleteNoteById(relativePath)
            linkDao.deleteLinksForSource(relativePath)
            tagDao.deleteTagsForNote(relativePath)
        }
        success
    }

    override suspend fun searchNotes(query: String): Flow<List<NoteItem>> {
        return noteDao.searchNotesByTitle(query).map { entities ->
            entities.map { entityToDomain(it) }
        }
    }

    private suspend fun indexNoteToDb(note: NoteItem) {
        val parsedTitle = try {
            MdNativeHelper.parseTitle(note.content).ifBlank { note.title }
        } catch (e: UnsatisfiedLinkError) {
            note.title
        } catch (e: Exception) {
            note.title
        }

        val extractedLinks = try {
            MdNativeHelper.extractWikiLinks(note.content).toList()
        } catch (e: UnsatisfiedLinkError) {
            emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        val extractedTags = try {
            MdNativeHelper.extractTags(note.content).toList()
        } catch (e: UnsatisfiedLinkError) {
            emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        val entity = NoteEntity(
            id = note.id,
            title = parsedTitle,
            relativePath = note.relativePath,
            content = note.content,
            fileFormat = note.fileFormat,
            updatedAt = note.updatedAt,
            sizeBytes = note.sizeBytes
        )

        noteDao.insertNote(entity)

        // Update links index
        linkDao.deleteLinksForSource(note.id)
        val linkEntities = extractedLinks.map { targetTitle ->
            val targetEntity = noteDao.getNoteByTitle(targetTitle)
            LinkEntity(
                id = "${note.id}->$targetTitle",
                sourceNoteId = note.id,
                targetTitle = targetTitle,
                targetNoteId = targetEntity?.id,
                isResolved = targetEntity != null
            )
        }
        if (linkEntities.isNotEmpty()) {
            linkDao.insertLinks(linkEntities)
        }

        // Update tags index
        tagDao.deleteTagsForNote(note.id)
        val tagEntities = extractedTags.map { tag ->
            TagEntity(
                noteId = note.id,
                tagName = tag
            )
        }
        if (tagEntities.isNotEmpty()) {
            tagDao.insertTags(tagEntities)
        }
    }

    private fun entityToDomain(entity: NoteEntity): NoteItem {
        return NoteItem(
            id = entity.id,
            title = entity.title,
            relativePath = entity.relativePath,
            content = entity.content,
            fileFormat = entity.fileFormat,
            updatedAt = entity.updatedAt,
            sizeBytes = entity.sizeBytes
        )
    }
}
