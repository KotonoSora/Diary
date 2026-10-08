package com.kotonosora.todolist.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kotonosora.todolist.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

class UserPreferencesRepository(
    private val context: Context
) : PreferencesRepository {

    private object PreferencesKeys {
        val CUSTOM_STORAGE_FOLDER_URI = stringPreferencesKey("custom_storage_folder_uri")
        val THEME_MODE = stringPreferencesKey("theme_mode") // "system", "dark", "light"
        val DEFAULT_NOTE_FORMAT = stringPreferencesKey("default_note_format") // "md", "txt"
        val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
        val HAS_SEEDED_FLASHCARDS = booleanPreferencesKey("has_seeded_flashcards")
    }

    override val customStorageFolderUri: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.CUSTOM_STORAGE_FOLDER_URI]
    }

    override val themeMode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.THEME_MODE] ?: "system"
    }

    override val defaultNoteFormat: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DEFAULT_NOTE_FORMAT] ?: "md"
    }

    override val hasCompletedOnboarding: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.HAS_COMPLETED_ONBOARDING] ?: false
    }

    override val hasSeededFlashcards: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.HAS_SEEDED_FLASHCARDS] ?: false
    }

    override suspend fun saveCustomStorageFolderUri(uri: String?) {
        context.dataStore.edit { preferences ->
            if (uri.isNullOrBlank()) {
                preferences.remove(PreferencesKeys.CUSTOM_STORAGE_FOLDER_URI)
            } else {
                preferences[PreferencesKeys.CUSTOM_STORAGE_FOLDER_URI] = uri
            }
        }
    }

    override suspend fun saveThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode
        }
    }

    override suspend fun saveDefaultNoteFormat(format: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_NOTE_FORMAT] = format
        }
    }

    override suspend fun saveHasCompletedOnboarding(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAS_COMPLETED_ONBOARDING] = completed
        }
    }

    override suspend fun saveHasSeededFlashcards(seeded: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAS_SEEDED_FLASHCARDS] = seeded
        }
    }
}
