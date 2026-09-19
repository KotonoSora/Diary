package com.kotonosora.todolist.feature.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.kotonosora.todolist.data.native.MainNativeHelper
import com.kotonosora.todolist.data.repository.UserPreferencesRepository
import com.kotonosora.todolist.domain.model.TaskItem
import com.kotonosora.todolist.domain.usecase.TaskUseCases
import com.kotonosora.todolist.notification.AppReminderWorker
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class TaskViewModel(
    private val useCases: TaskUseCases,
    private val workManager: WorkManager,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val customFolderUri: StateFlow<String?> = userPreferencesRepository.customStorageFolderUri
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
        userPreferencesRepository.saveCustomStorageFolderUri(uriStr)
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
        task.reminderTime?.let { scheduleReminder(task) }
    }

    fun updateTask(task: TaskItem) = viewModelScope.launch {
        useCases.updateTask(task)
        if (task.reminderTime != null) {
            scheduleReminder(task)
        } else {
            cancelReminder(task.id)
        }
    }

    fun deleteTask(task: TaskItem) = viewModelScope.launch {
        useCases.deleteTask(task)
        cancelReminder(task.id)
    }

    fun toggleTask(task: TaskItem) = viewModelScope.launch {
        updateTask(task.copy(isCompleted = !task.isCompleted))
    }

    private fun scheduleReminder(task: TaskItem) {
        val delay = (task.reminderTime ?: return) - System.currentTimeMillis()
        if (delay <= 0) return

        val data = workDataOf(
            AppReminderWorker.KEY_TASK_ID to task.id,
            AppReminderWorker.KEY_TASK_TITLE to task.title
        )

        val request = OneTimeWorkRequestBuilder<AppReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()

        workManager.enqueueUniqueWork(
            "reminder_${task.id}",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun cancelReminder(taskId: String) {
        workManager.cancelUniqueWork("reminder_$taskId")
    }
}
