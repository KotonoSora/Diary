package com.kotonosora.todolist.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotonosora.todolist.domain.model.TodoItem
import com.kotonosora.todolist.domain.repository.TodoRepository
import com.kotonosora.todolist.domain.usecase.*
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: TodoRepository
    private lateinit var useCases: TodoUseCases
    private lateinit var viewModel: CalendarViewModel

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
        every { repository.getAllTodos() } returns flowOf(emptyList())
        viewModel = CalendarViewModel(useCases)
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `previousMonth decrements the displayed month`() = runTest {
        val initialMonth = viewModel.currentMonth.value
        val initialYear = viewModel.currentYear.value

        viewModel.previousMonth()
        advanceUntilIdle()

        val cal = Calendar.getInstance().apply { set(initialYear, initialMonth, 1) }
        cal.add(Calendar.MONTH, -1)
        assertEquals(cal.get(Calendar.MONTH), viewModel.currentMonth.value)
        assertEquals(cal.get(Calendar.YEAR), viewModel.currentYear.value)
    }

    @Test
    fun `nextMonth increments the displayed month`() = runTest {
        val initialMonth = viewModel.currentMonth.value
        val initialYear = viewModel.currentYear.value

        viewModel.nextMonth()
        advanceUntilIdle()

        val cal = Calendar.getInstance().apply { set(initialYear, initialMonth, 1) }
        cal.add(Calendar.MONTH, 1)
        assertEquals(cal.get(Calendar.MONTH), viewModel.currentMonth.value)
        assertEquals(cal.get(Calendar.YEAR), viewModel.currentYear.value)
    }

    @Test
    fun `selectDate updates selectedDateMillis`() = runTest {
        val newDate = Calendar.getInstance().apply {
            set(2026, Calendar.JUNE, 15, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        viewModel.selectDate(newDate)

        assertEquals(newDate, viewModel.selectedDateMillis.value)
    }

    @Test
    fun `todosForSelectedDate filters todos by selected date`() = runTest {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.MAY, 5, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todoDueToday = TodoItem("1", "Task today", null, cal.timeInMillis, null, false)
        val todoDueTomorrow = TodoItem("2", "Task tomorrow", null, cal.timeInMillis + 86_400_000L, null, false)

        every { repository.getAllTodos() } returns flowOf(listOf(todoDueToday, todoDueTomorrow))
        viewModel = CalendarViewModel(useCases)

        // Subscribe so WhileSubscribed starts collecting the upstream combine
        val collectionJob = launch { viewModel.todosForSelectedDate.collect {} }

        val startOfDay = Calendar.getInstance().apply {
            set(2026, Calendar.MAY, 5, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        viewModel.selectDate(startOfDay)
        advanceUntilIdle()

        val todosForDate = viewModel.todosForSelectedDate.value
        collectionJob.cancel()
        assertEquals(1, todosForDate.size)
        assertEquals("Task today", todosForDate[0].title)
    }
}

