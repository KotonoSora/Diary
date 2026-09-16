package com.kotonosora.todolist.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotonosora.todolist.data.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val themeMode: StateFlow<String> = userPreferencesRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")

    val customStorageUri: StateFlow<String?> = userPreferencesRepository.customStorageFolderUri
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val defaultNoteFormat: StateFlow<String> = userPreferencesRepository.defaultNoteFormat
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "md")

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            userPreferencesRepository.saveThemeMode(mode)
        }
    }

    fun setCustomStorageUri(uri: String?) {
        viewModelScope.launch {
            userPreferencesRepository.saveCustomStorageFolderUri(uri)
        }
    }

    fun setDefaultNoteFormat(format: String) {
        viewModelScope.launch {
            userPreferencesRepository.saveDefaultNoteFormat(format)
        }
    }
}
