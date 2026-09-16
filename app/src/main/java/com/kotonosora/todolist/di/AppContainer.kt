package com.kotonosora.todolist.di

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.work.WorkManager
import com.kotonosora.todolist.data.database.AppDatabase
import com.kotonosora.todolist.data.database.LinkDao
import com.kotonosora.todolist.data.database.MediaDao
import com.kotonosora.todolist.data.database.NoteDao
import com.kotonosora.todolist.data.database.TagDao
import com.kotonosora.todolist.data.database.TodoDao
import com.kotonosora.todolist.data.database.ZettelMetadataDao
import com.kotonosora.todolist.data.factory.DefaultExoPlayerFactory
import com.kotonosora.todolist.data.factory.DefaultMediaRecorderFactory
import com.kotonosora.todolist.data.factory.ExoPlayerFactory
import com.kotonosora.todolist.data.factory.MediaRecorderFactory
import com.kotonosora.todolist.data.file.FileSyncManager
import com.kotonosora.todolist.data.file.MediaFileManager
import com.kotonosora.todolist.data.file.TodoFileManager
import com.kotonosora.todolist.data.file.VaultManager
import com.kotonosora.todolist.data.repository.TodoRepositoryImpl
import com.kotonosora.todolist.data.repository.UserPreferencesRepository
import com.kotonosora.todolist.data.repository.VaultRepositoryImpl
import com.kotonosora.todolist.domain.repository.TodoRepository
import com.kotonosora.todolist.domain.repository.VaultRepository
import com.kotonosora.todolist.domain.usecase.AddTodoUseCase
import com.kotonosora.todolist.domain.usecase.DeleteTodoUseCase
import com.kotonosora.todolist.domain.usecase.GetTodosByDateUseCase
import com.kotonosora.todolist.domain.usecase.GetTodosUseCase
import com.kotonosora.todolist.domain.usecase.SyncTodosUseCase
import com.kotonosora.todolist.domain.usecase.TodoUseCases
import com.kotonosora.todolist.domain.usecase.UpdateTodoUseCase
import com.kotonosora.todolist.notification.TodoNotificationManager

class AppContainer(private val applicationContext: Context) {

    val appDatabase: AppDatabase by lazy {
        AppDatabase.getDatabase(applicationContext)
    }

    val todoDao: TodoDao by lazy { appDatabase.todoDao() }
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

    val todoFileManager: TodoFileManager by lazy {
        TodoFileManager(applicationContext, userPreferencesRepository)
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
        FileSyncManager(applicationContext, todoDao, userPreferencesRepository)
    }

    val vaultRepository: VaultRepository by lazy {
        VaultRepositoryImpl(vaultManager, noteDao, linkDao, tagDao, zettelMetadataDao)
    }

    val todoRepository: TodoRepository by lazy {
        TodoRepositoryImpl(todoDao, todoFileManager)
    }

    val todoUseCases: TodoUseCases by lazy {
        TodoUseCases(
            getTodos = GetTodosUseCase(todoRepository),
            getTodosByDate = GetTodosByDateUseCase(todoRepository),
            addTodo = AddTodoUseCase(todoRepository),
            updateTodo = UpdateTodoUseCase(todoRepository),
            deleteTodo = DeleteTodoUseCase(todoRepository),
            syncTodos = SyncTodosUseCase(fileSyncManager, todoFileManager)
        )
    }

    val workManager: WorkManager by lazy {
        WorkManager.getInstance(applicationContext)
    }

    val notificationManager: TodoNotificationManager by lazy {
        TodoNotificationManager(applicationContext)
    }
}

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer not provided in CompositionLocal!")
}
