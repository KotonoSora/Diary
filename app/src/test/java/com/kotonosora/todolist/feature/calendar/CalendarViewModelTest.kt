package com.kotonosora.todolist.feature.calendar

import com.kotonosora.todolist.domain.model.TaskItem
import com.kotonosora.todolist.domain.repository.TaskRepository
import com.kotonosora.todolist.domain.usecase.AddTaskUseCase
import com.kotonosora.todolist.domain.usecase.DeleteTaskUseCase
import com.kotonosora.todolist.domain.usecase.GetTasksByDateUseCase
import com.kotonosora.todolist.domain.usecase.GetTasksUseCase
import com.kotonosora.todolist.domain.usecase.TaskUseCases
import com.kotonosora.todolist.domain.usecase.UpdateTaskUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: TaskRepository
    private lateinit var useCases: TaskUseCases
    private lateinit var viewModel: CalendarViewModel

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
        every { repository.getAllTasks() } returns flowOf(emptyList())
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
        val taskDueToday = TaskItem("1", "Task today", null, cal.timeInMillis, null, false)
        val taskDueTomorrow = TaskItem("2", "Task tomorrow", null, cal.timeInMillis + 86_400_000L, null, false)

        every { repository.getAllTasks() } returns flowOf(listOf(taskDueToday, taskDueTomorrow))
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
