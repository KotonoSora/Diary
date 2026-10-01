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
import com.kotonosora.todolist.domain.model.ActionStamp
import com.kotonosora.todolist.domain.model.EmotionStamp
import com.kotonosora.todolist.domain.model.NoteItem
import com.kotonosora.todolist.domain.model.NoteType
import com.kotonosora.todolist.domain.model.ParaCategory
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
import java.util.Locale

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

    override suspend fun syncVaultFilesToDb(overrideUri: Uri?) = withContext(Dispatchers.IO) {
        val notesFromFiles = vaultManager.readAllNotes(overrideUri)
        val validIds = notesFromFiles.map { it.id }.toSet()

        val allDbNotes = noteDao.getAllNotesOnce()
        for (dbNote in allDbNotes) {
            if (dbNote.id !in validIds) {
                noteDao.deleteNoteById(dbNote.id)
                linkDao.deleteLinksForSource(dbNote.id)
                tagDao.deleteTagsForNote(dbNote.id)
                zettelMetadataDao.deleteMetadataForNote(dbNote.id)
                noteFtsDao.deleteFtsForNote(dbNote.id)
            }
        }

        if (notesFromFiles.isEmpty()) return@withContext

        // Resolve all wikilink targets in one query so per-note indexing
        // doesn't do N title lookups each (previously O(notes x links)).
        val allTargets = notesFromFiles.asSequence().flatMap { note ->
            try {
                MdNativeHelper.extractWikiLinks(note.content).asSequence()
            } catch (e: Throwable) {
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
                } catch (e: Throwable) {
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
                } catch (e: Throwable) {
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
            noteDao.deleteNoteById(oldNoteId)
            linkDao.deleteLinksForSource(oldNoteId)
            tagDao.deleteTagsForNote(oldNoteId)
            zettelMetadataDao.deleteMetadataForNote(oldNoteId)
            noteFtsDao.deleteFtsForNote(oldNoteId)
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
            noteDao.deleteNoteById(oldRelativePath)
            linkDao.deleteLinksForSource(oldRelativePath)
            tagDao.deleteTagsForNote(oldRelativePath)
            zettelMetadataDao.deleteMetadataForNote(oldRelativePath)
            noteFtsDao.deleteFtsForNote(oldRelativePath)

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
            noteDao.deleteNoteById(relativePath)
            linkDao.deleteLinksForSource(relativePath)
            tagDao.deleteTagsForNote(relativePath)
            zettelMetadataDao.deleteMetadataForNote(relativePath)
            noteFtsDao.deleteFtsForNote(relativePath)
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
                    noteDao.deleteNoteById(dbNote.id)
                    linkDao.deleteLinksForSource(dbNote.id)
                    tagDao.deleteTagsForNote(dbNote.id)
                    zettelMetadataDao.deleteMetadataForNote(dbNote.id)
                    noteFtsDao.deleteFtsForNote(dbNote.id)
                }
            }

            syncVaultFilesToDb(overrideUri)
            return@withContext success
        }

    override suspend fun searchNotes(query: String): Flow<List<NoteItem>> {
        // Title fallback needs domain mapping too — batch its metadata as well.
        suspend fun toDomain(entities: List<NoteEntity>): List<NoteItem> {
            if (entities.isEmpty()) return emptyList()
            val metaMap = try {
                zettelMetadataDao.getMetadataForNotes(entities.map { it.id })
                    .associateBy { it.noteId }
            } catch (e: Exception) {
                emptyMap()
            }
            return entities.map { entityToDomain(it, metaMap[it.id]) }
        }
        // Escape LIKE wildcards so a literal `%`/`_` in the query can't
        // over-match (the DAO pattern uses ESCAPE '\').
        val likeFallback = noteDao.searchNotesByTitle(escapeLikeQuery(query)).map { entities ->
            toDomain(entities)
        }
        val ftsQuery = toFtsMatchQuery(query) ?: return likeFallback
        return noteDao.searchNotesFts(ftsQuery).map { entities ->
            toDomain(entities)
        }.catch { emitAll(likeFallback) }
    }

    /**
     * Escapes user input for a LIKE pattern using '\' as the escape char
     * (must match the DAO's `ESCAPE '\'` clause).
     */
    internal fun escapeLikeQuery(query: String): String {
        val sb = StringBuilder(query.length)
        for (c in query) {
            if (c == '\\' || c == '%' || c == '_') sb.append('\\')
            sb.append(c)
        }
        return sb.toString()
    }

    /**
     * Builds a safe FTS4 MATCH query from raw user input.
     * Tokens are split on non-alphanumerics (FTS4 simple tokenizer splits the
     * same way, e.g. `hello-world` is indexed as `hello` + `world`) and
     * prefix-matched so as-you-type search works. Returns null when nothing
     * searchable remains. Terms are capped so a pasted paragraph can't blow
     * up the MATCH statement.
     */
    private fun toFtsMatchQuery(query: String): String? {
        val tokens = query.split("[^\\p{L}\\p{Nd}]+".toRegex())
            .map { it.trim().take(MAX_FTS_TOKEN_LEN) }
            .filter { it.isNotBlank() }
            .take(MAX_FTS_TOKENS)
        if (tokens.isEmpty()) return null
        return tokens.joinToString(" ") { "\"$it*\"" }
    }

    companion object {
        /** Max terms per FTS MATCH; beyond this extra words add noise, not signal. */
        private const val MAX_FTS_TOKENS = 12

        /** Max chars per FTS term; guards against pathological pasted input. */
        private const val MAX_FTS_TOKEN_LEN = 40
    }

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

        // Frontmatter `tags: [...]` never contains `#`, so body extraction
        // misses it — merge both sources so tag index and FTS stay complete.
        val frontmatterTags = try {
            MdNativeHelper.parseFrontmatterTags(note.content)
        } catch (e: Throwable) {
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
        val emotion = try {
            MdNativeHelper.parseEmotionName(entity.content)
                ?.let { EmotionStamp.valueOf(it.uppercase(Locale.ROOT)) }
        } catch (e: Exception) {
            null
        }
        val actions = MdNativeHelper.parseActionNames(entity.content).mapNotNull {
            try {
                ActionStamp.valueOf(it.uppercase(Locale.ROOT))
            } catch (e: Exception) {
                null
            }
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
            paraCategory = paraCategory,
            emotion = emotion,
            actions = actions
        )
    }
}
