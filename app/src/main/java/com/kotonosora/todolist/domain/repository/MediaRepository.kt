package com.kotonosora.todolist.domain.repository

import com.kotonosora.todolist.domain.model.DeviceMediaFile
import com.kotonosora.todolist.domain.model.MediaItem
import kotlinx.coroutines.flow.Flow

/**
 * Vault media index (photos, videos, audio, PDFs) plus the on-device
 * `MediaStore` listing used by the in-app picker. Implemented by
 * `MediaRepositoryImpl` over Room + `DeviceMediaStore`; ViewModels depend on
 * this interface, never on the DAO or entities directly. File copying and
 * deletion stay with the caller (they need a `Context`); this interface owns
 * the index rows.
 */
interface MediaRepository {
    fun allMedia(): Flow<List<MediaItem>>

    fun mediaForTodo(todoId: String): Flow<List<MediaItem>>

    /** Direct index snapshot for cold-start merges (bypasses in-flight flows). */
    suspend fun snapshotPaths(): Set<String>

    suspend fun addMedia(type: String, filePath: String, todoId: String? = null)

    suspend fun removeByPath(path: String)

    suspend fun removeAllForTodo(todoId: String)

    /** Blocking `MediaStore` I/O — call off Main. Newest-first. */
    suspend fun queryDeviceMedia(): List<DeviceMediaFile>
}
