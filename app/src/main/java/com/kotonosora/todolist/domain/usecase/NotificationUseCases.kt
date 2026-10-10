package com.kotonosora.todolist.domain.usecase

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.kotonosora.todolist.domain.model.TaskItem
import com.kotonosora.todolist.domain.repository.TaskRepository
import com.kotonosora.todolist.notification.AppReminderWorker
import kotlinx.coroutines.flow.firstOrNull
import java.util.concurrent.TimeUnit

/**
 * Notification bounded context — application layer (DDD).
 *
 * Owns task-reminder scheduling: the `reminder_{id}` unique-work naming,
 * the past-due skip, and the reboot re-enqueue sweep. ViewModels and
 * receivers depend on [NotificationUseCases], never on WorkManager
 * directly. Mirrors the [TaskUseCases] bundle pattern.
 *
 * WorkManager lives in `domain/usecase` pragmatically, the same way
 * `SyncTasksUseCase` already depends on the data-layer `FileSyncManager`.
 */

class ScheduleTaskReminderUseCase(private val workManager: WorkManager) {
    /**
     * @return false when there is nothing to schedule (no reminder time or
     * already past-due); true when the reminder was enqueued.
     */
    operator fun invoke(
        task: TaskItem,
        policy: ExistingWorkPolicy = ExistingWorkPolicy.REPLACE,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        val reminderTime = task.reminderTime ?: return false
        val delay = reminderTime - nowMillis
        if (delay <= 0) return false

        val data = workDataOf(
            AppReminderWorker.KEY_TASK_ID to task.id,
            AppReminderWorker.KEY_TASK_TITLE to task.title
        )
        val request = OneTimeWorkRequestBuilder<AppReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()
        workManager.enqueueUniqueWork(
            reminderName(task.id),
            policy,
            request
        )
        return true
    }
}

class CancelTaskReminderUseCase(private val workManager: WorkManager) {
    operator fun invoke(taskId: String) {
        if (taskId.isBlank()) return
        workManager.cancelUniqueWork(reminderName(taskId))
    }
}

class ReenqueueDueRemindersUseCase(
    private val taskRepository: TaskRepository,
    private val scheduler: ScheduleTaskReminderUseCase
) {
    /**
     * Re-enqueues reminders for unfinished tasks with a future reminder time
     * (reboot safety net). @return how many reminders were enqueued.
     */
    suspend operator fun invoke(
        policy: ExistingWorkPolicy = ExistingWorkPolicy.KEEP,
        nowMillis: Long = System.currentTimeMillis()
    ): Int {
        val tasks = try {
            taskRepository.getAllTasks().firstOrNull() ?: return 0
        } catch (_: Exception) {
            return 0
        }
        var enqueued = 0
        for (task in tasks) {
            if (!task.isCompleted && scheduler(task, policy, nowMillis)) {
                enqueued++
            }
        }
        return enqueued
    }
}

/** Single owner of the per-task unique-work name (`reminder_{id}`). */
fun reminderName(taskId: String): String = "reminder_$taskId"

data class NotificationUseCases(
    val scheduleReminder: ScheduleTaskReminderUseCase,
    val cancelReminder: CancelTaskReminderUseCase,
    val reenqueueDue: ReenqueueDueRemindersUseCase
) {
    companion object {
        fun from(workManager: WorkManager, taskRepository: TaskRepository): NotificationUseCases {
            val scheduler = ScheduleTaskReminderUseCase(workManager)
            return NotificationUseCases(
                scheduleReminder = scheduler,
                cancelReminder = CancelTaskReminderUseCase(workManager),
                reenqueueDue = ReenqueueDueRemindersUseCase(taskRepository, scheduler)
            )
        }
    }
}
