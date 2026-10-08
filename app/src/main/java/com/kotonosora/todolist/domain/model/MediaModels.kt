package com.kotonosora.todolist.domain.model

import android.net.Uri
import java.util.UUID

/**
 * Domain model for one vault media attachment (photo, video, audio or PDF).
 * `todoId` is null for standalone gallery items. Persistence lives in
 * `media_attachments`; the filesystem remains the source of truth.
 */
data class MediaItem(
    val id: String = UUID.randomUUID().toString(),
    val todoId: String? = null,
    /** `photo` | `video` | `audio` | `pdf` (matches `MediaFileManager` types). */
    val type: String,
    val filePath: String,
    val createdAt: Long = System.currentTimeMillis()
)

/** Media types stored in `media_attachments.type`. */
object MediaType {
    const val PHOTO = "photo"
    const val VIDEO = "video"
    const val AUDIO = "audio"
    const val PDF = "pdf"
}

/**
 * One on-device media file resolved through `MediaStore` (never cloud
 * libraries) for the in-app device picker. Uri-based: the file is copied
 * into the vault on import.
 */
data class DeviceMediaFile(
    val uri: Uri,
    val displayName: String,
    val mimeType: String?,
    /** `photo` | `video` | `audio` | `pdf` (matches [MediaItem.type]). */
    val type: String,
    val dateAddedSec: Long,
    val sizeBytes: Long,
    val durationMs: Long = 0L
)
