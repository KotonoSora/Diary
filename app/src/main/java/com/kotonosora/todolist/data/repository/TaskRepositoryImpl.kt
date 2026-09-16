package com.kotonosora.todolist.data.repository

import com.kotonosora.todolist.data.database.TaskDao
import com.kotonosora.todolist.data.file.AppFileManager
import com.kotonosora.todolist.data.mapper.toDomain
import com.kotonosora.todolist.data.mapper.toEntity
import com.kotonosora.todolist.domain.model.TaskItem
import com.kotonosora.todolist.domain.repository.TaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class TaskRepositoryImpl(
    private val taskDao: TaskDao,
    private val appFileManager: AppFileManager
) : TaskRepository {

    override fun getAllTasks(): Flow<List<TaskItem>> {
        return taskDao.getAllTasks().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getTasksByDate(startOfDay: Long, endOfDay: Long): Flow<List<TaskItem>> {
        return taskDao.getTasksByDate(startOfDay, endOfDay).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getTaskById(id: String): TaskItem? {
        return taskDao.getTaskById(id)?.toDomain()
    }

    override suspend fun insertTask(task: TaskItem) = withContext(Dispatchers.IO) {
        // Save to file first
        appFileManager.saveTaskToFile(task)
        // Save to Room DB
        taskDao.insertTask(task.toEntity())
    }

    override suspend fun updateTask(task: TaskItem) = withContext(Dispatchers.IO) {
        appFileManager.saveTaskToFile(task)
        taskDao.updateTask(task.toEntity())
    }

    override suspend fun deleteTask(task: TaskItem) = withContext(Dispatchers.IO) {
        appFileManager.deleteTaskFile(task.id)
        taskDao.deleteTask(task.toEntity())
    }
}
