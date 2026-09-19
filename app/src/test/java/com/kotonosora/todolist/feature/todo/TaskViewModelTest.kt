package com.kotonosora.todolist.feature.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotonosora.todolist.domain.model.TaskItem
import com.kotonosora.todolist.domain.repository.TaskRepository
import com.kotonosora.todolist.domain.usecase.AddTaskUseCase
import com.kotonosora.todolist.domain.usecase.DeleteTaskUseCase
import com.kotonosora.todolist.domain.usecase.GetTasksByDateUseCase
import com.kotonosora.todolist.domain.usecase.GetTasksUseCase
import com.kotonosora.todolist.domain.usecase.TaskUseCases
import com.kotonosora.todolist.domain.usecase.UpdateTaskUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TaskViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: TaskRepository
    private lateinit var useCases: TaskUseCases
    private lateinit var viewModel: TestableTaskViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        useCases = TaskUseCases(
            getTasks = GetTasksUseCase(repository),
            getTasksByDate = GetTasksByDateUseCase(repository),
            addTask = AddTaskUseCase(repository),
            updateTask = UpdateTaskUseCase(repository),
            deleteTask = DeleteTaskUseCase(repository)
        )
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `initialState tasks should be empty list`() = runTest {
        every { repository.getAllTasks() } returns flowOf(emptyList())
        viewModel = TestableTaskViewModel(useCases)
        advanceUntilIdle()
        assertEquals(emptyList<TaskItem>(), viewModel.tasks.value)
    }

    @Test
    fun `addTask with valid title should call repository insertTask`() = runTest {
        every { repository.getAllTasks() } returns flowOf(emptyList())
        coEvery { repository.insertTask(any()) } returns Unit
        viewModel = TestableTaskViewModel(useCases)
        val task = TaskItem("1", "Test Task", null, null, null, false)
        viewModel.addTask(task)
        advanceUntilIdle()
        coVerify { repository.insertTask(task) }
    }

    @Test
    fun `deleteTask should call repository deleteTask`() = runTest {
        every { repository.getAllTasks() } returns flowOf(emptyList())
        coEvery { repository.deleteTask(any()) } returns Unit
        viewModel = TestableTaskViewModel(useCases)
        val task = TaskItem("1", "Task", null, null, null, false)
        viewModel.deleteTask(task)
        advanceUntilIdle()
        coVerify { repository.deleteTask(task) }
    }

    @Test
    fun `toggleComplete should flip isCompleted and call updateTask`() = runTest {
        every { repository.getAllTasks() } returns flowOf(emptyList())
        coEvery { repository.updateTask(any()) } returns Unit
        viewModel = TestableTaskViewModel(useCases)
        val task = TaskItem("1", "Task", null, null, null, false)
        viewModel.toggleComplete(task)
        advanceUntilIdle()
        coVerify { repository.updateTask(task.copy(isCompleted = true)) }
    }
}

class TestableTaskViewModel(private val useCases: TaskUseCases) : ViewModel() {
    val tasks: StateFlow<List<TaskItem>> = useCases.getTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    fun addTask(task: TaskItem) = viewModelScope.launch { useCases.addTask(task) }
    fun updateTask(task: TaskItem) = viewModelScope.launch { useCases.updateTask(task) }
    fun deleteTask(task: TaskItem) = viewModelScope.launch { useCases.deleteTask(task) }
    fun toggleComplete(task: TaskItem) = updateTask(task.copy(isCompleted = !task.isCompleted))
}
