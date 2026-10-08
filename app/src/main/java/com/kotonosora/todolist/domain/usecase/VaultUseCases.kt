package com.kotonosora.todolist.domain.usecase

import android.net.Uri
import com.kotonosora.todolist.domain.model.ActionStamp
import com.kotonosora.todolist.domain.model.EmotionStamp
import com.kotonosora.todolist.domain.model.NoteItem
import com.kotonosora.todolist.domain.model.NoteType
import com.kotonosora.todolist.domain.model.StampParser
import com.kotonosora.todolist.domain.model.VaultNode
import com.kotonosora.todolist.domain.model.ZettelUidGenerator
import com.kotonosora.todolist.domain.model.generateZettelContent
import com.kotonosora.todolist.domain.repository.VaultRepository
import kotlinx.coroutines.flow.Flow

/**
 * Vault/notes bounded context — application layer (DDD).
 *
 * Covers notes, folders, vault-tree sync, full-text search, links/tags and
 * custom templates. ViewModels depend on [VaultUseCases], never on
 * [VaultRepository] directly. Mirrors the [TaskUseCases] bundle pattern.
 */

class ObserveNotesUseCase(private val repository: VaultRepository) {
    operator fun invoke(): Flow<List<NoteItem>> = repository.getAllNotes()
}

class GetNoteByIdUseCase(private val repository: VaultRepository) {
    suspend operator fun invoke(id: String): NoteItem? {
        if (id.isBlank()) return null
        return repository.getNoteById(id)
    }
}

class GetVaultTreeUseCase(private val repository: VaultRepository) {
    suspend operator fun invoke(overrideUri: Uri? = null): VaultNode.FolderNode =
        repository.getVaultTree(overrideUri)
}

class SyncVaultUseCase(private val repository: VaultRepository) {
    suspend operator fun invoke(overrideUri: Uri? = null) =
        repository.syncVaultFilesToDb(overrideUri)
}

class CreateVaultFolderUseCase(private val repository: VaultRepository) {
    suspend operator fun invoke(folderPath: String, overrideUri: Uri? = null): Boolean {
        return folderPath.isNotBlank() && repository.createFolder(folderPath, overrideUri)
    }
}

class SaveNoteUseCase(private val repository: VaultRepository) {
    suspend operator fun invoke(note: NoteItem, overrideUri: Uri? = null): Boolean {
        return note.id.isNotBlank() && repository.saveNote(note, overrideUri)
    }
}

class RenameNoteUseCase(private val repository: VaultRepository) {
    suspend operator fun invoke(
        oldNoteId: String,
        newTitle: String,
        overrideUri: Uri? = null
    ): Boolean {
        return !(oldNoteId.isBlank() || newTitle.isBlank()) && repository.renameNote(oldNoteId, newTitle, overrideUri)
    }
}

class MoveNoteUseCase(private val repository: VaultRepository) {
    suspend operator fun invoke(
        oldRelativePath: String,
        destFolderPath: String,
        overrideUri: Uri? = null
    ): Boolean {
        return oldRelativePath.isNotBlank() && repository.moveNote(oldRelativePath, destFolderPath, overrideUri)
    }
}

class MoveFolderUseCase(private val repository: VaultRepository) {
    suspend operator fun invoke(
        oldRelativePath: String,
        destFolderPath: String,
        overrideUri: Uri? = null
    ): Boolean {
        return oldRelativePath.isNotBlank() && repository.moveFolder(oldRelativePath, destFolderPath, overrideUri)
    }
}

class DeleteNoteUseCase(private val repository: VaultRepository) {
    suspend operator fun invoke(relativePath: String, overrideUri: Uri? = null): Boolean {
        return relativePath.isNotBlank() && repository.deleteNote(relativePath, overrideUri)
    }
}

class DeleteFolderUseCase(private val repository: VaultRepository) {
    suspend operator fun invoke(folderPath: String, overrideUri: Uri? = null): Boolean {
        return folderPath.isNotBlank() && repository.deleteFolder(folderPath, overrideUri)
    }
}

class SearchNotesUseCase(private val repository: VaultRepository) {
    suspend operator fun invoke(query: String): Flow<List<NoteItem>> =
        repository.searchNotes(query)
}

class GetOutgoingLinksUseCase(private val repository: VaultRepository) {
    operator fun invoke(sourceNoteId: String): Flow<List<String>> =
        repository.getOutgoingLinks(sourceNoteId)
}

class GetIncomingLinksUseCase(private val repository: VaultRepository) {
    operator fun invoke(targetTitle: String): Flow<List<String>> =
        repository.getIncomingLinks(targetTitle)
}

class ObserveTagsUseCase(private val repository: VaultRepository) {
    operator fun invoke(): Flow<List<String>> = repository.getAllTags()
}

class GetTemplateNamesUseCase(private val repository: VaultRepository) {
    suspend operator fun invoke(overrideUri: Uri? = null): List<String> =
        repository.getCustomTemplateNames(overrideUri)
}

class GetTemplateContentUseCase(private val repository: VaultRepository) {
    suspend operator fun invoke(name: String, overrideUri: Uri? = null): String? {
        if (name.isBlank()) return null
        return repository.getCustomTemplateContent(name, overrideUri)
    }
}

class SaveTemplateUseCase(private val repository: VaultRepository) {
    suspend operator fun invoke(name: String, content: String, overrideUri: Uri? = null): Boolean {
        return !(name.isBlank() || content.isBlank()) && repository.saveCustomTemplate(name, content, overrideUri)
    }
}

class DeleteTemplateUseCase(private val repository: VaultRepository) {
    suspend operator fun invoke(name: String, overrideUri: Uri? = null): Boolean {
        return name.isNotBlank() && repository.deleteCustomTemplate(name, overrideUri)
    }
}

/**
 * Creates a zettel note (`<uid>-<title>.md`) with templated content in one
 * step. Owns UID generation, filename composition and [NoteItem] assembly —
 * previously inline in `VaultViewModel`.
 *
 * @return the vault-relative path of the created note, or null when the
 * title is blank or the save failed.
 */
class CreateZettelNoteUseCase(private val repository: VaultRepository) {
    suspend operator fun invoke(
        folderPath: String,
        title: String,
        noteType: NoteType,
        author: String? = null,
        sourceUrl: String? = null,
        emotion: EmotionStamp? = null,
        actions: List<ActionStamp> = emptyList(),
        overrideUri: Uri? = null
    ): String? {
        if (title.isBlank()) return null
        val uid = ZettelUidGenerator.generateUid()
        val filename = "$uid-$title.md"
        val relativePath = if (folderPath.isBlank()) filename else "$folderPath/$filename"
        val content = generateZettelContent(
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
        return if (repository.saveNote(note, overrideUri)) relativePath else null
    }
}

/**
 * Mood-stamp CRUD on an existing note: rewrites the frontmatter `emotion:` /
 * `actions:` lines (via [StampParser.withStamps]) and saves. Clearing both
 * removes the stamp lines without touching anything else.
 */
class UpdateNoteStampsUseCase(private val repository: VaultRepository) {
    suspend operator fun invoke(
        noteId: String,
        emotion: EmotionStamp?,
        actions: List<ActionStamp> = emptyList(),
        overrideUri: Uri? = null
    ): Boolean {
        if (noteId.isBlank()) return false
        val note = repository.getNoteById(noteId) ?: return false
        val content = StampParser.withStamps(note.content, emotion, actions)
        return repository.saveNote(
            note.copy(
                content = content,
                emotion = emotion,
                actions = actions,
                updatedAt = System.currentTimeMillis()
            ),
            overrideUri
        )
    }
}

data class VaultUseCases(
    val observeNotes: ObserveNotesUseCase,
    val getNoteById: GetNoteByIdUseCase,
    val getVaultTree: GetVaultTreeUseCase,
    val syncVault: SyncVaultUseCase,
    val createFolder: CreateVaultFolderUseCase,
    val saveNote: SaveNoteUseCase,
    val renameNote: RenameNoteUseCase,
    val moveNote: MoveNoteUseCase,
    val moveFolder: MoveFolderUseCase,
    val deleteNote: DeleteNoteUseCase,
    val deleteFolder: DeleteFolderUseCase,
    val searchNotes: SearchNotesUseCase,
    val outgoingLinks: GetOutgoingLinksUseCase,
    val incomingLinks: GetIncomingLinksUseCase,
    val observeTags: ObserveTagsUseCase,
    val templateNames: GetTemplateNamesUseCase,
    val templateContent: GetTemplateContentUseCase,
    val saveTemplate: SaveTemplateUseCase,
    val deleteTemplate: DeleteTemplateUseCase,
    val createZettelNote: CreateZettelNoteUseCase,
    val updateNoteStamps: UpdateNoteStampsUseCase
) {
    companion object {
        fun from(repository: VaultRepository): VaultUseCases = VaultUseCases(
            observeNotes = ObserveNotesUseCase(repository),
            getNoteById = GetNoteByIdUseCase(repository),
            getVaultTree = GetVaultTreeUseCase(repository),
            syncVault = SyncVaultUseCase(repository),
            createFolder = CreateVaultFolderUseCase(repository),
            saveNote = SaveNoteUseCase(repository),
            renameNote = RenameNoteUseCase(repository),
            moveNote = MoveNoteUseCase(repository),
            moveFolder = MoveFolderUseCase(repository),
            deleteNote = DeleteNoteUseCase(repository),
            deleteFolder = DeleteFolderUseCase(repository),
            searchNotes = SearchNotesUseCase(repository),
            outgoingLinks = GetOutgoingLinksUseCase(repository),
            incomingLinks = GetIncomingLinksUseCase(repository),
            observeTags = ObserveTagsUseCase(repository),
            templateNames = GetTemplateNamesUseCase(repository),
            templateContent = GetTemplateContentUseCase(repository),
            saveTemplate = SaveTemplateUseCase(repository),
            deleteTemplate = DeleteTemplateUseCase(repository),
            createZettelNote = CreateZettelNoteUseCase(repository),
            updateNoteStamps = UpdateNoteStampsUseCase(repository)
        )
    }
}
