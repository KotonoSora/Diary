package com.kotonosora.todolist.data.repository

import android.net.Uri
import com.kotonosora.todolist.data.database.LinkDao
import com.kotonosora.todolist.data.database.LinkEntity
import com.kotonosora.todolist.data.database.NoteDao
import com.kotonosora.todolist.data.database.NoteEntity
import com.kotonosora.todolist.data.database.NoteFtsDao
import com.kotonosora.todolist.data.database.NoteFtsEntity
import com.kotonosora.todolist.data.database.TagDao
import com.kotonosora.todolist.data.database.TagEntity
import com.kotonosora.todolist.data.database.ZettelMetadataDao
import com.kotonosora.todolist.data.database.ZettelMetadataEntity
import com.kotonosora.todolist.data.file.VaultManager
import com.kotonosora.todolist.data.native.MdNativeHelper
import com.kotonosora.todolist.domain.model.FtsQueryBuilder
import com.kotonosora.todolist.domain.model.NoteItem
import com.kotonosora.todolist.domain.model.NoteType
import com.kotonosora.todolist.domain.model.ParaCategory
import com.kotonosora.todolist.domain.model.StampParser
import com.kotonosora.todolist.domain.model.VaultNode
import com.kotonosora.todolist.domain.model.ZettelUidGenerator
import com.kotonosora.todolist.domain.model.renamedId
import com.kotonosora.todolist.domain.repository.VaultRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class VaultRepositoryImpl(
    private val vaultManager: VaultManager,
    private val noteDao: NoteDao,
    private val linkDao: LinkDao,
    private val tagDao: TagDao,
    private val zettelMetadataDao: ZettelMetadataDao,
    private val noteFtsDao: NoteFtsDao
) : VaultRepository {

    override fun getAllNotes(): Flow<List<NoteItem>> {
        return noteDao.getAllNotes().map { entities ->
            // Batch metadata in one query instead of N+1 per-note lookups.
            val metaMap = if (entities.isEmpty()) {
                emptyMap()
            } else {
                try {
                    zettelMetadataDao.getMetadataForNotes(entities.map { it.id })
                        .associateBy { it.noteId }
                } catch (e: Exception) {
                    android.util.Log.w("VaultRepository", "metadata batch failed", e)
                    emptyMap()
                }
            }
            entities.map { entityToDomain(it, metaMap[it.id]) }
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
        val meta = try {
            zettelMetadataDao.getMetadataForNote(id)
        } catch (e: Exception) {
            null
        }
        entityToDomain(entity, meta)
    }

    override suspend fun getVaultTree(overrideUri: Uri?): VaultNode.FolderNode {
        return vaultManager.getVaultTree(overrideUri)
    }

    private suspend fun clearIndex(noteId: String) {
        noteDao.deleteNoteById(noteId)
        linkDao.deleteLinksForSource(noteId)
        tagDao.deleteTagsForNote(noteId)
        zettelMetadataDao.deleteMetadataForNote(noteId)
        noteFtsDao.deleteFtsForNote(noteId)
    }

    override suspend fun syncVaultFilesToDb(overrideUri: Uri?) = withContext(Dispatchers.IO) {
        val notesFromFiles = vaultManager.readAllNotes(overrideUri)
        val validIds = notesFromFiles.map { it.id }.toSet()

        val allDbNotes = noteDao.getAllNotesOnce()
        for (dbNote in allDbNotes) {
            if (dbNote.id !in validIds) {
                clearIndex(dbNote.id)
            }
        }

        if (notesFromFiles.isEmpty()) return@withContext

        // Resolve all wikilink targets in one query so per-note indexing
        // doesn't do N title lookups each (previously O(notes x links)).
        val allTargets = notesFromFiles.asSequence().flatMap { note ->
            try {
                MdNativeHelper.extractWikiLinks(note.content).asSequence()
            } catch (e: Exception) {
                emptySequence()
            }
        }.distinct().toList()
        val globalTitleMap = if (allTargets.isEmpty()) {
            emptyMap()
        } else {
            try {
                noteDao.getNotesByTitles(allTargets).associateBy { it.title }
            } catch (e: Exception) {
                emptyMap()
            }
        }

        // Skip byte-identical notes: vault open re-syncs often, and unchanged
        // notes cost 5+ DAO writes each for zero benefit. Exception: a note
        // whose outgoing wikilink now resolves to a newly added note must be
        // re-indexed so isResolved flips.
        val dbById = allDbNotes.associateBy { it.id }
        val newTitles = notesFromFiles.asSequence()
            .filter { dbById[it.id] == null }
            .map { note ->
                try {
                    MdNativeHelper.parseTitle(note.content).ifBlank { note.title }
                } catch (e: Exception) {
                    note.title
                }
            }.filter { it.isNotBlank() }.toSet()
        for (note in notesFromFiles) {
            val existing = dbById[note.id]
            if (existing != null &&
                existing.updatedAt == note.updatedAt &&
                existing.sizeBytes == note.sizeBytes &&
                existing.content == note.content
            ) {
                if (newTitles.isEmpty()) continue
                val outgoing = try {
                    MdNativeHelper.extractWikiLinks(note.content).toList()
                } catch (e: Exception) {
                    emptyList()
                }
                if (outgoing.none { it in newTitles }) continue
            }
            indexNoteToDb(note, globalTitleMap)
        }
    }

    override suspend fun createFolder(folderPath: String, overrideUri: Uri?): Boolean =
        withContext(Dispatchers.IO) {
            vaultManager.createFolder(folderPath, overrideUri)
        }

    override suspend fun saveNote(note: NoteItem, overrideUri: Uri?): Boolean =
        withContext(Dispatchers.IO) {
            val success = vaultManager.saveNote(note, overrideUri)
            if (success) {
                indexNoteToDb(note)
            }
            success
        }

    /**
     * Renames a note and performs Cascading WikiLink Refactoring across all notes in the Vault.
     */
    override suspend fun renameNote(
        oldNoteId: String,
        newTitle: String,
        overrideUri: Uri?
    ): Boolean = withContext(Dispatchers.IO) {
        val oldNote = getNoteById(oldNoteId) ?: return@withContext false
        val oldTitle = oldNote.title

        val newNoteId = oldNote.renamedId(newTitle)

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
            clearIndex(oldNoteId)
            indexNoteToDb(updatedNote)

            // 3. Cascading WikiLink Refactoring across all other notes in the Vault.
            // Only notes actually mentioning the old title are touched; their
            // metadata is batch-fetched once instead of per-note.
            val allNotes = noteDao.getAllNotesOnce()
            val affected = allNotes.filter {
                it.id != newNoteId && it.content.contains("[[$oldTitle")
            }
            if (affected.isNotEmpty()) {
                val metaMap = try {
                    zettelMetadataDao.getMetadataForNotes(affected.map { it.id })
                        .associateBy { it.noteId }
                } catch (e: Exception) {
                    emptyMap()
                }
                // After rename, [[oldTitle]] resolves to the renamed note.
                val titleMap = try {
                    noteDao.getNotesByTitles(listOf(newTitle)).associateBy { it.title }
                } catch (e: Exception) {
                    emptyMap()
                }
                for (noteEntity in affected) {
                    val refactoredContent = noteEntity.content
                        .replace("[[$oldTitle]]", "[[$newTitle]]")
                        .replace("[[$oldTitle|", "[[$newTitle|")
                        .replace("[[$oldTitle#", "[[$newTitle#")

                    val refactoredNote =
                        entityToDomain(noteEntity, metaMap[noteEntity.id])
                            .copy(content = refactoredContent)
                    vaultManager.saveNote(refactoredNote, overrideUri)
                    indexNoteToDb(refactoredNote, titleMap)
                }
            }
            return@withContext true
        }

        return@withContext false
    }

    override suspend fun moveNote(
        oldRelativePath: String,
        destFolderPath: String,
        overrideUri: Uri?
    ): Boolean = withContext(Dispatchers.IO) {
        val fileName = oldRelativePath.substringAfterLast("/")
        val newRelativePath =
            if (destFolderPath.isBlank()) fileName else "$destFolderPath/$fileName"

        val noteEntity = noteDao.getNoteById(oldRelativePath)
        val movedOnDisk = vaultManager.moveFile(oldRelativePath, destFolderPath, overrideUri)

        if (movedOnDisk && noteEntity != null) {
            clearIndex(oldRelativePath)

            val meta = try {
                zettelMetadataDao.getMetadataForNote(oldRelativePath)
            } catch (e: Exception) {
                null
            }
            val updatedNote = entityToDomain(noteEntity, meta).copy(
                id = newRelativePath,
                relativePath = destFolderPath,
                updatedAt = System.currentTimeMillis()
            )
            indexNoteToDb(updatedNote)
            return@withContext true
        }
        return@withContext movedOnDisk
    }

    override suspend fun moveFolder(
        oldRelativePath: String,
        destFolderPath: String,
        overrideUri: Uri?
    ): Boolean = withContext(Dispatchers.IO) {
        val movedOnDisk = vaultManager.moveFolder(oldRelativePath, destFolderPath, overrideUri)
        if (movedOnDisk) {
            syncVaultFilesToDb(overrideUri)
        }
        movedOnDisk
    }

    override suspend fun deleteNote(relativePath: String, overrideUri: Uri?): Boolean =
        withContext(Dispatchers.IO) {
            val success = vaultManager.deleteNote(relativePath, overrideUri)
            // Single-note delete: drop its index rows directly. A full vault
            // re-sync here would re-read every file (O(vault)) for no benefit.
            clearIndex(relativePath)
            return@withContext success
        }

    override suspend fun deleteFolder(folderPath: String, overrideUri: Uri?): Boolean =
        withContext(Dispatchers.IO) {
            if (folderPath.isBlank()) return@withContext false
            val success = vaultManager.deleteFolder(folderPath, overrideUri)

            val allDbNotes = noteDao.getAllNotesOnce()
            for (dbNote in allDbNotes) {
                if (dbNote.relativePath == folderPath ||
                    dbNote.relativePath.startsWith("$folderPath/") ||
                    dbNote.id.startsWith("$folderPath/")
                ) {
                    clearIndex(dbNote.id)
                }
            }

            syncVaultFilesToDb(overrideUri)
            return@withContext success
        }

    override suspend fun searchNotes(query: String): Flow<List<NoteItem>> {
        // Escape LIKE wildcards so a literal `%`/`_` in the query can't
        // over-match (the DAO pattern uses ESCAPE '\').
        val likeFallback = noteDao.searchNotesByTitle(FtsQueryBuilder.escapeLike(query)).map { entities ->
            entitiesToDomain(entities)
        }
        val ftsQuery = FtsQueryBuilder.toFtsMatchQuery(query) ?: return likeFallback
        return noteDao.searchNotesFts(ftsQuery).map { entities ->
            entitiesToDomain(entities)
        }.catch { emitAll(likeFallback) }
    }

    private suspend fun entitiesToDomain(entities: List<NoteEntity>): List<NoteItem> {
        if (entities.isEmpty()) return emptyList()
        val metaMap = try {
            zettelMetadataDao.getMetadataForNotes(entities.map { it.id })
                .associateBy { it.noteId }
        } catch (e: Exception) {
            emptyMap()
        }
        return entities.map { entityToDomain(it, metaMap[it.id]) }
    }

    internal fun escapeLikeQuery(query: String): String = FtsQueryBuilder.escapeLike(query)

    override suspend fun getCustomTemplateNames(overrideUri: Uri?): List<String> =
        withContext(Dispatchers.IO) {
            vaultManager.listTemplateNames(overrideUri)
        }

    override suspend fun getCustomTemplateContent(
        name: String,
        overrideUri: Uri?
    ): String? = withContext(Dispatchers.IO) {
        vaultManager.readTemplate(name, overrideUri)
    }

    override suspend fun saveCustomTemplate(
        name: String,
        content: String,
        overrideUri: Uri?
    ): Boolean = withContext(Dispatchers.IO) {
        vaultManager.saveTemplate(name, content, overrideUri)
    }

    override suspend fun deleteCustomTemplate(
        name: String,
        overrideUri: Uri?
    ): Boolean = withContext(Dispatchers.IO) {
        vaultManager.deleteTemplate(name, overrideUri)
    }

    private suspend fun indexNoteToDb(
        note: NoteItem,
        titleMap: Map<String, NoteEntity>? = null
    ) {
        val parsedTitle = try {
            MdNativeHelper.parseTitle(note.content).ifBlank { note.title }
        } catch (e: Exception) {
            note.title
        }

        val extractedLinks = try {
            MdNativeHelper.extractWikiLinks(note.content).toList()
        } catch (e: Exception) {
            emptyList()
        }

        val extractedTags = try {
            MdNativeHelper.extractTags(note.content).toList()
        } catch (e: Exception) {
            emptyList()
        }

        // Frontmatter `tags: [...]` never contains `#`, so body extraction
        // misses it — merge both sources so tag index and FTS stay complete.
        val frontmatterTags = try {
            MdNativeHelper.parseFrontmatterTags(note.content)
        } catch (e: Exception) {
            emptyList()
        }
        val allTags = (extractedTags + frontmatterTags).distinct()

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

        // Update links index. Reuse the caller-provided title map when syncing
        // the whole vault (1 query total); otherwise resolve this note's
        // targets in a single IN query instead of one per link.
        val resolvedMap: Map<String, NoteEntity> = titleMap
            ?: if (extractedLinks.isEmpty()) {
                emptyMap()
            } else {
                try {
                    noteDao.getNotesByTitles(extractedLinks.distinct())
                        .associateBy { it.title }
                } catch (e: Exception) {
                    emptyMap()
                }
            }
        linkDao.deleteLinksForSource(note.id)
        val linkEntities = extractedLinks.map { targetTitle ->
            val targetEntity = resolvedMap[targetTitle]
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
        val tagEntities = allTags.map { tag ->
            TagEntity(
                noteId = note.id,
                tagName = tag
            )
        }
        if (tagEntities.isNotEmpty()) {
            tagDao.insertTags(tagEntities)
        }

        // Update full-text search index atomically (delete + insert in one
        // transaction so a crash can't leave the note unsearchable).
        noteFtsDao.replaceFtsForNote(
            note.id,
            NoteFtsEntity(
                rowid = 0,
                noteId = note.id,
                title = parsedTitle,
                content = note.content,
                tags = allTags.joinToString(" ")
            )
        )
    }

    private fun entityToDomain(
        entity: NoteEntity,
        meta: ZettelMetadataEntity?
    ): NoteItem {
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

        // Mood stamps live in YAML frontmatter (`emotion:` / `actions:`) written at
        // creation time — rehydrate them on every read so they survive DB round-trips.
        val emotion = StampParser.parseEmotion(entity.content)
        val actions = StampParser.parseActions(entity.content)

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
            paraCategory = paraCategory,
            emotion = emotion,
            actions = actions
        )
    }
}
