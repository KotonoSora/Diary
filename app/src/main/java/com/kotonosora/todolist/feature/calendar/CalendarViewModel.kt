package com.kotonosora.todolist.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotonosora.todolist.domain.model.NoteItem
import com.kotonosora.todolist.domain.model.TaskItem
import com.kotonosora.todolist.domain.repository.VaultRepository
import com.kotonosora.todolist.domain.usecase.TaskUseCases
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class CalendarViewModel(
    private val useCases: TaskUseCases,
    private val vaultRepository: VaultRepository? = null
) : ViewModel() {

    private val _currentYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    private val _currentMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH))
    private val _selectedDateMillis = MutableStateFlow(
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    )

    val currentYear: StateFlow<Int> = _currentYear.asStateFlow()
    val currentMonth: StateFlow<Int> = _currentMonth.asStateFlow()  // 0-based
    val selectedDateMillis: StateFlow<Long> = _selectedDateMillis.asStateFlow()

    val allTodos: StateFlow<List<TaskItem>> = useCases.getTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allNotes: StateFlow<List<NoteItem>> = vaultRepository?.getAllNotes()
        ?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        ?: MutableStateFlow(emptyList())

    val todosForSelectedDate: StateFlow<List<TaskItem>> = combine(
        _selectedDateMillis, allTodos
    ) { dateMillis, todos ->
        val cal = Calendar.getInstance().apply {
            timeInMillis = dateMillis
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        val end = start + 86_400_000L - 1
        todos.filter { it.dueDate != null && it.dueDate in start..end }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val notesForSelectedDate: StateFlow<List<NoteItem>> = combine(
        _selectedDateMillis, allNotes
    ) { dateMillis, notes ->
        val cal = Calendar.getInstance().apply {
            timeInMillis = dateMillis
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        val end = start + 86_400_000L - 1
        notes.filter { it.updatedAt in start..end }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun selectDate(dateMillis: Long) {
        _selectedDateMillis.value = dateMillis
        val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
        _currentYear.value = cal.get(Calendar.YEAR)
        _currentMonth.value = cal.get(Calendar.MONTH)
    }

    fun selectToday() {
        val todayCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        _selectedDateMillis.value = todayCal.timeInMillis
        _currentYear.value = todayCal.get(Calendar.YEAR)
        _currentMonth.value = todayCal.get(Calendar.MONTH)
    }

    fun previousMonth() {
        val cal = Calendar.getInstance().apply { set(_currentYear.value, _currentMonth.value, 1) }
        cal.add(Calendar.MONTH, -1)
        _currentYear.value = cal.get(Calendar.YEAR)
        _currentMonth.value = cal.get(Calendar.MONTH)
    }

    fun nextMonth() {
        val cal = Calendar.getInstance().apply { set(_currentYear.value, _currentMonth.value, 1) }
        cal.add(Calendar.MONTH, 1)
        _currentYear.value = cal.get(Calendar.YEAR)
        _currentMonth.value = cal.get(Calendar.MONTH)
    }

    fun toggleTodoStatus(todo: TaskItem) {
        viewModelScope.launch {
            useCases.updateTask(todo.copy(isCompleted = !todo.isCompleted))
        }
    }
}
