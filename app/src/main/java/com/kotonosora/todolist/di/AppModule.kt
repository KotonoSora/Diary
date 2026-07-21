package com.kotonosora.todolist.di

import android.content.Context
import androidx.work.WorkManager
import com.kotonosora.todolist.data.database.AppDatabase
import com.kotonosora.todolist.data.database.MediaDao
import com.kotonosora.todolist.data.database.TodoDao
import com.kotonosora.todolist.data.file.FileSyncManager
import com.kotonosora.todolist.data.file.TodoFileManager
import com.kotonosora.todolist.data.repository.TodoRepositoryImpl
import com.kotonosora.todolist.domain.repository.TodoRepository
import com.kotonosora.todolist.domain.usecase.AddTodoUseCase
import com.kotonosora.todolist.domain.usecase.DeleteTodoUseCase
import com.kotonosora.todolist.domain.usecase.GetTodosByDateUseCase
import com.kotonosora.todolist.domain.usecase.GetTodosUseCase
import com.kotonosora.todolist.domain.usecase.SyncTodosUseCase
import com.kotonosora.todolist.domain.usecase.TodoUseCases
import com.kotonosora.todolist.domain.usecase.UpdateTodoUseCase
import com.kotonosora.todolist.notification.TodoNotificationManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideTodoDao(db: AppDatabase): TodoDao = db.todoDao()

    @Provides
    @Singleton
    fun provideMediaDao(db: AppDatabase): MediaDao = db.mediaDao()

    @Provides
    @Singleton
    fun provideTodoFileManager(@ApplicationContext context: Context): TodoFileManager {
        return TodoFileManager(context)
    }

    @Provides
    @Singleton
    fun provideFileSyncManager(
        @ApplicationContext context: Context,
        todoDao: TodoDao
    ): FileSyncManager = FileSyncManager(context, todoDao)

    @Provides
    @Singleton
    fun provideTodoRepository(
        todoDao: TodoDao,
        fileManager: TodoFileManager
    ): TodoRepository = TodoRepositoryImpl(todoDao, fileManager)

    @Provides
    @Singleton
    fun provideTodoUseCases(
        repository: TodoRepository,
        fileSyncManager: FileSyncManager,
        fileManager: TodoFileManager
    ): TodoUseCases = TodoUseCases(
        getTodos = GetTodosUseCase(repository),
        getTodosByDate = GetTodosByDateUseCase(repository),
        addTodo = AddTodoUseCase(repository),
        updateTodo = UpdateTodoUseCase(repository),
        deleteTodo = DeleteTodoUseCase(repository),
        syncTodos = SyncTodosUseCase(fileSyncManager, fileManager)
    )

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideTodoNotificationManager(
        @ApplicationContext context: Context
    ): TodoNotificationManager = TodoNotificationManager(context)
}
