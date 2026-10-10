package com.kotonosora.todolist.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import com.kotonosora.todolist.MainApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Re-schedules pending task reminders after device reboot.
 * WorkManager re-enqueues persisted tasks automatically, but this receiver
 * provides an extra safety net via [ReenqueueDueRemindersUseCase][com.kotonosora.todolist.domain.usecase.ReenqueueDueRemindersUseCase].
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as MainApplication
                app.container.notificationUseCases.reenqueueDue(
                    policy = ExistingWorkPolicy.KEEP
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
