package com.kotonosora.todolist.data.file

import android.content.Context
import android.os.Environment
import com.kotonosora.todolist.data.database.TodoDao
import com.kotonosora.todolist.data.database.TodoEntity
import java.io.File

/**
 * Bidirectional sync between .md files in Documents and the SQLite database.
 */
class FileSyncManager(
    private val context: Context,
    private val todoDao: TodoDao
) {

    private fun getStorageDir(): File {
        return (context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: context.filesDir).also { it.mkdirs() }
    }

    /**
     * Reads all .md files from the Documents directory and upserts any
     * IDs not currently in the database.
     */
    suspend fun syncFilesToDb() {
        val dir = getStorageDir()
        val mdFiles = dir.listFiles { f -> f.extension == "md" } ?: return

        for (file in mdFiles) {
            val todoId = file.nameWithoutExtension
            val existing = todoDao.getTodoById(todoId)
            if (existing == null) {
                val parsed = parseMdFile(file, todoId)
                todoDao.insertTodo(parsed)
            }
        }
    }

    /**
     * Writes a .md file for every DB entity whose backing file is missing
     * (e.g. file was deleted externally while the app was closed).
     */
    suspend fun syncDbToFiles(fileManager: TodoFileManager) {
        val dir = getStorageDir()
        val existingFileIds = dir.listFiles { f -> f.extension == "md" }
            ?.map { it.nameWithoutExtension }
            ?.toSet() ?: emptySet()

        val allEntities = todoDao.getAllTodosOnce()
        for (entity in allEntities) {
            if (entity.id !in existingFileIds) {
                val domainItem = com.kotonosora.todolist.domain.model.TodoItem(
                    id = entity.id,
                    title = entity.title,
                    description = entity.description,
                    dueDate = entity.dueDate,
                    filePath = entity.filePath,
                    isCompleted = entity.isCompleted,
                    reminderTime = entity.reminderTime
                )
                fileManager.saveTodoToFile(domainItem)
            }
        }
    }

    // ── Simple markdown parser ────────────────────────────────────────────────

    private fun parseMdFile(file: File, id: String): TodoEntity {
        var title = id
        var isCompleted = false
        var dueDate: Long? = null
        val descriptionLines = mutableListOf<String>()
        var inDescriptionSection = false

        for (line in file.readLines()) {
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
            }
        }

        val description = descriptionLines.joinToString("\n").trim().ifBlank { null }

        return TodoEntity(
            id = id,
            title = title,
            description = description,
            dueDate = dueDate,
            filePath = file.absolutePath,
            isCompleted = isCompleted
        )
    }

    private fun parseDateString(dateStr: String): Long? {
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
            sdf.parse(dateStr)?.time
        } catch (e: Exception) {
            null
        }
    }
}

