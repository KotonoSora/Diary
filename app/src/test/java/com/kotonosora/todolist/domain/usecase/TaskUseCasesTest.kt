package com.kotonosora.todolist.domain.usecase

import com.kotonosora.todolist.domain.model.TaskItem
import com.kotonosora.todolist.domain.repository.TaskRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class TaskUseCasesTest {

    private lateinit var repository: TaskRepository
    private lateinit var getTasksUseCase: GetTasksUseCase
    private lateinit var addTaskUseCase: AddTaskUseCase
    private lateinit var updateTaskUseCase: UpdateTaskUseCase
    private lateinit var deleteTaskUseCase: DeleteTaskUseCase

    @Before
    fun setup() {
        repository = mockk()
        getTasksUseCase = GetTasksUseCase(repository)
        addTaskUseCase = AddTaskUseCase(repository)
        updateTaskUseCase = UpdateTaskUseCase(repository)
        deleteTaskUseCase = DeleteTaskUseCase(repository)
    }

    @Test
    fun `getTasks should return flow of tasks from repository`() = runTest {
        val dummyTasks = listOf(
            TaskItem("1", "Title 1", null, null, null, false, reminderTime = null),
            TaskItem("2", "Title 2", null, null, null, true, reminderTime = null)
        )
        coEvery { repository.getAllTasks() } returns flowOf(dummyTasks)

        val resultFlow = getTasksUseCase()

        resultFlow.collect { tasks ->
            assertEquals(2, tasks.size)
            assertEquals("Title 1", tasks[0].title)
        }
    }

    @Test
    fun `addTask with blank title should throw exception`() = runTest {
        val invalidTask = TaskItem("1", "", null, null, null, false)

        var exceptionThrown = false
        try {
            addTaskUseCase(invalidTask)
        } catch (e: IllegalArgumentException) {
            exceptionThrown = true
        }
        assertEquals(true, exceptionThrown)
    }

    @Test
    fun `addTask with valid title should call repository insert`() = runTest {
        val validTask = TaskItem("1", "Valid Title", null, null, null, false)
        coEvery { repository.insertTask(validTask) } returns Unit

        addTaskUseCase(validTask)

        coVerify { repository.insertTask(validTask) }
    }
}
