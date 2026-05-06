package com.kotonosora.todolist.data.file

import android.content.Context
import android.os.Environment
import com.kotonosora.todolist.domain.model.TodoItem
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TodoFileManager(private val context: Context) {

    private fun getStorageDir(): File {
        // Use app-scoped external files directory — no WRITE_EXTERNAL_STORAGE required on API 29+
        return (context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: context.filesDir).also { it.mkdirs() }
    }

    fun saveTodoToFile(todo: TodoItem): String? {
        val dir = getStorageDir()
        val file = File(dir, "${todo.id}.md")
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        val content = buildString {
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

        return try {
            file.writeText(content)
            file.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    fun deleteTodoFile(todoId: String): Boolean {
        val file = File(getStorageDir(), "$todoId.md")
        return !file.exists() || file.delete()
    }
}
