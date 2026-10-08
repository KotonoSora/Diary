package com.kotonosora.todolist.domain.usecase

import com.kotonosora.todolist.domain.model.DeviceMediaFile
import com.kotonosora.todolist.domain.model.MediaItem
import com.kotonosora.todolist.domain.model.MediaType
import com.kotonosora.todolist.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow

/**
 * Media bounded context — application layer (DDD).
 *
 * Owns the media index rows (`media_attachments`). File copying/deletion stays
 * with the caller (needs a `Context`); this layer validates type/path and
 * exposes index operations. ViewModels depend on [MediaUseCases], never on
 * [MediaRepository] directly. Mirrors the [TaskUseCases] bundle pattern.
 */

private val VALID_MEDIA_TYPES = setOf(
    MediaType.PHOTO,
    MediaType.VIDEO,
    MediaType.AUDIO,
    MediaType.PDF
)

class ObserveAllMediaUseCase(private val repository: MediaRepository) {
    operator fun invoke(): Flow<List<MediaItem>> = repository.allMedia()
}

class ObserveMediaForTodoUseCase(private val repository: MediaRepository) {
    operator fun invoke(todoId: String): Flow<List<MediaItem>> =
        repository.mediaForTodo(todoId)
}

class SnapshotMediaPathsUseCase(private val repository: MediaRepository) {
    suspend operator fun invoke(): Set<String> = repository.snapshotPaths()
}

class AddMediaUseCase(private val repository: MediaRepository) {
    suspend operator fun invoke(type: String, filePath: String, todoId: String? = null) {
        require(type in VALID_MEDIA_TYPES) { "Unknown media type: $type" }
        require(filePath.isNotBlank()) { "Media filePath must not be blank" }
        repository.addMedia(type, filePath, todoId)
    }
}

class RemoveMediaByPathUseCase(private val repository: MediaRepository) {
    suspend operator fun invoke(path: String) {
        if (path.isBlank()) return
        repository.removeByPath(path)
    }
}

class RemoveAllMediaForTodoUseCase(private val repository: MediaRepository) {
    suspend operator fun invoke(todoId: String) {
        if (todoId.isBlank()) return
        repository.removeAllForTodo(todoId)
    }
}

class QueryDeviceMediaUseCase(private val repository: MediaRepository) {
    suspend operator fun invoke(): List<DeviceMediaFile> = repository.queryDeviceMedia()
}

data class MediaUseCases(
    val observeAll: ObserveAllMediaUseCase,
    val observeForTodo: ObserveMediaForTodoUseCase,
    val snapshotPaths: SnapshotMediaPathsUseCase,
    val addMedia: AddMediaUseCase,
    val removeByPath: RemoveMediaByPathUseCase,
    val removeAllForTodo: RemoveAllMediaForTodoUseCase,
    val queryDeviceMedia: QueryDeviceMediaUseCase
) {
    companion object {
        fun from(repository: MediaRepository): MediaUseCases = MediaUseCases(
            observeAll = ObserveAllMediaUseCase(repository),
            observeForTodo = ObserveMediaForTodoUseCase(repository),
            snapshotPaths = SnapshotMediaPathsUseCase(repository),
            addMedia = AddMediaUseCase(repository),
            removeByPath = RemoveMediaByPathUseCase(repository),
            removeAllForTodo = RemoveAllMediaForTodoUseCase(repository),
            queryDeviceMedia = QueryDeviceMediaUseCase(repository)
        )
    }
}
