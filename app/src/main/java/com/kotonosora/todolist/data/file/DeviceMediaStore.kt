package com.kotonosora.todolist.data.file

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore

/**
 * A photo / video / audio file physically stored on this device, resolved
 * through `MediaStore` — which by definition never includes cloud libraries
 * (Google Photos cloud, Drive, …). This is the sole source for the in-app
 * device-media picker, so Google Photos can never appear in the UI.
 */
data class DeviceMediaItem(
    val uri: Uri,
    val displayName: String,
    val mimeType: String?,
    /** `photo` | `video` | `audio` | `pdf` (matches `media_attachments.type`). */
    val type: String,
    val dateAddedSec: Long,
    val sizeBytes: Long,
    val durationMs: Long = 0L
)

object DeviceMediaStore {

    /**
     * Lists on-device photos, videos and audio newest-first.
     * Blocking `ContentResolver` I/O — call off Main.
     */
    fun queryDeviceMedia(context: Context): List<DeviceMediaItem> {
        val out = mutableListOf<DeviceMediaItem>()
        val resolver = context.contentResolver
        queryImages(resolver, out)
        queryVideos(resolver, out)
        queryAudios(resolver, out)
        queryPdfs(resolver, out)
        return out.sortedByDescending { it.dateAddedSec }
    }

    private fun queryImages(
        resolver: android.content.ContentResolver,
        out: MutableList<DeviceMediaItem>
    ) {
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.SIZE
        )
        queryCollection(
            resolver,
            collection,
            projection,
            sortOrder()
        ) { id, name, mime, added, size, _ ->
            out += DeviceMediaItem(
                uri = ContentUris.withAppendedId(collection, id),
                displayName = name,
                mimeType = mime,
                type = "photo",
                dateAddedSec = added,
                sizeBytes = size
            )
        }
    }

    private fun queryVideos(
        resolver: android.content.ContentResolver,
        out: MutableList<DeviceMediaItem>
    ) {
        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DURATION
        )
        queryCollection(
            resolver,
            collection,
            projection,
            sortOrder()
        ) { id, name, mime, added, size, duration ->
            out += DeviceMediaItem(
                uri = ContentUris.withAppendedId(collection, id),
                displayName = name,
                mimeType = mime,
                type = "video",
                dateAddedSec = added,
                sizeBytes = size,
                durationMs = duration
            )
        }
    }

    private fun queryAudios(
        resolver: android.content.ContentResolver,
        out: MutableList<DeviceMediaItem>
    ) {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DURATION
        )
        // Skip system sounds (alarms/ringtones/notifications) so the picker
        // lists user audio only — music, podcasts, voice recordings.
        val selection = "${MediaStore.Audio.Media.IS_ALARM} = 0 AND " +
                "${MediaStore.Audio.Media.IS_RINGTONE} = 0 AND " +
                "${MediaStore.Audio.Media.IS_NOTIFICATION} = 0"
        queryCollection(
            resolver,
            collection,
            projection,
            sortOrder(),
            selection = selection
        ) { id, name, mime, added, size, duration ->
            out += DeviceMediaItem(
                uri = ContentUris.withAppendedId(collection, id),
                displayName = name,
                mimeType = mime,
                type = "audio",
                dateAddedSec = added,
                sizeBytes = size,
                durationMs = duration
            )
        }
    }

    private fun queryPdfs(
        resolver: android.content.ContentResolver,
        out: MutableList<DeviceMediaItem>
    ) {
        val collection = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.SIZE
        )
        queryCollection(
            resolver,
            collection,
            projection,
            sortOrder(),
            selection = "${MediaStore.Files.FileColumns.MIME_TYPE} = ?",
            selectionArgs = arrayOf("application/pdf")
        ) { id, name, mime, added, size, _ ->
            out += DeviceMediaItem(
                uri = ContentUris.withAppendedId(collection, id),
                displayName = name,
                mimeType = mime,
                type = "pdf",
                dateAddedSec = added,
                sizeBytes = size
            )
        }
    }

    private fun sortOrder(): String = "${MediaStore.MediaColumns.DATE_ADDED} DESC"

    private inline fun queryCollection(
        resolver: android.content.ContentResolver,
        collection: Uri,
        projection: Array<String>,
        sortOrder: String,
        selection: String? = null,
        selectionArgs: Array<String>? = null,
        onRow: (id: Long, name: String, mime: String?, added: Long, size: Long, duration: Long) -> Unit
    ) {
        try {
            resolver.query(collection, projection, selection, selectionArgs, sortOrder)
                ?.use { cursor ->

                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                    val mimeCol = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
                    val addedCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED)
                    val sizeCol = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
                    val durationCol = cursor.getColumnIndex(MediaStore.MediaColumns.DURATION)
                    while (cursor.moveToNext()) {
                        val name = cursor.getString(nameCol) ?: continue
                        onRow(
                            cursor.getLong(idCol),
                            name,
                            mimeCol.takeIf { it != -1 }?.let { cursor.getString(it) },
                            addedCol.takeIf { it != -1 }?.let { cursor.getLong(it) } ?: 0L,
                            sizeCol.takeIf { it != -1 }?.let { cursor.getLong(it) } ?: 0L,
                            durationCol.takeIf { it != -1 }?.let { cursor.getLong(it) } ?: 0L
                        )
                    }
                }
        } catch (_: Exception) {
            // A single failing collection must not hide the others.
        }
    }
}
