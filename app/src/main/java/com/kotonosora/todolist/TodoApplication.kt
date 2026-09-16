package com.kotonosora.todolist

import android.app.Application
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.kotonosora.todolist.data.sync.FileSyncWorker
import com.kotonosora.todolist.di.AppContainer
import com.kotonosora.todolist.notification.TodoNotificationManager

class TodoApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Load the native library once for the entire app lifecycle
        System.loadLibrary("todolist")
        // Create the notification channel
        TodoNotificationManager.createNotificationChannel(this)
        // Trigger a one-time bidirectional sync of .md files <-> Room DB on every launch.
        WorkManager.getInstance(this).enqueueUniqueWork(
            FileSyncWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<FileSyncWorker>().build()
        )
    }
}
