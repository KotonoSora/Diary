package com.kotonosora.todolist

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.kotonosora.todolist.data.sync.FileSyncWorker
import com.kotonosora.todolist.notification.TodoNotificationManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class TodoApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Load the native library once for the entire app lifecycle
        System.loadLibrary("todolist")
        // Create the notification channel
        TodoNotificationManager.createNotificationChannel(this)
        // Trigger a one-time bidirectional sync of .md files <-> Room DB on every launch.
        // ExistingWorkPolicy.KEEP ensures only one sync runs even for rapid restarts.
        WorkManager.getInstance(this).enqueueUniqueWork(
            FileSyncWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<FileSyncWorker>().build()
        )
    }
}
