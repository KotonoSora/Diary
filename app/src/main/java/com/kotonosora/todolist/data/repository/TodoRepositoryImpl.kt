package com.kotonosora.todolist.data.repository

import com.kotonosora.todolist.data.database.TodoDao
import com.kotonosora.todolist.data.file.TodoFileManager
import com.kotonosora.todolist.data.mapper.toDomain
import com.kotonosora.todolist.data.mapper.toEntity
import com.kotonosora.todolist.domain.model.TodoItem
import com.kotonosora.todolist.domain.repository.TodoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class TodoRepositoryImpl(
    private val todoDao: TodoDao,
    private val todoFileManager: TodoFileManager
) : TodoRepository {

    override fun getAllTodos(): Flow<List<TodoItem>> {
        return todoDao.getAllTodos().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getTodosByDate(startOfDay: Long, endOfDay: Long): Flow<List<TodoItem>> {
        return todoDao.getTodosByDate(startOfDay, endOfDay).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getTodoById(id: String): TodoItem? {
        return todoDao.getTodoById(id)?.toDomain()
    }

    override suspend fun insertTodo(todo: TodoItem) = withContext(Dispatchers.IO) {
        // Save to file first
        todoFileManager.saveTodoToFile(todo)
        // Save to Room DB
        todoDao.insertTodo(todo.toEntity())
    }

    override suspend fun updateTodo(todo: TodoItem) = withContext(Dispatchers.IO) {
        todoFileManager.saveTodoToFile(todo)
        todoDao.updateTodo(todo.toEntity())
    }

    override suspend fun deleteTodo(todo: TodoItem) = withContext(Dispatchers.IO) {
        todoFileManager.deleteTodoFile(todo.id)
        todoDao.deleteTodo(todo.toEntity())
    }
}
