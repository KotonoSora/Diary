package com.kotonosora.todolist.feature.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotonosora.todolist.data.native.MainNativeHelper
import com.kotonosora.todolist.domain.model.TaskItem
import com.kotonosora.todolist.domain.usecase.NotificationUseCases
import com.kotonosora.todolist.domain.usecase.PreferencesUseCases
import com.kotonosora.todolist.domain.usecase.TaskUseCases
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * DDD: task CRUD via [TaskUseCases], reminders via [NotificationUseCases],
 * settings via [PreferencesUseCases] — never WorkManager or repositories
 * directly.
 */
class TaskViewModel(
    private val useCases: TaskUseCases,
    private val notifications: NotificationUseCases,
    private val prefs: PreferencesUseCases
) : ViewModel() {

    val customFolderUri: StateFlow<String?> = prefs.observeCustomStorageFolder()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Unfiltered list of all tasks — use this for lookups (e.g. edit screen). */
    val allTasks: StateFlow<List<TaskItem>> = useCases.getTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /** Filtered list of tasks based on prefix search using C++ native helper. */
    val tasks: StateFlow<List<TaskItem>> = combine(allTasks, _searchQuery) { tasks, query ->
        if (query.isBlank()) {
            tasks
        } else {
            val titles = tasks.map { it.title }.toTypedArray()
            val matchedTitles = MainNativeHelper.filterByPrefix(titles, query).toSet()
            tasks.filter { it.title in matchedTitles }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun saveCustomFolderUri(uriStr: String?) = viewModelScope.launch {
        prefs.saveCustomStorageFolder(uriStr)
    }

    fun syncTasks() = viewModelScope.launch {
        _isLoading.value = true
        val startTime = System.currentTimeMillis()
        useCases.syncTasks?.invoke()
        val elapsedTime = System.currentTimeMillis() - startTime
        if (elapsedTime < 600) {
            delay(600 - elapsedTime)
        }
        _isLoading.value = false
    }

    fun addTask(task: TaskItem) = viewModelScope.launch {
        useCases.addTask(task)
        task.reminderTime?.let { notifications.scheduleReminder(task) }
    }

    fun updateTask(task: TaskItem) = viewModelScope.launch {
        useCases.updateTask(task)
        if (task.reminderTime != null) {
            notifications.scheduleReminder(task)
        } else {
            notifications.cancelReminder(task.id)
        }
    }

    fun deleteTask(task: TaskItem) = viewModelScope.launch {
        useCases.deleteTask(task)
        notifications.cancelReminder(task.id)
    }

    fun toggleTask(task: TaskItem) = viewModelScope.launch {
        updateTask(task.copy(isCompleted = !task.isCompleted))
    }
}
