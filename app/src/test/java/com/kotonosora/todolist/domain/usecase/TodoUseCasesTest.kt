package com.kotonosora.todolist.domain.usecase

import com.kotonosora.todolist.domain.model.TodoItem
import com.kotonosora.todolist.domain.repository.TodoRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class TodoUseCasesTest {

    private lateinit var repository: TodoRepository
    private lateinit var getTodosUseCase: GetTodosUseCase
    private lateinit var addTodoUseCase: AddTodoUseCase
    private lateinit var updateTodoUseCase: UpdateTodoUseCase
    private lateinit var deleteTodoUseCase: DeleteTodoUseCase

    @Before
    fun setup() {
        repository = mockk()
        getTodosUseCase = GetTodosUseCase(repository)
        addTodoUseCase = AddTodoUseCase(repository)
        updateTodoUseCase = UpdateTodoUseCase(repository)
        deleteTodoUseCase = DeleteTodoUseCase(repository)
    }

    @Test
    fun `getTodos should return flow of todos from repository`() = runTest {
        val dummyTodos = listOf(
            TodoItem("1", "Title 1", null, null, null, false, reminderTime = null),
            TodoItem("2", "Title 2", null, null, null, true, reminderTime = null)
        )
        coEvery { repository.getAllTodos() } returns flowOf(dummyTodos)

        val resultFlow = getTodosUseCase()

        resultFlow.collect { todos ->
            assertEquals(2, todos.size)
            assertEquals("Title 1", todos[0].title)
        }
    }

    @Test
    fun `addTodo with blank title should throw exception`() = runTest {
        val invalidTodo = TodoItem("1", "", null, null, null, false)

        var exceptionThrown = false
        try {
            addTodoUseCase(invalidTodo)
        } catch (e: IllegalArgumentException) {
            exceptionThrown = true
        }
        assertEquals(true, exceptionThrown)
    }

    @Test
    fun `addTodo with valid title should call repository insert`() = runTest {
        val validTodo = TodoItem("1", "Valid Title", null, null, null, false)
        coEvery { repository.insertTodo(validTodo) } returns Unit

        addTodoUseCase(validTodo)

        coVerify { repository.insertTodo(validTodo) }
    }
}
