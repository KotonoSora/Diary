package com.kotonosora.todolist.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * User settings backed by DataStore (`user_settings`). Implemented by
 * `UserPreferencesRepository`; ViewModels and file managers depend on this
 * interface, never on the DataStore implementation directly.
 */
interface PreferencesRepository {
    val customStorageFolderUri: Flow<String?>
    val themeMode: Flow<String>
    val defaultNoteFormat: Flow<String>
    val hasCompletedOnboarding: Flow<Boolean>
    val hasSeededFlashcards: Flow<Boolean>

    suspend fun saveCustomStorageFolderUri(uri: String?)
    suspend fun saveThemeMode(mode: String)
    suspend fun saveDefaultNoteFormat(format: String)
    suspend fun saveHasCompletedOnboarding(completed: Boolean)
    suspend fun saveHasSeededFlashcards(seeded: Boolean)
}
