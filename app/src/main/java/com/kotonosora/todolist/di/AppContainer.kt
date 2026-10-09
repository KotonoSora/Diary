package com.kotonosora.todolist.di

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.work.WorkManager
import com.kotonosora.todolist.data.database.AppDatabase
import com.kotonosora.todolist.data.database.DeckProgressDatabase
import com.kotonosora.todolist.data.database.LinkDao
import com.kotonosora.todolist.data.database.NoteDao
import com.kotonosora.todolist.data.database.NoteFtsDao
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
import com.kotonosora.todolist.data.media.AudioCaptureServiceImpl
import com.kotonosora.todolist.data.repository.DayMarkerRepositoryImpl
import com.kotonosora.todolist.data.repository.FlashcardRepositoryImpl
import com.kotonosora.todolist.data.repository.MediaRepositoryImpl
import com.kotonosora.todolist.data.repository.MoodRepositoryImpl
import com.kotonosora.todolist.data.repository.PdfReaderRepositoryImpl
import com.kotonosora.todolist.data.repository.TaskRepositoryImpl
import com.kotonosora.todolist.data.repository.UserPreferencesRepository
import com.kotonosora.todolist.data.repository.VaultRepositoryImpl
import com.kotonosora.todolist.domain.repository.FlashcardRepository
import com.kotonosora.todolist.domain.repository.DayMarkerRepository
import com.kotonosora.todolist.domain.repository.MediaRepository
import com.kotonosora.todolist.domain.repository.MoodRepository
import com.kotonosora.todolist.domain.repository.PdfReaderRepository
import com.kotonosora.todolist.domain.repository.PreferencesRepository
import com.kotonosora.todolist.domain.repository.TaskRepository
import com.kotonosora.todolist.domain.repository.VaultRepository
import com.kotonosora.todolist.domain.service.AudioCaptureService
import com.kotonosora.todolist.domain.usecase.AddTaskUseCase
import com.kotonosora.todolist.domain.usecase.DayMarkerUseCases
import com.kotonosora.todolist.domain.usecase.DeleteTaskUseCase
import com.kotonosora.todolist.domain.usecase.FlashcardUseCases
import com.kotonosora.todolist.domain.usecase.GetTasksByDateUseCase
import com.kotonosora.todolist.domain.usecase.GetTasksUseCase
import com.kotonosora.todolist.domain.usecase.MediaUseCases
import com.kotonosora.todolist.domain.usecase.MoodUseCases
import com.kotonosora.todolist.domain.usecase.NotificationUseCases
import com.kotonosora.todolist.domain.usecase.PdfUseCases
import com.kotonosora.todolist.domain.usecase.PreferencesUseCases
import com.kotonosora.todolist.domain.usecase.SyncTasksUseCase
import com.kotonosora.todolist.domain.usecase.TaskUseCases
import com.kotonosora.todolist.domain.usecase.UpdateTaskUseCase
import com.kotonosora.todolist.domain.usecase.VaultUseCases
import com.kotonosora.todolist.notification.AppNotificationManager

class AppContainer(private val applicationContext: Context) {

    val appDatabase: AppDatabase by lazy {
        AppDatabase.getDatabase(applicationContext)
    }

    val taskDao: TaskDao by lazy { appDatabase.taskDao() }
    val noteDao: NoteDao by lazy { appDatabase.noteDao() }
    val linkDao: LinkDao by lazy { appDatabase.linkDao() }
    val tagDao: TagDao by lazy { appDatabase.tagDao() }
    val zettelMetadataDao: ZettelMetadataDao by lazy { appDatabase.zettelMetadataDao() }
    val noteFtsDao: NoteFtsDao by lazy { appDatabase.noteFtsDao() }

    val pdfReaderRepository: PdfReaderRepository by lazy {
        PdfReaderRepositoryImpl(appDatabase.pdfReaderDao())
    }

    val mediaRepository: MediaRepository by lazy {
        MediaRepositoryImpl(applicationContext, appDatabase.mediaDao())
    }

    val moodRepository: MoodRepository by lazy {
        MoodRepositoryImpl(appDatabase.moodDao(), dayMarkerRepository)
    }

    val dayMarkerRepository: DayMarkerRepository by lazy {
        DayMarkerRepositoryImpl(appDatabase.dayMarkerDao())
    }

    val userPreferencesRepository: PreferencesRepository by lazy {
        UserPreferencesRepository(applicationContext)
    }

    private val deckProgressDatabase: DeckProgressDatabase by lazy {
        DeckProgressDatabase.getDatabase(applicationContext)
    }

    val flashcardRepository: FlashcardRepository by lazy {
        FlashcardRepositoryImpl(
            database = deckProgressDatabase,
            deckDao = deckProgressDatabase.flashcardDeckDao(),
            cardDao = deckProgressDatabase.flashcardCardDao(),
            progressDao = deckProgressDatabase.deckProgressDao()
        )
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
        FileSyncManager(applicationContext, taskDao, userPreferencesRepository, dayMarkerRepository)
    }

    val vaultRepository: VaultRepository by lazy {
        VaultRepositoryImpl(
            vaultManager,
            noteDao,
            linkDao,
            tagDao,
            zettelMetadataDao,
            noteFtsDao,
            dayMarkerRepository
        )
    }

    val taskRepository: TaskRepository by lazy {
        TaskRepositoryImpl(taskDao, appFileManager, dayMarkerRepository)
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

    val pdfUseCases: PdfUseCases by lazy {
        PdfUseCases.from(pdfReaderRepository)
    }

    val vaultUseCases: VaultUseCases by lazy {
        VaultUseCases.from(vaultRepository)
    }

    val mediaUseCases: MediaUseCases by lazy {
        MediaUseCases.from(mediaRepository)
    }

    val moodUseCases: MoodUseCases by lazy {
        MoodUseCases.from(moodRepository)
    }

    val dayMarkerUseCases: DayMarkerUseCases by lazy {
        DayMarkerUseCases.from(dayMarkerRepository)
    }

    val flashcardUseCases: FlashcardUseCases by lazy {
        FlashcardUseCases.from(flashcardRepository)
    }

    val preferencesUseCases: PreferencesUseCases by lazy {
        PreferencesUseCases.from(userPreferencesRepository)
    }

    val notificationUseCases: NotificationUseCases by lazy {
        NotificationUseCases.from(workManager, taskRepository)
    }

    /**
     * Per-ViewModel audio recorder (owns its own polling scope — never a
     * singleton, so state can't leak across `MediaViewModel` instances).
     */
    fun newAudioCaptureService(): AudioCaptureService = AudioCaptureServiceImpl(
        applicationContext,
        mediaFileManager,
        mediaRecorderFactory
    )

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
