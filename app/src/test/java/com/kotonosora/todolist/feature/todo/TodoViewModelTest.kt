package com.kotonosora.todolist.feature.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotonosora.todolist.domain.model.TodoItem
import com.kotonosora.todolist.domain.repository.TodoRepository
import com.kotonosora.todolist.domain.usecase.*
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TodoViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: TodoRepository
    private lateinit var useCases: TodoUseCases
    private lateinit var viewModel: TestableTodoViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        useCases = TodoUseCases(
            getTodos = GetTodosUseCase(repository),
            getTodosByDate = GetTodosByDateUseCase(repository),
            addTodo = AddTodoUseCase(repository),
            updateTodo = UpdateTodoUseCase(repository),
            deleteTodo = DeleteTodoUseCase(repository)
        )
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `initialState todos should be empty list`() = runTest {
        every { repository.getAllTodos() } returns flowOf(emptyList())
        viewModel = TestableTodoViewModel(useCases)
        advanceUntilIdle()
        assertEquals(emptyList<TodoItem>(), viewModel.todos.value)
    }

    @Test
    fun `addTodo with valid title should call repository insertTodo`() = runTest {
        every { repository.getAllTodos() } returns flowOf(emptyList())
        coEvery { repository.insertTodo(any()) } returns Unit
        viewModel = TestableTodoViewModel(useCases)
        val todo = TodoItem("1", "Test Task", null, null, null, false)
        viewModel.addTodo(todo)
        advanceUntilIdle()
        coVerify { repository.insertTodo(todo) }
    }

    @Test
    fun `deleteTodo should call repository deleteTodo`() = runTest {
        every { repository.getAllTodos() } returns flowOf(emptyList())
        coEvery { repository.deleteTodo(any()) } returns Unit
        viewModel = TestableTodoViewModel(useCases)
        val todo = TodoItem("1", "Task", null, null, null, false)
        viewModel.deleteTodo(todo)
        advanceUntilIdle()
        coVerify { repository.deleteTodo(todo) }
    }

    @Test
    fun `toggleComplete should flip isCompleted and call updateTodo`() = runTest {
        every { repository.getAllTodos() } returns flowOf(emptyList())
        coEvery { repository.updateTodo(any()) } returns Unit
        viewModel = TestableTodoViewModel(useCases)
        val todo = TodoItem("1", "Task", null, null, null, false)
        viewModel.toggleComplete(todo)
        advanceUntilIdle()
        coVerify { repository.updateTodo(todo.copy(isCompleted = true)) }
    }
}

/** Testable ViewModel that takes TodoUseCases directly without needing an Application. */
class TestableTodoViewModel(private val useCases: TodoUseCases) : ViewModel() {
    val todos: StateFlow<List<TodoItem>> = useCases.getTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    fun addTodo(todo: TodoItem) = viewModelScope.launch { useCases.addTodo(todo) }
    fun updateTodo(todo: TodoItem) = viewModelScope.launch { useCases.updateTodo(todo) }
    fun deleteTodo(todo: TodoItem) = viewModelScope.launch { useCases.deleteTodo(todo) }
    fun toggleComplete(todo: TodoItem) = updateTodo(todo.copy(isCompleted = !todo.isCompleted))
}
