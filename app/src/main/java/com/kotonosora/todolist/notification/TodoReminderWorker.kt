package com.kotonosora.todolist.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class TodoReminderWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val notificationManager: TodoNotificationManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val todoId = inputData.getString(KEY_TODO_ID) ?: return Result.failure()
        val todoTitle = inputData.getString(KEY_TODO_TITLE) ?: return Result.failure()
        notificationManager.showReminderNotification(todoId, todoTitle)
        return Result.success()
    }

    companion object {
        const val KEY_TODO_ID = "todo_id"
        const val KEY_TODO_TITLE = "todo_title"
    }
}

