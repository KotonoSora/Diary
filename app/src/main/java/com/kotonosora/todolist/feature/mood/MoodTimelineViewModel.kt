package com.kotonosora.todolist.feature.mood

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotonosora.todolist.domain.model.EmotionStamp
import com.kotonosora.todolist.domain.model.MoodEntry
import com.kotonosora.todolist.domain.usecase.MoodUseCases
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

/**
 * Presentation model for one timeline row. Named to not collide with the
 * domain [MoodEntry].
 */
data class MoodTimelineEntry(
    val id: String,
    val emotion: EmotionStamp,
    val note: String,
    val linkedNoteId: String?,
    val createdAt: Long
)

data class MoodDay(
    val dayStartMillis: Long,
    val entries: List<MoodTimelineEntry>
)

data class MoodUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * DDD: mood CRUD via [MoodUseCases] only. Day-grouping is presentation logic
 * and stays here. Updates always start from a loaded entity so `createdAt`
 * (and therefore the day group) never changes on edit.
 */
class MoodTimelineViewModel(
    private val useCases: MoodUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(MoodUiState())
    val uiState: StateFlow<MoodUiState> = _uiState.asStateFlow()

    val timeline: StateFlow<List<MoodDay>> = useCases.observeMoods()
        .map { moods ->
            moods.groupBy { startOfDayMillis(it.createdAt) }
                .map { (day, dayMoods) ->
                    MoodDay(
                        dayStartMillis = day,
                        entries = dayMoods.map { mood ->
                            MoodTimelineEntry(
                                id = mood.id,
                                emotion = mood.emotion,
                                note = mood.note,
                                linkedNoteId = mood.linkedNoteId,
                                createdAt = mood.createdAt
                            )
                        }
                    )
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun logMood(emotion: EmotionStamp, note: String) = viewModelScope.launch {
        runOp { useCases.logMood(MoodEntry(emotion = emotion, note = note.trim())) }
    }

    fun updateMood(entry: MoodEntry) = viewModelScope.launch {
        runOp { useCases.updateMood(entry) }
    }

    fun deleteMoodById(id: String) = viewModelScope.launch {
        runOp { useCases.deleteMoodById(id) }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private suspend fun runOp(block: suspend () -> Unit) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        try {
            block()
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(error = e.message ?: "Mood operation failed")
        } finally {
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    companion object {
        fun startOfDayMillis(epochMillis: Long): Long {
            val zone = ZoneId.systemDefault()
            return Instant.ofEpochMilli(epochMillis).atZone(zone)
                .toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()
        }
    }
}
