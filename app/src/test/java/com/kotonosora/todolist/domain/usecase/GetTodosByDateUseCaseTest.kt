package com.kotonosora.todolist.domain.usecase

import com.kotonosora.todolist.domain.model.TodoItem
import com.kotonosora.todolist.domain.repository.TodoRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Calendar

class GetTodosByDateUseCaseTest {

    private lateinit var repository: TodoRepository
    private lateinit var useCase: GetTodosByDateUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = GetTodosByDateUseCase(repository)
    }

    @Test
    fun `invoke should call repository getTodosByDate with correct day boundaries`() = runTest {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.MAY)
            set(Calendar.DAY_OF_MONTH, 5)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = cal.timeInMillis
        val endOfDay = startOfDay + 86_400_000L - 1

        every { repository.getTodosByDate(startOfDay, endOfDay) } returns flowOf(emptyList())

        useCase(startOfDay)

        verify { repository.getTodosByDate(startOfDay, endOfDay) }
    }

    @Test
    fun `invoke should return todos filtered for date`() = runTest {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.MAY, 5, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = cal.timeInMillis
        val endOfDay = startOfDay + 86_400_000L - 1

        val expectedTodos = listOf(
            TodoItem("1", "Task on May 5", null, startOfDay + 3600_000L, null, false)
        )
        every { repository.getTodosByDate(startOfDay, endOfDay) } returns flowOf(expectedTodos)

        useCase(startOfDay).collect { todos ->
            assertEquals(1, todos.size)
            assertEquals("Task on May 5", todos[0].title)
        }
    }
}

