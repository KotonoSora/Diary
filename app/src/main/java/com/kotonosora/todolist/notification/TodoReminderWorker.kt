package com.kotonosora.todolist.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kotonosora.todolist.TodoApplication

class TodoReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val todoId = inputData.getString(KEY_TODO_ID) ?: return Result.failure()
        val todoTitle = inputData.getString(KEY_TODO_TITLE) ?: return Result.failure()
        val app = applicationContext as TodoApplication
        app.container.notificationManager.showReminderNotification(todoId, todoTitle)
        return Result.success()
    }

    companion object {
        const val KEY_TODO_ID = "todo_id"
        const val KEY_TODO_TITLE = "todo_title"
    }
}
