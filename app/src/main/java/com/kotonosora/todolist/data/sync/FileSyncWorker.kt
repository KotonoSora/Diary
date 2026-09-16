package com.kotonosora.todolist.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kotonosora.todolist.TodoApplication

/**
 * One-time WorkManager worker that reconciles .md files in the Documents directory
 * with the Room database (bidirectional sync via AppContainer todoUseCases.syncTodos).
 */
class FileSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val app = applicationContext as TodoApplication
            app.container.todoUseCases.syncTodos?.invoke()
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "file_sync_startup"
    }
}
