package com.kotonosora.todolist.domain.repository

import com.kotonosora.todolist.domain.model.TaskItem
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun getAllTasks(): Flow<List<TaskItem>>
    fun getTasksByDate(startOfDay: Long, endOfDay: Long): Flow<List<TaskItem>>
    suspend fun getTaskById(id: String): TaskItem?
    suspend fun insertTask(task: TaskItem)
    suspend fun updateTask(task: TaskItem)
    suspend fun deleteTask(task: TaskItem)
}
