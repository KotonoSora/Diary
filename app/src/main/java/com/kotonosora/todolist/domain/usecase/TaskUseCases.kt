package com.kotonosora.todolist.domain.usecase

import com.kotonosora.todolist.data.file.AppFileManager
import com.kotonosora.todolist.data.file.FileSyncManager
import com.kotonosora.todolist.domain.model.TaskItem
import com.kotonosora.todolist.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class GetTasksUseCase(private val repository: TaskRepository) {
    operator fun invoke(): Flow<List<TaskItem>> = repository.getAllTasks()
}

class GetTasksByDateUseCase(private val repository: TaskRepository) {
    operator fun invoke(dateMillis: Long): Flow<List<TaskItem>> {
        val cal = Calendar.getInstance().apply {
            timeInMillis = dateMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = cal.timeInMillis
        val endOfDay = startOfDay + 86_400_000L - 1
        return repository.getTasksByDate(startOfDay, endOfDay)
    }
}

class AddTaskUseCase(private val repository: TaskRepository) {
    suspend operator fun invoke(task: TaskItem) {
        if (task.title.isBlank()) throw IllegalArgumentException("Title cannot be blank")
        repository.insertTask(task)
    }
}

class UpdateTaskUseCase(private val repository: TaskRepository) {
    suspend operator fun invoke(task: TaskItem) {
        if (task.title.isBlank()) throw IllegalArgumentException("Title cannot be blank")
        repository.updateTask(task)
    }
}

class DeleteTaskUseCase(private val repository: TaskRepository) {
    suspend operator fun invoke(task: TaskItem) = repository.deleteTask(task)
}

class SyncTasksUseCase(
    private val fileSyncManager: FileSyncManager,
    private val fileManager: AppFileManager
) {
    suspend operator fun invoke() {
        fileSyncManager.syncFilesToDb()
        fileSyncManager.syncDbToFiles(fileManager)
    }
}

data class TaskUseCases(
    val getTasks: GetTasksUseCase,
    val getTasksByDate: GetTasksByDateUseCase,
    val addTask: AddTaskUseCase,
    val updateTask: UpdateTaskUseCase,
    val deleteTask: DeleteTaskUseCase,
    val syncTasks: SyncTasksUseCase? = null
)
