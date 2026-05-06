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
 * Re-schedules pending todo reminders after device reboot.
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

                // Re-enqueue reminders for incomplete todos with a future reminderTime
                // Use the suspend one-shot query to avoid hanging on a never-completing Flow
                val incompleteTodos = db.todoDao().getAllTodosOnce()

                val now = System.currentTimeMillis()
                incompleteTodos
                    .filter { !it.isCompleted && it.reminderTime != null && it.reminderTime > now }
                    .forEach { entity ->
                        val delay = entity.reminderTime!! - now
                        val data = workDataOf(
                            TodoReminderWorker.KEY_TODO_ID to entity.id,
                            TodoReminderWorker.KEY_TODO_TITLE to entity.title
                        )
                        val request = OneTimeWorkRequestBuilder<TodoReminderWorker>()
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

