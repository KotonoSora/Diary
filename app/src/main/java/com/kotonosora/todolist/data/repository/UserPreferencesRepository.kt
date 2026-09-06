package com.kotonosora.todolist.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

@Singleton
class UserPreferencesRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    private object PreferencesKeys {
        val CUSTOM_STORAGE_FOLDER_URI = stringPreferencesKey("custom_storage_folder_uri")
    }

    val customStorageFolderUri: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.CUSTOM_STORAGE_FOLDER_URI]
    }

    suspend fun saveCustomStorageFolderUri(uri: String?) {
        context.dataStore.edit { preferences ->
            if (uri.isNullOrBlank()) {
                preferences.remove(PreferencesKeys.CUSTOM_STORAGE_FOLDER_URI)
            } else {
                preferences[PreferencesKeys.CUSTOM_STORAGE_FOLDER_URI] = uri
            }
        }
    }
}
