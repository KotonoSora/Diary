package com.kotonosora.todolist.di

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.work.WorkManager
import com.kotonosora.todolist.data.database.AppDatabase
import com.kotonosora.todolist.data.database.LinkDao
import com.kotonosora.todolist.data.database.MediaDao
import com.kotonosora.todolist.data.database.NoteDao
import com.kotonosora.todolist.data.database.TagDao
import com.kotonosora.todolist.data.database.TaskDao
import com.kotonosora.todolist.data.database.ZettelMetadataDao
import com.kotonosora.todolist.data.factory.DefaultExoPlayerFactory
import com.kotonosora.todolist.data.factory.DefaultMediaRecorderFactory
import com.kotonosora.todolist.data.factory.ExoPlayerFactory
import com.kotonosora.todolist.data.factory.MediaRecorderFactory
import com.kotonosora.todolist.data.file.AppFileManager
import com.kotonosora.todolist.data.file.FileSyncManager
import com.kotonosora.todolist.data.file.MediaFileManager
import com.kotonosora.todolist.data.file.VaultManager
import com.kotonosora.todolist.data.repository.TaskRepositoryImpl
import com.kotonosora.todolist.data.repository.UserPreferencesRepository
import com.kotonosora.todolist.data.repository.VaultRepositoryImpl
import com.kotonosora.todolist.domain.repository.TaskRepository
import com.kotonosora.todolist.domain.repository.VaultRepository
import com.kotonosora.todolist.domain.usecase.AddTaskUseCase
import com.kotonosora.todolist.domain.usecase.DeleteTaskUseCase
import com.kotonosora.todolist.domain.usecase.GetTasksByDateUseCase
import com.kotonosora.todolist.domain.usecase.GetTasksUseCase
import com.kotonosora.todolist.domain.usecase.SyncTasksUseCase
import com.kotonosora.todolist.domain.usecase.TaskUseCases
import com.kotonosora.todolist.domain.usecase.UpdateTaskUseCase
import com.kotonosora.todolist.notification.AppNotificationManager

class AppContainer(private val applicationContext: Context) {

    val appDatabase: AppDatabase by lazy {
        AppDatabase.getDatabase(applicationContext)
    }

    val taskDao: TaskDao by lazy { appDatabase.taskDao() }
    val mediaDao: MediaDao by lazy { appDatabase.mediaDao() }
    val noteDao: NoteDao by lazy { appDatabase.noteDao() }
    val linkDao: LinkDao by lazy { appDatabase.linkDao() }
    val tagDao: TagDao by lazy { appDatabase.tagDao() }
    val zettelMetadataDao: ZettelMetadataDao by lazy { appDatabase.zettelMetadataDao() }

    val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(applicationContext)
    }

    val vaultManager: VaultManager by lazy {
        VaultManager(applicationContext, userPreferencesRepository)
    }

    val appFileManager: AppFileManager by lazy {
        AppFileManager(applicationContext, userPreferencesRepository)
    }

    val mediaFileManager: MediaFileManager by lazy {
        MediaFileManager(applicationContext)
    }

    val mediaRecorderFactory: MediaRecorderFactory by lazy {
        DefaultMediaRecorderFactory(applicationContext)
    }

    val exoPlayerFactory: ExoPlayerFactory by lazy {
        DefaultExoPlayerFactory(applicationContext)
    }

    val fileSyncManager: FileSyncManager by lazy {
        FileSyncManager(applicationContext, taskDao, userPreferencesRepository)
    }

    val vaultRepository: VaultRepository by lazy {
        VaultRepositoryImpl(vaultManager, noteDao, linkDao, tagDao, zettelMetadataDao)
    }

    val taskRepository: TaskRepository by lazy {
        TaskRepositoryImpl(taskDao, appFileManager)
    }

    val taskUseCases: TaskUseCases by lazy {
        TaskUseCases(
            getTasks = GetTasksUseCase(taskRepository),
            getTasksByDate = GetTasksByDateUseCase(taskRepository),
            addTask = AddTaskUseCase(taskRepository),
            updateTask = UpdateTaskUseCase(taskRepository),
            deleteTask = DeleteTaskUseCase(taskRepository),
            syncTasks = SyncTasksUseCase(fileSyncManager, appFileManager)
        )
    }

    val workManager: WorkManager by lazy {
        WorkManager.getInstance(applicationContext)
    }

    val notificationManager: AppNotificationManager by lazy {
        AppNotificationManager(applicationContext)
    }
}

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer not provided in CompositionLocal!")
}
