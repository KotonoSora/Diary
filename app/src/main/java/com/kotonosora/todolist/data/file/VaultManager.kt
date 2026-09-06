package com.kotonosora.todolist.data.file

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.documentfile.provider.DocumentFile
import com.kotonosora.todolist.data.repository.UserPreferencesRepository
import com.kotonosora.todolist.domain.model.NoteItem
import com.kotonosora.todolist.domain.model.VaultNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultManager @Inject constructor(
    private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository? = null
) {

    fun getDefaultStorageDir(): File {
        return (context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: context.filesDir).also { it.mkdirs() }
    }

    suspend fun resolveVaultUri(overrideUri: Uri? = null): Uri? {
        if (overrideUri != null) return overrideUri
        val savedUriStr = try {
            userPreferencesRepository?.customStorageFolderUri?.firstOrNull()
        } catch (e: Exception) {
            null
        }
        return if (!savedUriStr.isNullOrBlank()) Uri.parse(savedUriStr) else null
    }

    /**
     * Recursively scans the vault directory to build a VaultNode tree.
     */
    suspend fun getVaultTree(overrideUri: Uri? = null): VaultNode.FolderNode = withContext(Dispatchers.IO) {
        val resolvedUri = resolveVaultUri(overrideUri)

        if (resolvedUri != null) {
            try {
                val rootDocument = DocumentFile.fromTreeUri(context, resolvedUri)
                if (rootDocument != null && rootDocument.canRead()) {
                    return@withContext scanDocumentDirectory(rootDocument, "")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Fallback to local file directory
        val rootDir = getDefaultStorageDir()
        return@withContext scanLocalDirectory(rootDir, "")
    }

    private fun scanLocalDirectory(dir: File, relativePath: String): VaultNode.FolderNode {
        val children = mutableListOf<VaultNode>()
        val files = dir.listFiles() ?: emptyArray()

        for (file in files) {
            val childPath = if (relativePath.isEmpty()) file.name else "$relativePath/${file.name}"
            if (file.isDirectory) {
                if (file.name != "_assets" && !file.name.startsWith(".")) {
                    children.add(scanLocalDirectory(file, childPath))
                }
            } else if (file.extension.equals("md", ignoreCase = true) || file.extension.equals("txt", ignoreCase = true)) {
                children.add(
                    VaultNode.FileNode(
                        name = file.name,
                        relativePath = childPath,
                        extension = file.extension,
                        sizeBytes = file.length(),
                        updatedAt = file.lastModified()
                    )
                )
            }
        }

        return VaultNode.FolderNode(
            name = dir.name,
            relativePath = relativePath,
            children = children
        )
    }

    private fun scanDocumentDirectory(dir: DocumentFile, relativePath: String): VaultNode.FolderNode {
        val children = mutableListOf<VaultNode>()
        val files = dir.listFiles()

        for (file in files) {
            val name = file.name ?: continue
            val childPath = if (relativePath.isEmpty()) name else "$relativePath/$name"
            if (file.isDirectory) {
                if (name != "_assets" && !name.startsWith(".")) {
                    children.add(scanDocumentDirectory(file, childPath))
                }
            } else if (name.endsWith(".md", ignoreCase = true) || name.endsWith(".txt", ignoreCase = true)) {
                val extension = name.substringAfterLast(".", "md")
                children.add(
                    VaultNode.FileNode(
                        name = name,
                        relativePath = childPath,
                        extension = extension,
                        sizeBytes = file.length(),
                        updatedAt = file.lastModified()
                    )
                )
            }
        }

        return VaultNode.FolderNode(
            name = dir.name ?: "Vault",
            relativePath = relativePath,
            children = children
        )
    }

    /**
     * Reads all notes from the vault directory.
     */
    suspend fun readAllNotes(overrideUri: Uri? = null): List<NoteItem> = withContext(Dispatchers.IO) {
        val notes = mutableListOf<NoteItem>()
        val resolvedUri = resolveVaultUri(overrideUri)

        if (resolvedUri != null) {
            try {
                val rootDocument = DocumentFile.fromTreeUri(context, resolvedUri)
                if (rootDocument != null && rootDocument.canRead()) {
                    readDocumentNotesRecursively(rootDocument, "", notes)
                    return@withContext notes
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val rootDir = getDefaultStorageDir()
        readLocalNotesRecursively(rootDir, "", notes)
        return@withContext notes
    }

    private fun readLocalNotesRecursively(dir: File, relativePath: String, result: MutableList<NoteItem>) {
        val files = dir.listFiles() ?: return
        for (file in files) {
            val childPath = if (relativePath.isEmpty()) file.name else "$relativePath/${file.name}"
            if (file.isDirectory) {
                if (file.name != "_assets" && !file.name.startsWith(".")) {
                    readLocalNotesRecursively(file, childPath, result)
                }
            } else if (file.extension.equals("md", ignoreCase = true) || file.extension.equals("txt", ignoreCase = true)) {
                val content = file.readText()
                val title = extractTitleFromContent(content, file.nameWithoutExtension)
                result.add(
                    NoteItem(
                        id = childPath,
                        title = title,
                        relativePath = relativePath,
                        content = content,
                        fileFormat = file.extension,
                        updatedAt = file.lastModified(),
                        sizeBytes = file.length()
                    )
                )
            }
        }
    }

    private fun readDocumentNotesRecursively(dir: DocumentFile, relativePath: String, result: MutableList<NoteItem>) {
        for (file in dir.listFiles()) {
            val name = file.name ?: continue
            val childPath = if (relativePath.isEmpty()) name else "$relativePath/$name"
            if (file.isDirectory) {
                if (name != "_assets" && !name.startsWith(".")) {
                    readDocumentNotesRecursively(file, childPath, result)
                }
            } else if (name.endsWith(".md", ignoreCase = true) || name.endsWith(".txt", ignoreCase = true)) {
                val content = context.contentResolver.openInputStream(file.uri)?.bufferedReader()?.use { it.readText() } ?: ""
                val extension = name.substringAfterLast(".", "md")
                val title = extractTitleFromContent(content, name.substringBeforeLast("."))
                result.add(
                    NoteItem(
                        id = childPath,
                        title = title,
                        relativePath = relativePath,
                        content = content,
                        fileFormat = extension,
                        updatedAt = file.lastModified(),
                        sizeBytes = file.length()
                    )
                )
            }
        }
    }

    /**
     * Saves or creates a note in the local storage directory or custom SAF vault.
     */
    suspend fun saveNote(note: NoteItem, overrideUri: Uri? = null): Boolean = withContext(Dispatchers.IO) {
        val resolvedUri = resolveVaultUri(overrideUri)

        if (resolvedUri != null) {
            try {
                val rootDocument = DocumentFile.fromTreeUri(context, resolvedUri)
                if (rootDocument != null && rootDocument.canWrite()) {
                    val targetFile = findOrCreateDocumentByRelativePath(rootDocument, note.id, note.fileFormat)
                    if (targetFile != null) {
                        context.contentResolver.openOutputStream(targetFile.uri, "wt")?.use { stream ->
                            stream.write(note.content.toByteArray())
                        }
                        return@withContext true
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Fallback to local storage dir
        try {
            val file = File(getDefaultStorageDir(), note.id)
            file.parentFile?.mkdirs()
            file.writeText(note.content)
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    /**
     * Deletes a note file.
     */
    suspend fun deleteNote(relativePath: String, overrideUri: Uri? = null): Boolean = withContext(Dispatchers.IO) {
        val resolvedUri = resolveVaultUri(overrideUri)

        if (resolvedUri != null) {
            try {
                val rootDocument = DocumentFile.fromTreeUri(context, resolvedUri)
                if (rootDocument != null) {
                    val targetFile = findDocumentByRelativePath(rootDocument, relativePath)
                    return@withContext targetFile?.delete() ?: false
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val file = File(getDefaultStorageDir(), relativePath)
        return@withContext if (file.exists()) file.delete() else false
    }

    private fun findOrCreateDocumentByRelativePath(
        root: DocumentFile,
        relativePath: String,
        fileFormat: String
    ): DocumentFile? {
        val segments = relativePath.split("/")
        var currentDir = root

        for (i in 0 until segments.size - 1) {
            val dirName = segments[i]
            val subDir = currentDir.findFile(dirName)
            currentDir = if (subDir != null && subDir.isDirectory) {
                subDir
            } else {
                currentDir.createDirectory(dirName) ?: return null
            }
        }

        val fileName = segments.last()
        val existing = currentDir.findFile(fileName)
        if (existing != null && existing.isFile) return existing

        val mimeType = if (fileFormat.equals("txt", ignoreCase = true)) "text/plain" else "text/markdown"
        return currentDir.createFile(mimeType, fileName)
    }

    private fun findDocumentByRelativePath(root: DocumentFile, relativePath: String): DocumentFile? {
        val segments = relativePath.split("/")
        var current = root

        for (i in 0 until segments.size - 1) {
            val dirName = segments[i]
            current = current.findFile(dirName) ?: return null
            if (!current.isDirectory) return null
        }

        return current.findFile(segments.last())
    }

    private fun extractTitleFromContent(content: String, fallback: String): String {
        val firstLine = content.lines().firstOrNull { it.isNotBlank() } ?: return fallback
        return if (firstLine.startsWith("# ")) {
            firstLine.removePrefix("# ").trim()
        } else {
            fallback
        }
    }
}
