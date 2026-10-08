package com.kotonosora.todolist.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotonosora.todolist.domain.usecase.PreferencesUseCases
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * DDD: settings read/writes go through [PreferencesUseCases]
 * (application layer), never the repository directly.
 */
class SettingsViewModel(
    private val prefs: PreferencesUseCases
) : ViewModel() {

    val themeMode: StateFlow<String> = prefs.observeThemeMode()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")

    val customStorageUri: StateFlow<String?> = prefs.observeCustomStorageFolder()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val defaultNoteFormat: StateFlow<String> = prefs.observeDefaultNoteFormat()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "md")

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            prefs.saveThemeMode(mode)
        }
    }

    fun setCustomStorageUri(uri: String?) {
        viewModelScope.launch {
            prefs.saveCustomStorageFolder(uri)
        }
    }

    fun setDefaultNoteFormat(format: String) {
        viewModelScope.launch {
            prefs.saveDefaultNoteFormat(format)
        }
    }
}
