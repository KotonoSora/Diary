package com.kotonosora.todolist.data.file

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.documentfile.provider.DocumentFile
import com.kotonosora.todolist.data.database.TodoDao
import com.kotonosora.todolist.data.database.TodoEntity
import com.kotonosora.todolist.data.repository.UserPreferencesRepository
import com.kotonosora.todolist.domain.model.TodoItem
import kotlinx.coroutines.flow.firstOrNull
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bidirectional sync between .md and .txt files in Documents (or custom folder) and the SQLite database.
 */
@Singleton
class FileSyncManager @Inject constructor(
    private val context: Context,
    private val todoDao: TodoDao,
    private val userPreferencesRepository: UserPreferencesRepository? = null
) {

    private fun getStorageDir(): File {
        return (context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: context.filesDir).also { it.mkdirs() }
    }

    private suspend fun resolveCustomFolderUri(overrideUri: Uri?): Uri? {
        if (overrideUri != null) return overrideUri
        val savedUriStr = try {
            userPreferencesRepository?.customStorageFolderUri?.firstOrNull()
        } catch (e: Exception) {
            null
        }
        return if (!savedUriStr.isNullOrBlank()) Uri.parse(savedUriStr) else null
    }

    /**
     * Reads all .md and .txt files from storage directory (or custom folder) and upserts any
     * IDs not currently in the database.
     */
    suspend fun syncFilesToDb(customFolderUri: Uri? = null) {
        val resolvedUri = resolveCustomFolderUri(customFolderUri)
        if (resolvedUri != null) {
            try {
                val treeFile = DocumentFile.fromTreeUri(context, resolvedUri)
                if (treeFile != null && treeFile.canRead()) {
                    treeFile.listFiles().forEach { doc ->
                        val name = doc.name ?: ""
                        if (name.endsWith(".md", ignoreCase = true) || name.endsWith(".txt", ignoreCase = true)) {
                            val todoId = name.substringBeforeLast(".")
                            val extension = name.substringAfterLast(".", "md")
                            val existing = todoDao.getTodoById(todoId)
                            if (existing == null) {
                                val text = context.contentResolver.openInputStream(doc.uri)
                                    ?.bufferedReader()?.readText() ?: ""
                                val parsed = if (extension.equals("txt", ignoreCase = true)) {
                                    parseTxtText(text, todoId, doc.uri.toString())
                                } else {
                                    parseMdText(text, todoId, doc.uri.toString())
                                }
                                todoDao.insertTodo(parsed)
                            }
                        }
                    }
                    return
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Fallback to local storage dir
        val dir = getStorageDir()
        val textFiles = dir.listFiles { f -> f.extension == "md" || f.extension == "txt" } ?: return

        for (file in textFiles) {
            val todoId = file.nameWithoutExtension
            val existing = todoDao.getTodoById(todoId)
            if (existing == null) {
                val parsed = if (file.extension == "txt") parseTxtFile(file, todoId) else parseMdFile(file, todoId)
                todoDao.insertTodo(parsed)
            }
        }
    }

    /**
     * Writes a file (.md or .txt) for every DB entity whose backing file is missing.
     */
    suspend fun syncDbToFiles(fileManager: TodoFileManager, customFolderUri: Uri? = null) {
        val resolvedUri = resolveCustomFolderUri(customFolderUri)
        val existingFiles = mutableSetOf<String>()

        if (resolvedUri != null) {
            try {
                val treeFile = DocumentFile.fromTreeUri(context, resolvedUri)
                treeFile?.listFiles()?.mapNotNullTo(existingFiles) { it.name }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            val dir = getStorageDir()
            dir.listFiles { f -> f.extension == "md" || f.extension == "txt" }
                ?.mapTo(existingFiles) { it.name }
        }

        val allEntities = todoDao.getAllTodosOnce()
        for (entity in allEntities) {
            val expectedFileName = "${entity.id}.${entity.fileFormat}"
            if (expectedFileName !in existingFiles) {
                val domainItem = TodoItem(
                    id = entity.id,
                    title = entity.title,
                    description = entity.description,
                    dueDate = entity.dueDate,
                    filePath = entity.filePath,
                    isCompleted = entity.isCompleted,
                    reminderTime = entity.reminderTime,
                    fileFormat = entity.fileFormat
                )
                fileManager.saveTodoToFile(domainItem, resolvedUri)
            }
        }
    }

    // ── Simple markdown parser ────────────────────────────────────────────────

    private fun parseMdFile(file: File, id: String): TodoEntity {
        return parseMdText(file.readText(), id, file.absolutePath)
    }

    private fun parseMdText(text: String, id: String, absolutePath: String): TodoEntity {
        var title = id
        var isCompleted = false
        var dueDate: Long? = null
        var reminderTime: Long? = null
        var mediaPath: String? = null
        val descriptionLines = mutableListOf<String>()
        var inDescriptionSection = false

        for (line in text.lines()) {
            when {
                line.startsWith("## Description") -> inDescriptionSection = true
                inDescriptionSection && line.startsWith("#") -> inDescriptionSection = false
                inDescriptionSection -> descriptionLines.add(line)
                line.startsWith("# ") -> title = line.removePrefix("# ").trim()
                line.contains("**Status**: Completed") -> isCompleted = true
                line.contains("**Due Date**: ") -> {
                    val dateStr = line.substringAfter("**Due Date**: ").trim()
                    dueDate = parseDateString(dateStr)
                }
                line.contains("**Reminder**: ") -> {
                    val dateStr = line.substringAfter("**Reminder**: ").trim()
                    reminderTime = parseDateString(dateStr)
                }
                line.contains("**Media**: ") -> {
                    mediaPath = line.substringAfter("**Media**: ").trim().ifBlank { null }
                }
            }
        }

        val description = descriptionLines.joinToString("\n").trim().ifBlank { null }

        return TodoEntity(
            id = id,
            title = title,
            description = description,
            dueDate = dueDate,
            filePath = mediaPath ?: absolutePath,
            isCompleted = isCompleted,
            reminderTime = reminderTime,
            fileFormat = "md"
        )
    }

    // ── Simple plain text parser ──────────────────────────────────────────────

    private fun parseTxtFile(file: File, id: String): TodoEntity {
        return parseTxtText(file.readText(), id, file.absolutePath)
    }

    private fun parseTxtText(text: String, id: String, absolutePath: String): TodoEntity {
        var title = id
        var isCompleted = false
        var dueDate: Long? = null
        var reminderTime: Long? = null
        var mediaPath: String? = null
        val descriptionLines = mutableListOf<String>()
        var inDescriptionSection = false

        for (line in text.lines()) {
            when {
                line.startsWith("DESCRIPTION:") -> inDescriptionSection = true
                inDescriptionSection -> descriptionLines.add(line)
                line.startsWith("TITLE: ") -> title = line.removePrefix("TITLE: ").trim()
                line.startsWith("STATUS: Completed") -> isCompleted = true
                line.startsWith("DUE DATE: ") -> {
                    val dateStr = line.removePrefix("DUE DATE: ").trim()
                    dueDate = parseDateString(dateStr)
                }
                line.startsWith("REMINDER: ") -> {
                    val dateStr = line.removePrefix("REMINDER: ").trim()
                    reminderTime = parseDateString(dateStr)
                }
                line.startsWith("MEDIA: ") -> {
                    mediaPath = line.removePrefix("MEDIA: ").trim().ifBlank { null }
                }
            }
        }

        val description = descriptionLines.joinToString("\n").trim().ifBlank { null }

        return TodoEntity(
            id = id,
            title = title,
            description = description,
            dueDate = dueDate,
            filePath = mediaPath ?: absolutePath,
            isCompleted = isCompleted,
            reminderTime = reminderTime,
            fileFormat = "txt"
        )
    }

    private fun parseDateString(dateStr: String): Long? {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            sdf.parse(dateStr)?.time
        } catch (e: Exception) {
            null
        }
    }
}
