package com.kotonosora.todolist.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotonosora.todolist.domain.model.DayMarkers
import com.kotonosora.todolist.domain.model.NoteItem
import com.kotonosora.todolist.domain.model.MoodEntry
import com.kotonosora.todolist.domain.model.TaskItem
import com.kotonosora.todolist.domain.usecase.DayMarkerUseCases
import com.kotonosora.todolist.domain.usecase.MoodUseCases
import com.kotonosora.todolist.domain.usecase.TaskUseCases
import com.kotonosora.todolist.domain.usecase.VaultUseCases
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Calendar

/**
 * DDD: tasks via [TaskUseCases], notes via [VaultUseCases] — never
 * repositories directly.
 */
class CalendarViewModel(
    private val useCases: TaskUseCases,
    vaultUseCases: VaultUseCases,
    moodUseCases: MoodUseCases,
    dayMarkerUseCases: DayMarkerUseCases
) : ViewModel() {

    private val notesFlow = vaultUseCases.observeNotes()

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

    val allNotes: StateFlow<List<NoteItem>> = notesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allMoods: StateFlow<List<MoodEntry>> = moodUseCases.observeMoods()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Persisted day-markers (single source of truth for calendar dots).
     * Self-heals on first open: a wiped or pre-marker database rebuilds here
     * instead of needing a versioned flag.
     */
    val dayMarkers: StateFlow<Map<LocalDate, DayMarkers>> =
        dayMarkerUseCases.observeDayMarkers()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    init {
        viewModelScope.launch { dayMarkerUseCases.rebuildIfEmpty() }
    }

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

    val moodsForSelectedDate: StateFlow<List<MoodEntry>> = combine(
        _selectedDateMillis, allMoods
    ) { dateMillis, moods ->
        val cal = Calendar.getInstance().apply {
            timeInMillis = dateMillis
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        val end = start + 86_400_000L - 1
        moods.filter { it.createdAt in start..end }
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
