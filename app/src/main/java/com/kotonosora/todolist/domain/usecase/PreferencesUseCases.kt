package com.kotonosora.todolist.domain.usecase

import com.kotonosora.todolist.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow

/**
 * Settings bounded context — application layer (DDD).
 *
 * Owns every DataStore `user_settings` read/write (theme, vault folder,
 * note format, onboarding/seed flags). ViewModels depend on
 * [PreferencesUseCases], never on [PreferencesRepository] directly.
 * Value validation lives here so every caller shares it.
 * Mirrors the [TaskUseCases] bundle pattern.
 */

const val THEME_MODE_SYSTEM = "system"
const val THEME_MODE_LIGHT = "light"
const val THEME_MODE_DARK = "dark"

private val VALID_THEME_MODES = setOf(THEME_MODE_SYSTEM, THEME_MODE_LIGHT, THEME_MODE_DARK)

const val NOTE_FORMAT_MARKDOWN = "md"
const val NOTE_FORMAT_TEXT = "txt"

private val VALID_NOTE_FORMATS = setOf(NOTE_FORMAT_MARKDOWN, NOTE_FORMAT_TEXT)

class ObserveThemeModeUseCase(private val repository: PreferencesRepository) {
    operator fun invoke(): Flow<String> = repository.themeMode
}

class SaveThemeModeUseCase(private val repository: PreferencesRepository) {
    suspend operator fun invoke(mode: String) {
        require(mode in VALID_THEME_MODES) { "Unknown theme mode: $mode" }
        repository.saveThemeMode(mode)
    }
}

class ObserveCustomStorageFolderUseCase(private val repository: PreferencesRepository) {
    operator fun invoke(): Flow<String?> = repository.customStorageFolderUri
}

class SaveCustomStorageFolderUseCase(private val repository: PreferencesRepository) {
    suspend operator fun invoke(uri: String?) {
        repository.saveCustomStorageFolderUri(uri?.takeIf { it.isNotBlank() })
    }
}

class ObserveDefaultNoteFormatUseCase(private val repository: PreferencesRepository) {
    operator fun invoke(): Flow<String> = repository.defaultNoteFormat
}

class SaveDefaultNoteFormatUseCase(private val repository: PreferencesRepository) {
    suspend operator fun invoke(format: String) {
        require(format in VALID_NOTE_FORMATS) { "Unknown note format: $format" }
        repository.saveDefaultNoteFormat(format)
    }
}

class ObserveHasCompletedOnboardingUseCase(private val repository: PreferencesRepository) {
    operator fun invoke(): Flow<Boolean> = repository.hasCompletedOnboarding
}

class SaveHasCompletedOnboardingUseCase(private val repository: PreferencesRepository) {
    suspend operator fun invoke(completed: Boolean) {
        repository.saveHasCompletedOnboarding(completed)
    }
}

class ObserveHasSeededFlashcardsUseCase(private val repository: PreferencesRepository) {
    operator fun invoke(): Flow<Boolean> = repository.hasSeededFlashcards
}

class SaveHasSeededFlashcardsUseCase(private val repository: PreferencesRepository) {
    suspend operator fun invoke(seeded: Boolean) {
        repository.saveHasSeededFlashcards(seeded)
    }
}

data class PreferencesUseCases(
    val observeThemeMode: ObserveThemeModeUseCase,
    val saveThemeMode: SaveThemeModeUseCase,
    val observeCustomStorageFolder: ObserveCustomStorageFolderUseCase,
    val saveCustomStorageFolder: SaveCustomStorageFolderUseCase,
    val observeDefaultNoteFormat: ObserveDefaultNoteFormatUseCase,
    val saveDefaultNoteFormat: SaveDefaultNoteFormatUseCase,
    val observeHasCompletedOnboarding: ObserveHasCompletedOnboardingUseCase,
    val saveHasCompletedOnboarding: SaveHasCompletedOnboardingUseCase,
    val observeHasSeededFlashcards: ObserveHasSeededFlashcardsUseCase,
    val saveHasSeededFlashcards: SaveHasSeededFlashcardsUseCase
) {
    companion object {
        fun from(repository: PreferencesRepository): PreferencesUseCases = PreferencesUseCases(
            observeThemeMode = ObserveThemeModeUseCase(repository),
            saveThemeMode = SaveThemeModeUseCase(repository),
            observeCustomStorageFolder = ObserveCustomStorageFolderUseCase(repository),
            saveCustomStorageFolder = SaveCustomStorageFolderUseCase(repository),
            observeDefaultNoteFormat = ObserveDefaultNoteFormatUseCase(repository),
            saveDefaultNoteFormat = SaveDefaultNoteFormatUseCase(repository),
            observeHasCompletedOnboarding = ObserveHasCompletedOnboardingUseCase(repository),
            saveHasCompletedOnboarding = SaveHasCompletedOnboardingUseCase(repository),
            observeHasSeededFlashcards = ObserveHasSeededFlashcardsUseCase(repository),
            saveHasSeededFlashcards = SaveHasSeededFlashcardsUseCase(repository)
        )
    }
}
