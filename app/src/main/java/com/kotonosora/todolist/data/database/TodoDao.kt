package com.kotonosora.todolist.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    @Query("SELECT * FROM todo_items ORDER BY dueDate ASC")
    fun getAllTodos(): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todo_items WHERE dueDate >= :startOfDay AND dueDate <= :endOfDay ORDER BY dueDate ASC")
    fun getTodosByDate(startOfDay: Long, endOfDay: Long): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todo_items WHERE id = :id")
    suspend fun getTodoById(id: String): TodoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodo(todo: TodoEntity)

    @Update
    suspend fun updateTodo(todo: TodoEntity)

    @Delete
    suspend fun deleteTodo(todo: TodoEntity)

    @Query("SELECT * FROM todo_items")
    suspend fun getAllTodosOnce(): List<TodoEntity>
}
