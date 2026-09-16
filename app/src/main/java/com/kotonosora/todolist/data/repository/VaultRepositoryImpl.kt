package com.kotonosora.todolist.data.repository

import android.net.Uri
import com.kotonosora.todolist.data.database.LinkDao
import com.kotonosora.todolist.data.database.LinkEntity
import com.kotonosora.todolist.data.database.NoteDao
import com.kotonosora.todolist.data.database.NoteEntity
import com.kotonosora.todolist.data.database.TagDao
import com.kotonosora.todolist.data.database.TagEntity
import com.kotonosora.todolist.data.database.ZettelMetadataDao
import com.kotonosora.todolist.data.database.ZettelMetadataEntity
import com.kotonosora.todolist.data.file.VaultManager
import com.kotonosora.todolist.data.native.MdNativeHelper
import com.kotonosora.todolist.domain.model.NoteItem
import com.kotonosora.todolist.domain.model.NoteType
import com.kotonosora.todolist.domain.model.ParaCategory
import com.kotonosora.todolist.domain.model.VaultNode
import com.kotonosora.todolist.domain.model.ZettelUidGenerator
import com.kotonosora.todolist.domain.repository.VaultRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class VaultRepositoryImpl(
    private val vaultManager: VaultManager,
    private val noteDao: NoteDao,
    private val linkDao: LinkDao,
    private val tagDao: TagDao,
    private val zettelMetadataDao: ZettelMetadataDao
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

    /**
     * Renames a note and performs Cascading WikiLink Refactoring across all notes in the Vault.
     */
    override suspend fun renameNote(oldNoteId: String, newTitle: String, overrideUri: Uri?): Boolean = withContext(Dispatchers.IO) {
        val oldNote = getNoteById(oldNoteId) ?: return@withContext false
        val oldTitle = oldNote.title

        val newFilename = if (oldNote.id.contains("-")) {
            val prefix = oldNote.id.substringAfterLast("/").substringBefore("-")
            "$prefix-$newTitle.${oldNote.fileFormat}"
        } else {
            "$newTitle.${oldNote.fileFormat}"
        }

        val newNoteId = if (oldNote.relativePath.isBlank()) newFilename else "${oldNote.relativePath}/$newFilename"

        // 1. Update file header content
        val updatedContent = if (oldNote.content.startsWith("# $oldTitle")) {
            oldNote.content.replaceFirst("# $oldTitle", "# $newTitle")
        } else {
            oldNote.content
        }

        val updatedNote = oldNote.copy(
            id = newNoteId,
            title = newTitle,
            content = updatedContent,
            updatedAt = System.currentTimeMillis()
        )

        // 2. Perform local/SAF file rename
        val renamedOnDisk = vaultManager.renameNote(oldNoteId, newNoteId, overrideUri)
        if (renamedOnDisk) {
            vaultManager.saveNote(updatedNote, overrideUri)
            noteDao.deleteNoteById(oldNoteId)
            linkDao.deleteLinksForSource(oldNoteId)
            tagDao.deleteTagsForNote(oldNoteId)
            zettelMetadataDao.deleteMetadataForNote(oldNoteId)
            indexNoteToDb(updatedNote)

            // 3. Cascading WikiLink Refactoring across all other notes in the Vault
            val allNotes = noteDao.getAllNotesOnce()
            for (noteEntity in allNotes) {
                if (noteEntity.id != newNoteId && noteEntity.content.contains("[[$oldTitle")) {
                    val refactoredContent = noteEntity.content
                        .replace("[[$oldTitle]]", "[[$newTitle]]")
                        .replace("[[$oldTitle|", "[[$newTitle|")
                        .replace("[[$oldTitle#", "[[$newTitle#")

                    val refactoredNote = entityToDomain(noteEntity).copy(content = refactoredContent)
                    vaultManager.saveNote(refactoredNote, overrideUri)
                    indexNoteToDb(refactoredNote)
                }
            }
            return@withContext true
        }

        return@withContext false
    }

    override suspend fun deleteNote(relativePath: String, overrideUri: Uri?): Boolean = withContext(Dispatchers.IO) {
        val success = vaultManager.deleteNote(relativePath, overrideUri)
        if (success) {
            noteDao.deleteNoteById(relativePath)
            linkDao.deleteLinksForSource(relativePath)
            tagDao.deleteTagsForNote(relativePath)
            zettelMetadataDao.deleteMetadataForNote(relativePath)
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
        } catch (e: Throwable) {
            note.title
        }

        val extractedLinks = try {
            MdNativeHelper.extractWikiLinks(note.content).toList()
        } catch (e: Throwable) {
            emptyList()
        }

        val extractedTags = try {
            MdNativeHelper.extractTags(note.content).toList()
        } catch (e: Throwable) {
            emptyList()
        }

        val entity = NoteEntity(
            id = note.id,
            uid = note.uid.ifBlank { ZettelUidGenerator.generateUid() },
            title = parsedTitle,
            noteType = note.noteType.name,
            relativePath = note.relativePath,
            content = note.content,
            fileFormat = note.fileFormat,
            updatedAt = note.updatedAt,
            sizeBytes = note.sizeBytes
        )

        noteDao.insertNote(entity)

        // Index Zettel metadata
        val metadataEntity = ZettelMetadataEntity(
            noteId = note.id,
            uid = entity.uid,
            noteType = entity.noteType,
            author = note.author,
            sourceUrl = note.sourceUrl,
            paraCategory = note.paraCategory?.name
        )
        zettelMetadataDao.insertMetadata(metadataEntity)

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

    private suspend fun entityToDomain(entity: NoteEntity): NoteItem {
        val meta = zettelMetadataDao.getMetadataForNote(entity.id)
        val noteType = try {
            NoteType.valueOf(entity.noteType)
        } catch (e: Exception) {
            NoteType.PERMANENT
        }

        val paraCategory = try {
            meta?.paraCategory?.let { ParaCategory.valueOf(it) }
        } catch (e: Exception) {
            null
        }

        return NoteItem(
            id = entity.id,
            uid = entity.uid,
            title = entity.title,
            noteType = noteType,
            relativePath = entity.relativePath,
            content = entity.content,
            fileFormat = entity.fileFormat,
            updatedAt = entity.updatedAt,
            sizeBytes = entity.sizeBytes,
            author = meta?.author,
            sourceUrl = meta?.sourceUrl,
            paraCategory = paraCategory
        )
    }
}
