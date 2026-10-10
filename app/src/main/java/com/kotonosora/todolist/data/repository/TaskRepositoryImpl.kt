package com.kotonosora.todolist.data.repository

import com.kotonosora.todolist.data.database.TaskDao
import com.kotonosora.todolist.data.file.AppFileManager
import com.kotonosora.todolist.data.mapper.toDomain
import com.kotonosora.todolist.data.mapper.toEntity
import com.kotonosora.todolist.domain.model.TaskItem
import com.kotonosora.todolist.domain.model.dayKeyOf
import com.kotonosora.todolist.domain.repository.DayMarkerRepository
import com.kotonosora.todolist.domain.repository.TaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class TaskRepositoryImpl(
    private val taskDao: TaskDao,
    private val appFileManager: AppFileManager,
    private val dayMarkers: DayMarkerRepository
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
        taskDao.insertTask(task.toEntity())
        val day = dayKeyOf(task.dueDate)
        if (day != null) dayMarkers.refreshDays(setOf(day))
    }

    override suspend fun updateTask(task: TaskItem) = withContext(Dispatchers.IO) {
        // Refresh both the old and the new due-date day (a move must clear
        // the old dot as well as set the new one). Skipped entirely when the
        // day didn't change — the recompute would return identical counts.
        val oldDay = dayKeyOf(taskDao.getTaskById(task.id)?.dueDate)
        taskDao.updateTask(task.toEntity())
        val newDay = dayKeyOf(task.dueDate)
        if (oldDay != newDay) dayMarkers.refreshDays(setOfNotNull(oldDay, newDay))
    }

    override suspend fun deleteTask(task: TaskItem) = withContext(Dispatchers.IO) {
        taskDao.deleteTask(task.toEntity())
        val day = dayKeyOf(task.dueDate)
        if (day != null) dayMarkers.refreshDays(setOf(day))
    }
}
