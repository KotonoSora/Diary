package com.kotonosora.todolist.domain.usecase

import com.kotonosora.todolist.domain.model.TaskItem
import com.kotonosora.todolist.domain.repository.TaskRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Calendar

class GetTasksByDateUseCaseTest {

    private lateinit var repository: TaskRepository
    private lateinit var useCase: GetTasksByDateUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = GetTasksByDateUseCase(repository)
    }

    @Test
    fun `invoke should call repository getTasksByDate with correct day boundaries`() = runTest {
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

        every { repository.getTasksByDate(startOfDay, endOfDay) } returns flowOf(emptyList())

        useCase(startOfDay)

        verify { repository.getTasksByDate(startOfDay, endOfDay) }
    }

    @Test
    fun `invoke should return tasks filtered for date`() = runTest {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.MAY, 5, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = cal.timeInMillis
        val endOfDay = startOfDay + 86_400_000L - 1

        val expectedTasks = listOf(
            TaskItem("1", "Task on May 5", null, startOfDay + 3600_000L, null, false)
        )
        every { repository.getTasksByDate(startOfDay, endOfDay) } returns flowOf(expectedTasks)

        useCase(startOfDay).collect { tasks ->
            assertEquals(1, tasks.size)
            assertEquals("Task on May 5", tasks[0].title)
        }
    }
}
