package com.kotonosora.todolist.data.repository

import com.kotonosora.todolist.data.database.TodoDao
import com.kotonosora.todolist.data.database.TodoEntity
import com.kotonosora.todolist.data.file.TodoFileManager
import com.kotonosora.todolist.data.mapper.toDomain
import com.kotonosora.todolist.domain.model.TodoItem
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class TodoRepositoryTest {

    private lateinit var todoDao: TodoDao
    private lateinit var todoFileManager: TodoFileManager
    private lateinit var repository: TodoRepositoryImpl

    @Before
    fun setup() {
        todoDao = mockk(relaxed = true)
        todoFileManager = mockk(relaxed = true)
        repository = TodoRepositoryImpl(todoDao, todoFileManager)
    }

    @Test
    fun `insertTodo should save to file and then to database`() = runTest {
        val todo = TodoItem("id1", "Test todo", null, null, null, false)

        coEvery { todoFileManager.saveTodoToFile(todo) } returns "/path/to/file.md"
        coEvery { todoDao.insertTodo(any()) } just Runs

        repository.insertTodo(todo)

        coVerify(ordering = Ordering.ORDERED) {
            todoFileManager.saveTodoToFile(todo)
            todoDao.insertTodo(any())
        }
    }

    @Test
    fun `updateTodo should update file and database`() = runTest {
        val todo = TodoItem("id1", "Updated Title", null, null, null, true)

        coEvery { todoFileManager.saveTodoToFile(todo) } returns "/path/to/file.md"
        coEvery { todoDao.updateTodo(any()) } just Runs

        repository.updateTodo(todo)

        coVerify { todoFileManager.saveTodoToFile(todo) }
        coVerify { todoDao.updateTodo(any()) }
    }

    @Test
    fun `deleteTodo should delete file and database entry`() = runTest {
        val todo = TodoItem("id1", "Title", null, null, null, false)

        coEvery { todoFileManager.deleteTodoFile("id1") } returns true
        coEvery { todoDao.deleteTodo(any()) } just Runs

        repository.deleteTodo(todo)

        coVerify { todoFileManager.deleteTodoFile("id1") }
        coVerify { todoDao.deleteTodo(any()) }
    }

    @Test
    fun `getAllTodos should return mapped domain objects`() = runTest {
        val entity = TodoEntity("id1", "Entity Title", null, null, null, false, null)
        every { todoDao.getAllTodos() } returns flowOf(listOf(entity))

        val result = mutableListOf<List<TodoItem>>()
        repository.getAllTodos().collect { result.add(it) }

        assert(result.isNotEmpty())
        assert(result[0][0].title == "Entity Title")
    }
}

