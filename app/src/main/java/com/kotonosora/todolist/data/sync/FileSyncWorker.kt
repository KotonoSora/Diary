package com.kotonosora.todolist.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kotonosora.todolist.MainApplication

/**
 * One-time WorkManager worker that reconciles .md files in the Documents directory
 * with the Room database (bidirectional sync via AppContainer taskUseCases.syncTasks).
 */
class FileSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val app = applicationContext as MainApplication
            app.container.taskUseCases.syncTasks?.invoke()
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
