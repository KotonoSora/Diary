package com.kotonosora.todolist.feature.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.kotonosora.todolist.data.native.TodoNativeHelper
import com.kotonosora.todolist.domain.model.TodoItem
import com.kotonosora.todolist.domain.usecase.TodoUseCases
import com.kotonosora.todolist.notification.TodoReminderWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class TodoViewModel @Inject constructor(
    private val useCases: TodoUseCases,
    private val workManager: WorkManager
) : ViewModel() {

    /** Unfiltered list of all todos — use this for lookups (e.g. edit screen). */
    val allTodos: StateFlow<List<TodoItem>> = useCases.getTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    /** All todos, filtered via native C++ filterByPrefix when a search query is active. */
    val todos: StateFlow<List<TodoItem>> = combine(allTodos, _searchQuery) { todos, query ->
        if (query.isBlank()) {
            todos
        } else {
            val titles = todos.map { it.title }.toTypedArray()
            val matchedTitles = TodoNativeHelper.filterByPrefix(titles, query).toSet()
            todos.filter { it.title in matchedTitles }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setSearchQuery(query: String) { _searchQuery.value = query }

    fun addTodo(todo: TodoItem) = viewModelScope.launch {
        useCases.addTodo(todo)
        todo.reminderTime?.let { scheduleReminder(todo) }
    }

    fun updateTodo(todo: TodoItem) = viewModelScope.launch {
        useCases.updateTodo(todo)
        if (todo.reminderTime != null) scheduleReminder(todo) else cancelReminder(todo.id)
    }

    fun deleteTodo(todo: TodoItem) = viewModelScope.launch {
        cancelReminder(todo.id)
        useCases.deleteTodo(todo)
    }

    fun toggleComplete(todo: TodoItem) = updateTodo(todo.copy(isCompleted = !todo.isCompleted))

    private fun scheduleReminder(todo: TodoItem) {
        val delay = (todo.reminderTime ?: return) - System.currentTimeMillis()
        if (delay <= 0) return
        val data = workDataOf(
            TodoReminderWorker.KEY_TODO_ID to todo.id,
            TodoReminderWorker.KEY_TODO_TITLE to todo.title
        )
        val request = OneTimeWorkRequestBuilder<TodoReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()
        workManager.enqueueUniqueWork("reminder_${todo.id}", ExistingWorkPolicy.REPLACE, request)
    }

    private fun cancelReminder(todoId: String) {
        workManager.cancelUniqueWork("reminder_$todoId")
    }
}


