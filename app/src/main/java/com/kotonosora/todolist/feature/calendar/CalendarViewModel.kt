package com.kotonosora.todolist.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotonosora.todolist.domain.model.TodoItem
import com.kotonosora.todolist.domain.usecase.TodoUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val useCases: TodoUseCases
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

    val allTodos: StateFlow<List<TodoItem>> = useCases.getTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val todosForSelectedDate: StateFlow<List<TodoItem>> = combine(
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

    fun selectDate(dateMillis: Long) {
        _selectedDateMillis.value = dateMillis
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
}
