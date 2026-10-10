package com.kotonosora.todolist.data.repository

import android.content.Context
import com.kotonosora.todolist.data.database.MediaDao
import com.kotonosora.todolist.data.file.DeviceMediaStore
import com.kotonosora.todolist.data.mapper.toDomain
import com.kotonosora.todolist.data.mapper.toEntity
import com.kotonosora.todolist.domain.model.DeviceMediaFile
import com.kotonosora.todolist.domain.model.MediaItem
import com.kotonosora.todolist.domain.repository.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class MediaRepositoryImpl(
    private val context: Context,
    private val mediaDao: MediaDao
) : MediaRepository {

    // In-memory filePath -> item for the delete fallback below.
    private val indexCache = mutableMapOf<String, MediaItem>()

    override fun allMedia(): Flow<List<MediaItem>> {
        return mediaDao.getAllMedia().map { entities ->
            indexCache.clear()
            entities.forEach { indexCache[it.filePath] = it.toDomain() }
            entities.map { it.toDomain() }
        }
    }

    override fun mediaForTodo(todoId: String): Flow<List<MediaItem>> {
        return mediaDao.getMediaForTodo(todoId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun snapshotPaths(): Set<String> = withContext(Dispatchers.IO) {
        try {
            mediaDao.getAllMedia().firstOrNull()?.map { it.filePath }?.toSet()
                ?: emptySet()
        } catch (e: Exception) {
            e.printStackTrace()
            emptySet()
        }
    }

    override suspend fun addMedia(type: String, filePath: String, todoId: String?) =
        withContext(Dispatchers.IO) {
            val item = MediaItem(todoId = todoId, type = type, filePath = filePath)
            try {
                mediaDao.insertMedia(item.toEntity())
            } catch (e: Exception) {
                android.util.Log.w("MediaRepository", "insert $type failed", e)
            }
            indexCache[filePath] = item
        }

    override suspend fun removeByPath(path: String) {
        withContext(Dispatchers.IO) {
            val removed = indexCache.remove(path)
            try {
                mediaDao.deleteByPath(path)
            } catch (e: Exception) {
                android.util.Log.w("MediaRepository", "deleteByPath failed, fallback", e)
                removed?.let {
                    try {
                        mediaDao.deleteMedia(it.toEntity())
                    } catch (e2: Exception) {
                        android.util.Log.w("MediaRepository", "deleteMedia fallback failed", e2)
                    }
                }
            }
        }
    }

    override suspend fun removeAllForTodo(todoId: String) = withContext(Dispatchers.IO) {
        mediaDao.deleteAllMediaForTodo(todoId)
    }

    override suspend fun queryDeviceMedia(): List<DeviceMediaFile> =
        withContext(Dispatchers.IO) {
            try {
                DeviceMediaStore.queryDeviceMedia(context).map { it.toDomain() }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
}
