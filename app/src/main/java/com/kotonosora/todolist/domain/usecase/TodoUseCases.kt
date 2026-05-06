package com.kotonosora.todolist.domain.usecase

import com.kotonosora.todolist.data.file.FileSyncManager
import com.kotonosora.todolist.data.file.TodoFileManager
import com.kotonosora.todolist.domain.model.TodoItem
import com.kotonosora.todolist.domain.repository.TodoRepository
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class GetTodosUseCase(private val repository: TodoRepository) {
    operator fun invoke(): Flow<List<TodoItem>> = repository.getAllTodos()
}

class GetTodosByDateUseCase(private val repository: TodoRepository) {
    operator fun invoke(dateMillis: Long): Flow<List<TodoItem>> {
        val cal = Calendar.getInstance().apply {
            timeInMillis = dateMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = cal.timeInMillis
        val endOfDay = startOfDay + 86_400_000L - 1
        return repository.getTodosByDate(startOfDay, endOfDay)
    }
}

class AddTodoUseCase(private val repository: TodoRepository) {
    suspend operator fun invoke(todo: TodoItem) {
        if (todo.title.isBlank()) throw IllegalArgumentException("Title cannot be blank")
        repository.insertTodo(todo)
    }
}

class UpdateTodoUseCase(private val repository: TodoRepository) {
    suspend operator fun invoke(todo: TodoItem) {
        if (todo.title.isBlank()) throw IllegalArgumentException("Title cannot be blank")
        repository.updateTodo(todo)
    }
}

class DeleteTodoUseCase(private val repository: TodoRepository) {
    suspend operator fun invoke(todo: TodoItem) = repository.deleteTodo(todo)
}

class SyncTodosUseCase(
    private val fileSyncManager: FileSyncManager,
    private val fileManager: TodoFileManager
) {
    suspend operator fun invoke() {
        fileSyncManager.syncFilesToDb()
        fileSyncManager.syncDbToFiles(fileManager)
    }
}

data class TodoUseCases(
    val getTodos: GetTodosUseCase,
    val getTodosByDate: GetTodosByDateUseCase,
    val addTodo: AddTodoUseCase,
    val updateTodo: UpdateTodoUseCase,
    val deleteTodo: DeleteTodoUseCase,
    val syncTodos: SyncTodosUseCase? = null
)
