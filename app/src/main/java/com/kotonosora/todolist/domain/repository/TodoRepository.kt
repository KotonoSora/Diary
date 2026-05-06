package com.kotonosora.todolist.domain.repository

import com.kotonosora.todolist.domain.model.TodoItem
import kotlinx.coroutines.flow.Flow

interface TodoRepository {
    fun getAllTodos(): Flow<List<TodoItem>>
    fun getTodosByDate(startOfDay: Long, endOfDay: Long): Flow<List<TodoItem>>
    suspend fun getTodoById(id: String): TodoItem?
    suspend fun insertTodo(todo: TodoItem)
    suspend fun updateTodo(todo: TodoItem)
    suspend fun deleteTodo(todo: TodoItem)
}
