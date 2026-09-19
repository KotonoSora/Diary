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

class VaultManager(
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
    suspend fun getVaultTree(overrideUri: Uri? = null): VaultNode.FolderNode =
        withContext(Dispatchers.IO) {
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
            } else if (file.extension.equals(
                    "md",
                    ignoreCase = true
                ) || file.extension.equals("txt", ignoreCase = true)
            ) {
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

    private fun scanDocumentDirectory(
        dir: DocumentFile,
        relativePath: String
    ): VaultNode.FolderNode {
        val children = mutableListOf<VaultNode>()
        val files = dir.listFiles()

        for (file in files) {
            val name = file.name ?: continue
            val childPath = if (relativePath.isEmpty()) name else "$relativePath/$name"
            if (file.isDirectory) {
                if (name != "_assets" && !name.startsWith(".")) {
                    children.add(scanDocumentDirectory(file, childPath))
                }
            } else if (name.endsWith(".md", ignoreCase = true) || name.endsWith(
                    ".txt",
                    ignoreCase = true
                )
            ) {
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
    suspend fun readAllNotes(overrideUri: Uri? = null): List<NoteItem> =
        withContext(Dispatchers.IO) {
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

    private fun readLocalNotesRecursively(
        dir: File,
        relativePath: String,
        result: MutableList<NoteItem>
    ) {
        val files = dir.listFiles() ?: return
        for (file in files) {
            val childPath = if (relativePath.isEmpty()) file.name else "$relativePath/${file.name}"
            if (file.isDirectory) {
                if (file.name != "_assets" && !file.name.startsWith(".")) {
                    readLocalNotesRecursively(file, childPath, result)
                }
            } else if (file.extension.equals(
                    "md",
                    ignoreCase = true
                ) || file.extension.equals("txt", ignoreCase = true)
            ) {
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

    private fun readDocumentNotesRecursively(
        dir: DocumentFile,
        relativePath: String,
        result: MutableList<NoteItem>
    ) {
        for (file in dir.listFiles()) {
            val name = file.name ?: continue
            val childPath = if (relativePath.isEmpty()) name else "$relativePath/$name"
            if (file.isDirectory) {
                if (name != "_assets" && !name.startsWith(".")) {
                    readDocumentNotesRecursively(file, childPath, result)
                }
            } else if (name.endsWith(".md", ignoreCase = true) || name.endsWith(
                    ".txt",
                    ignoreCase = true
                )
            ) {
                val content = context.contentResolver.openInputStream(file.uri)?.bufferedReader()
                    ?.use { it.readText() } ?: ""
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
    suspend fun saveNote(note: NoteItem, overrideUri: Uri? = null): Boolean =
        withContext(Dispatchers.IO) {
            val resolvedUri = resolveVaultUri(overrideUri)

            if (resolvedUri != null) {
                try {
                    val rootDocument = DocumentFile.fromTreeUri(context, resolvedUri)
                    if (rootDocument != null && rootDocument.canWrite()) {
                        val targetFile = findOrCreateDocumentByRelativePath(
                            rootDocument,
                            note.id,
                            note.fileFormat
                        )
                        if (targetFile != null) {
                            context.contentResolver.openOutputStream(targetFile.uri, "wt")
                                ?.use { stream ->
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
     * Renames a note file locally or via SAF DocumentFile.
     */
    suspend fun renameNote(
        oldRelativePath: String,
        newRelativePath: String,
        overrideUri: Uri? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val resolvedUri = resolveVaultUri(overrideUri)

        if (resolvedUri != null) {
            try {
                val rootDocument = DocumentFile.fromTreeUri(context, resolvedUri)
                if (rootDocument != null) {
                    val targetFile = findDocumentByRelativePath(rootDocument, oldRelativePath)
                    val newFileName = newRelativePath.substringAfterLast("/")
                    return@withContext targetFile?.renameTo(newFileName) ?: false
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val oldFile = File(getDefaultStorageDir(), oldRelativePath)
        val newFile = File(getDefaultStorageDir(), newRelativePath)
        if (oldFile.exists()) {
            newFile.parentFile?.mkdirs()
            return@withContext oldFile.renameTo(newFile)
        }
        return@withContext false
    }

    /**
     * Deletes a note file.
     */
    suspend fun deleteNote(relativePath: String, overrideUri: Uri? = null): Boolean =
        withContext(Dispatchers.IO) {
            var deleted = false
            val resolvedUri = resolveVaultUri(overrideUri)

            if (resolvedUri != null) {
                try {
                    val rootDocument = DocumentFile.fromTreeUri(context, resolvedUri)
                    if (rootDocument != null) {
                        val targetFile = findDocumentByRelativePath(rootDocument, relativePath)
                        if (targetFile != null && targetFile.exists()) {
                            deleted = targetFile.delete()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val localFile = File(getDefaultStorageDir(), relativePath)
            if (localFile.exists()) {
                val localDeleted = localFile.delete()
                deleted = deleted || localDeleted
            }

            return@withContext deleted
        }

    /**
     * Deletes a folder directory recursively.
     */
    suspend fun deleteFolder(folderPath: String, overrideUri: Uri? = null): Boolean =
        withContext(Dispatchers.IO) {
            if (folderPath.isBlank()) return@withContext false
            var deleted = false

            val resolvedUri = resolveVaultUri(overrideUri)
            if (resolvedUri != null) {
                try {
                    val rootDocument = DocumentFile.fromTreeUri(context, resolvedUri)
                    if (rootDocument != null) {
                        val targetDir = findDocumentByRelativePath(rootDocument, folderPath)
                        if (targetDir != null && targetDir.isDirectory) {
                            deleted = targetDir.delete()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            try {
                val localDir = File(getDefaultStorageDir(), folderPath)
                if (localDir.exists() && localDir.isDirectory) {
                    val localDeleted = localDir.deleteRecursively()
                    deleted = deleted || localDeleted
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return@withContext deleted
        }

    /**
     * Creates a folder directory in local storage or custom SAF vault.
     */
    suspend fun createFolder(folderPath: String, overrideUri: Uri? = null): Boolean =
        withContext(Dispatchers.IO) {
            val resolvedUri = resolveVaultUri(overrideUri)

            if (resolvedUri != null) {
                try {
                    val rootDocument = DocumentFile.fromTreeUri(context, resolvedUri)
                    if (rootDocument != null) {
                        val created = findOrCreateDirByRelativePath(rootDocument, folderPath)
                        return@withContext created != null
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            try {
                val dir = File(getDefaultStorageDir(), folderPath)
                return@withContext dir.mkdirs() || dir.exists()
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext false
            }
        }

    /**
     * Moves a file to a new destination folder.
     */
    suspend fun moveFile(
        oldRelativePath: String,
        destFolderPath: String,
        overrideUri: Uri? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val fileName = oldRelativePath.substringAfterLast("/")
        val newRelativePath =
            if (destFolderPath.isBlank()) fileName else "$destFolderPath/$fileName"
        if (oldRelativePath == newRelativePath) return@withContext true

        val resolvedUri = resolveVaultUri(overrideUri)
        if (resolvedUri != null) {
            try {
                val rootDocument = DocumentFile.fromTreeUri(context, resolvedUri)
                if (rootDocument != null) {
                    val srcFile = findDocumentByRelativePath(rootDocument, oldRelativePath)
                    if (srcFile != null) {
                        val destDir =
                            if (destFolderPath.isBlank()) rootDocument else findOrCreateDirByRelativePath(
                                rootDocument,
                                destFolderPath
                            )
                        if (destDir != null) {
                            val existingTarget = destDir.findFile(fileName)
                            if (existingTarget != null && existingTarget.isFile) {
                                existingTarget.delete()
                            }
                            val mimeType =
                                if (fileName.endsWith(
                                        ".txt",
                                        ignoreCase = true
                                    )
                                ) "text/plain" else "text/markdown"
                            val newDoc = destDir.createFile(mimeType, fileName)
                            if (newDoc != null) {
                                context.contentResolver.openInputStream(srcFile.uri)?.use { input ->
                                    context.contentResolver.openOutputStream(newDoc.uri, "wt")
                                        ?.use { output ->
                                            input.copyTo(output)
                                        }
                                }
                                srcFile.delete()
                                return@withContext true
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        try {
            val srcFile = File(getDefaultStorageDir(), oldRelativePath)
            val destFile = File(getDefaultStorageDir(), newRelativePath)
            if (srcFile.exists()) {
                destFile.parentFile?.mkdirs()
                return@withContext srcFile.renameTo(destFile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext false
    }

    /**
     * Moves a folder directory to a new destination folder.
     */
    suspend fun moveFolder(
        oldRelativePath: String,
        destFolderPath: String,
        overrideUri: Uri? = null
    ): Boolean = withContext(Dispatchers.IO) {
        if (oldRelativePath.isBlank()) return@withContext false
        if (destFolderPath == oldRelativePath || destFolderPath.startsWith("$oldRelativePath/")) {
            return@withContext false
        }

        val folderName = oldRelativePath.substringAfterLast("/")
        val newRelativePath =
            if (destFolderPath.isBlank()) folderName else "$destFolderPath/$folderName"
        if (oldRelativePath == newRelativePath) return@withContext true

        val resolvedUri = resolveVaultUri(overrideUri)
        if (resolvedUri != null) {
            try {
                val rootDocument = DocumentFile.fromTreeUri(context, resolvedUri)
                if (rootDocument != null) {
                    val srcDir = findDocumentByRelativePath(rootDocument, oldRelativePath)
                    if (srcDir != null) {
                        val targetParent =
                            if (destFolderPath.isBlank()) rootDocument else findOrCreateDirByRelativePath(
                                rootDocument,
                                destFolderPath
                            )
                        if (targetParent != null) {
                            return@withContext moveDocumentDirectoryRecursively(
                                srcDir,
                                targetParent,
                                folderName
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        try {
            val srcDir = File(getDefaultStorageDir(), oldRelativePath)
            val destDir = File(getDefaultStorageDir(), newRelativePath)
            if (srcDir.exists() && srcDir.isDirectory) {
                destDir.parentFile?.mkdirs()
                return@withContext srcDir.renameTo(destDir)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext false
    }

    private fun findOrCreateDirByRelativePath(
        root: DocumentFile,
        relativePath: String
    ): DocumentFile? {
        if (relativePath.isBlank()) return root
        val segments = relativePath.split("/").filter { it.isNotBlank() }
        var currentDir = root

        for (segment in segments) {
            val subDir = currentDir.findFile(segment)
            currentDir = if (subDir != null && subDir.isDirectory) {
                subDir
            } else {
                currentDir.createDirectory(segment) ?: return null
            }
        }
        return currentDir
    }

    private fun moveDocumentDirectoryRecursively(
        srcDir: DocumentFile,
        targetParentDir: DocumentFile,
        newFolderName: String
    ): Boolean {
        val existing = targetParentDir.findFile(newFolderName)
        val destDir =
            if (existing != null && existing.isDirectory) existing else targetParentDir.createDirectory(
                newFolderName
            ) ?: return false

        for (file in srcDir.listFiles()) {
            val name = file.name ?: continue
            if (file.isDirectory) {
                moveDocumentDirectoryRecursively(file, destDir, name)
            } else {
                val mimeType = file.type ?: "text/markdown"
                val newFile = destDir.createFile(mimeType, name)
                if (newFile != null) {
                    context.contentResolver.openInputStream(file.uri)?.use { input ->
                        context.contentResolver.openOutputStream(newFile.uri, "wt")?.use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
            file.delete()
        }
        srcDir.delete()
        return true
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

        val mimeType =
            if (fileFormat.equals("txt", ignoreCase = true)) "text/plain" else "text/markdown"
        return currentDir.createFile(mimeType, fileName)
    }

    private fun findDocumentByRelativePath(
        root: DocumentFile,
        relativePath: String
    ): DocumentFile? {
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
