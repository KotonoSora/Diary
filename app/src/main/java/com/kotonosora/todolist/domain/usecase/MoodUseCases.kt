package com.kotonosora.todolist.domain.usecase

import com.kotonosora.todolist.domain.model.MoodEntry
import com.kotonosora.todolist.domain.repository.MoodRepository
import kotlinx.coroutines.flow.Flow

/**
 * Mood bounded context — application layer (DDD).
 *
 * Owns the standalone mood log (`mood_entries`). The note text is optional
 * (emotion-only logging is valid); only the timestamp is normalized here.
 * ViewModels depend on [MoodUseCases], never on [MoodRepository] directly.
 * Mirrors the [TaskUseCases] bundle pattern.
 */
class ObserveMoodsUseCase(private val repository: MoodRepository) {
    operator fun invoke(): Flow<List<MoodEntry>> = repository.observeMoods()
}

class LogMoodUseCase(private val repository: MoodRepository) {
    suspend operator fun invoke(mood: MoodEntry) {
        val normalized = mood.copy(
            id = mood.id.ifBlank { java.util.UUID.randomUUID().toString() },
            createdAt = if (mood.createdAt <= 0) {
                System.currentTimeMillis()
            } else {
                mood.createdAt
            }
        )
        repository.insertMood(normalized)
    }
}

class UpdateMoodUseCase(private val repository: MoodRepository) {
    suspend operator fun invoke(mood: MoodEntry) {
        require(mood.id.isNotBlank()) { "Mood id must not be blank" }
        val normalized = if (mood.createdAt <= 0) {
            mood.copy(createdAt = System.currentTimeMillis())
        } else {
            mood
        }
        repository.updateMood(normalized)
    }
}

class DeleteMoodUseCase(private val repository: MoodRepository) {
    suspend operator fun invoke(mood: MoodEntry) = repository.deleteMood(mood)
}

class DeleteMoodByIdUseCase(private val repository: MoodRepository) {
    suspend operator fun invoke(id: String) {
        if (id.isBlank()) return
        repository.deleteMoodById(id)
    }
}

data class MoodUseCases(
    val observeMoods: ObserveMoodsUseCase,
    val logMood: LogMoodUseCase,
    val updateMood: UpdateMoodUseCase,
    val deleteMood: DeleteMoodUseCase,
    val deleteMoodById: DeleteMoodByIdUseCase
) {
    companion object {
        fun from(repository: MoodRepository): MoodUseCases = MoodUseCases(
            observeMoods = ObserveMoodsUseCase(repository),
            logMood = LogMoodUseCase(repository),
            updateMood = UpdateMoodUseCase(repository),
            deleteMood = DeleteMoodUseCase(repository),
            deleteMoodById = DeleteMoodByIdUseCase(repository)
        )
    }
}
