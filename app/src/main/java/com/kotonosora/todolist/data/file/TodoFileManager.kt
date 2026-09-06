package com.kotonosora.todolist.data.file

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.documentfile.provider.DocumentFile
import com.kotonosora.todolist.data.repository.UserPreferencesRepository
import com.kotonosora.todolist.domain.model.TodoItem
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TodoFileManager @Inject constructor(
    private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository? = null
) {

    fun getStorageDir(): File {
        return (context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: context.filesDir).also { it.mkdirs() }
    }

    private fun resolveCustomFolderUri(overrideUri: Uri?): Uri? {
        if (overrideUri != null) return overrideUri
        val savedUriStr = runBlocking {
            try {
                userPreferencesRepository?.customStorageFolderUri?.firstOrNull()
            } catch (e: Exception) {
                null
            }
        }
        return if (!savedUriStr.isNullOrBlank()) Uri.parse(savedUriStr) else null
    }

    fun saveTodoToFile(todo: TodoItem, customFolderUri: Uri? = null): String? {
        val resolvedUri = resolveCustomFolderUri(customFolderUri)
        val extension = if (todo.fileFormat.equals("txt", ignoreCase = true)) "txt" else "md"
        val oppositeExtension = if (extension == "txt") "md" else "txt"
        val fileName = "${todo.id}.$extension"
        val oppositeFileName = "${todo.id}.$oppositeExtension"

        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        val content = if (extension == "txt") {
            buildString {
                appendLine("TITLE: ${todo.title}")
                appendLine("STATUS: ${if (todo.isCompleted) "Completed" else "Pending"}")
                if (todo.dueDate != null) {
                    appendLine("DUE DATE: ${formatter.format(Date(todo.dueDate))}")
                }
                if (todo.reminderTime != null) {
                    appendLine("REMINDER: ${formatter.format(Date(todo.reminderTime))}")
                }
                if (todo.filePath != null) {
                    appendLine("MEDIA: ${todo.filePath}")
                }
                appendLine()
                if (!todo.description.isNullOrBlank()) {
                    appendLine("DESCRIPTION:")
                    appendLine(todo.description)
                }
            }
        } else {
            buildString {
                appendLine("# ${todo.title}")
                appendLine()
                appendLine("- **Status**: ${if (todo.isCompleted) "Completed" else "Pending"}")
                if (todo.dueDate != null) {
                    appendLine("- **Due Date**: ${formatter.format(Date(todo.dueDate))}")
                }
                if (todo.reminderTime != null) {
                    appendLine("- **Reminder**: ${formatter.format(Date(todo.reminderTime))}")
                }
                if (todo.filePath != null) {
                    appendLine("- **Media**: ${todo.filePath}")
                }
                appendLine()
                if (!todo.description.isNullOrBlank()) {
                    appendLine("## Description")
                    appendLine(todo.description)
                }
            }
        }

        if (resolvedUri != null) {
            try {
                val treeFile = DocumentFile.fromTreeUri(context, resolvedUri)
                if (treeFile != null && treeFile.canWrite()) {
                    // Delete opposite file if exists
                    treeFile.findFile(oppositeFileName)?.delete()

                    var targetDoc = treeFile.findFile(fileName)
                    if (targetDoc == null) {
                        val mimeType = if (extension == "txt") "text/plain" else "text/markdown"
                        targetDoc = treeFile.createFile(mimeType, fileName)
                    }

                    targetDoc?.uri?.let { uri ->
                        context.contentResolver.openOutputStream(uri, "rwt")?.use { stream ->
                            stream.write(content.toByteArray(Charsets.UTF_8))
                        }
                        return uri.toString()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Fallback to local app storage directory
        val dir = getStorageDir()
        val file = File(dir, fileName)
        val oppositeFile = File(dir, oppositeFileName)
        if (oppositeFile.exists()) {
            oppositeFile.delete()
        }

        return try {
            file.writeText(content)
            file.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    fun deleteTodoFile(todoId: String, customFolderUri: Uri? = null): Boolean {
        val resolvedUri = resolveCustomFolderUri(customFolderUri)
        var success = true
        if (resolvedUri != null) {
            try {
                val treeFile = DocumentFile.fromTreeUri(context, resolvedUri)
                if (treeFile != null) {
                    treeFile.findFile("$todoId.md")?.delete()
                    treeFile.findFile("$todoId.txt")?.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val dir = getStorageDir()
        val mdFile = File(dir, "$todoId.md")
        val txtFile = File(dir, "$todoId.txt")
        if (mdFile.exists()) success = success && mdFile.delete()
        if (txtFile.exists()) success = success && txtFile.delete()
        return success
    }
}
