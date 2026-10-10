package com.kotonosora.todolist.data.mapper

import com.kotonosora.todolist.data.database.MediaEntity
import com.kotonosora.todolist.data.file.DeviceMediaItem
import com.kotonosora.todolist.domain.model.DeviceMediaFile
import com.kotonosora.todolist.domain.model.MediaItem

fun MediaEntity.toDomain(): MediaItem {
    return MediaItem(
        id = id,
        todoId = todoId,
        type = type,
        filePath = filePath,
        createdAt = createdAt
    )
}

fun MediaItem.toEntity(): MediaEntity {
    return MediaEntity(
        id = id,
        todoId = todoId,
        type = type,
        filePath = filePath,
        createdAt = createdAt
    )
}

fun DeviceMediaItem.toDomain(): DeviceMediaFile {
    return DeviceMediaFile(
        uri = uri,
        displayName = displayName,
        mimeType = mimeType,
        type = type,
        dateAddedSec = dateAddedSec,
        sizeBytes = sizeBytes,
        durationMs = durationMs
    )
}
