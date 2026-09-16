package com.kotonosora.todolist.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.kotonosora.todolist.data.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Re-schedules pending task reminders after device reboot.
 * WorkManager re-enqueues persisted tasks automatically, but this receiver
 * provides an extra safety net by querying the DB directly.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val workManager = WorkManager.getInstance(context)

                // Re-enqueue reminders for incomplete tasks with a future reminderTime
                // Use the suspend one-shot query to avoid hanging on a never-completing Flow
                val incompleteTasks = db.taskDao().getAllTasksOnce()

                val now = System.currentTimeMillis()
                incompleteTasks
                    .filter { !it.isCompleted && it.reminderTime != null && it.reminderTime > now }
                    .forEach { entity ->
                        val delay = entity.reminderTime!! - now
                        val data = workDataOf(
                            AppReminderWorker.KEY_TASK_ID to entity.id,
                            AppReminderWorker.KEY_TASK_TITLE to entity.title
                        )
                        val request = OneTimeWorkRequestBuilder<AppReminderWorker>()
                            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                            .setInputData(data)
                            .build()
                        workManager.enqueueUniqueWork(
                            "reminder_${entity.id}",
                            ExistingWorkPolicy.KEEP,
                            request
                        )
                    }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
