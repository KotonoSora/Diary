package com.kotonosora.todolist.feature.mood

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotonosora.todolist.domain.model.EmotionStamp
import com.kotonosora.todolist.domain.repository.VaultRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.ZoneId

data class MoodEntry(
    val noteId: String,
    val title: String,
    val emotion: EmotionStamp,
    val updatedAt: Long
)

data class MoodDay(
    val dayStartMillis: Long,
    val entries: List<MoodEntry>
)

class MoodTimelineViewModel(
    vaultRepository: VaultRepository
) : ViewModel() {

    val timeline: StateFlow<List<MoodDay>> = vaultRepository.getAllNotes()
        .map { notes ->
            notes.asSequence()
                .filter { it.emotion != null }
                .sortedByDescending { it.updatedAt }
                .groupBy { startOfDayMillis(it.updatedAt) }
                .map { (day, dayNotes) ->
                    MoodDay(
                        dayStartMillis = day,
                        entries = dayNotes.map { note ->
                            MoodEntry(
                                noteId = note.id,
                                title = note.title.ifBlank { note.id.substringAfterLast("/") },
                                emotion = note.emotion!!,
                                updatedAt = note.updatedAt
                            )
                        }
                    )
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    companion object {
        fun startOfDayMillis(epochMillis: Long): Long {
            val zone = ZoneId.systemDefault()
            return Instant.ofEpochMilli(epochMillis).atZone(zone)
                .toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()
        }
    }
}
