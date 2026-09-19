package com.kotonosora.todolist.data.file

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.documentfile.provider.DocumentFile
import com.kotonosora.todolist.common.AppConstants
import com.kotonosora.todolist.data.repository.UserPreferencesRepository
import com.kotonosora.todolist.domain.model.TaskItem
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date

class AppFileManager(
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

    fun saveTaskToFile(task: TaskItem, customFolderUri: Uri? = null): String? {
        val resolvedUri = resolveCustomFolderUri(customFolderUri)
        val extension = if (task.fileFormat.equals("txt", ignoreCase = true)) "txt" else "md"
        val oppositeExtension = if (extension == "txt") "md" else "txt"
        val fileName = "${task.id}.$extension"
        val oppositeFileName = "${task.id}.$oppositeExtension"

        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", AppConstants.APP_LOCALE)

        val content = if (extension == "txt") {
            buildString {
                appendLine("TITLE: ${task.title}")
                appendLine("STATUS: ${if (task.isCompleted) "Completed" else "Pending"}")
                if (task.dueDate != null) {
                    appendLine("DUE DATE: ${formatter.format(Date(task.dueDate))}")
                }
                if (task.reminderTime != null) {
                    appendLine("REMINDER: ${formatter.format(Date(task.reminderTime))}")
                }
                if (task.filePath != null) {
                    appendLine("MEDIA: ${task.filePath}")
                }
                appendLine()
                if (!task.description.isNullOrBlank()) {
                    appendLine("DESCRIPTION:")
                    appendLine(task.description)
                }
            }
        } else {
            buildString {
                appendLine("# ${task.title}")
                appendLine()
                appendLine("- **Status**: ${if (task.isCompleted) "Completed" else "Pending"}")
                if (task.dueDate != null) {
                    appendLine("- **Due Date**: ${formatter.format(Date(task.dueDate))}")
                }
                if (task.reminderTime != null) {
                    appendLine("- **Reminder**: ${formatter.format(Date(task.reminderTime))}")
                }
                if (task.filePath != null) {
                    appendLine("- **Media**: ${task.filePath}")
                }
                appendLine()
                if (!task.description.isNullOrBlank()) {
                    appendLine("## Description")
                    appendLine(task.description)
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

    fun deleteTaskFile(taskId: String, customFolderUri: Uri? = null): Boolean {
        val resolvedUri = resolveCustomFolderUri(customFolderUri)
        var success = true
        if (resolvedUri != null) {
            try {
                val treeFile = DocumentFile.fromTreeUri(context, resolvedUri)
                if (treeFile != null) {
                    treeFile.findFile("$taskId.md")?.delete()
                    treeFile.findFile("$taskId.txt")?.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val dir = getStorageDir()
        val mdFile = File(dir, "$taskId.md")
        val txtFile = File(dir, "$taskId.txt")
        if (mdFile.exists()) success = success && mdFile.delete()
        if (txtFile.exists()) success = success && txtFile.delete()
        return success
    }
}
