package com.kotonosora.todolist.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kotonosora.todolist.domain.usecase.TodoUseCases
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * One-time WorkManager worker that reconciles .md files in the Documents directory
 * with the Room database (bidirectional sync via [TodoUseCases.syncTodos]).
 *
 * Scheduled once per app launch with [ExistingWorkPolicy.KEEP] so only a single
 * instance runs at a time, even if the app is started multiple times quickly.
 */
@HiltWorker
class FileSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val useCases: TodoUseCases
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            useCases.syncTodos?.invoke()
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

