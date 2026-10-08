package com.kotonosora.todolist.data.file

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile
import com.kotonosora.todolist.common.AppConstants
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class MediaOutputLocation {
    data class LocalFile(val file: File) : MediaOutputLocation()
    data class DocumentFileUri(
        val documentFile: DocumentFile,
        val uri: Uri,
        val pathString: String
    ) : MediaOutputLocation()
}

/**
 * Vault-relative media file: [pathString] is either a `content://` SAF uri
 * string or an absolute local path. [type] matches `media_attachments.type`
 * (`photo` / `video` / `audio` / `pdf`).
 */
data class VaultMediaFile(
    val pathString: String,
    val type: String,
    val displayName: String
)

class MediaFileManager(
    private val context: Context
) {

    private fun generateTimestamp(): String {
        return SimpleDateFormat("yyyyMMdd_HHmmss_SSS", AppConstants.APP_LOCALE).format(Date())
    }

    private fun vaultTree(customFolderUriStr: String?): DocumentFile? {
        if (customFolderUriStr.isNullOrBlank()) return null
        return try {
            val treeUri = Uri.parse(customFolderUriStr)
            val treeFile = DocumentFile.fromTreeUri(context, treeUri)
            if (treeFile != null && treeFile.canWrite()) treeFile else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun findOrCreateSafDir(root: DocumentFile, dirName: String): DocumentFile? {
        return try {
            val existing = root.findFile(dirName)
            if (existing != null && existing.isDirectory) {
                existing
            } else {
                root.createDirectory(dirName)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /** Local vault root = same dir [VaultManager.getDefaultStorageDir] uses. */
    private fun localVaultRoot(): File {
        return (context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: context.filesDir).also { it.mkdirs() }
    }

    private fun localTypeDir(dirName: String): File {
        return File(localVaultRoot(), dirName).also { it.mkdirs() }
    }

    /**
     * Returns a non-overwriting File in [dir]: when two imports land in the
     * same millisecond with the same sanitized name, appends `_1`, `_2`, …
     * before the extension instead of silently overwriting. SAF needs no
     * equivalent — `createFile()` auto-dedupes.
     */
    private fun uniqueLocalFile(dir: File, fileName: String): File {
        var candidate = File(dir, fileName)
        if (!candidate.exists()) return candidate
        val stem = fileName.substringBeforeLast(".", fileName)
        val ext = fileName.substringAfterLast(".", "")
        val suffix = if (ext.isNotEmpty() && "." in fileName) ".$ext" else ""
        var index = 1
        while (candidate.exists() && index < 1000) {
            candidate = File(dir, "${stem}_$index$suffix")
            index++
        }
        return candidate
    }

    private fun createSafOutput(
        customFolderUriStr: String?,
        dirName: String,
        fileName: String,
        mimeType: String
    ): MediaOutputLocation.DocumentFileUri? {
        val root = vaultTree(customFolderUriStr) ?: return null
        return try {
            val dir = findOrCreateSafDir(root, dirName) ?: root
            val docFile = dir.createFile(mimeType, fileName) ?: return null
            MediaOutputLocation.DocumentFileUri(
                documentFile = docFile,
                uri = docFile.uri,
                pathString = docFile.uri.toString()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun createPhotoOutputLocation(customFolderUriStr: String?): MediaOutputLocation {
        val fileName = "IMG_${generateTimestamp()}.jpg"
        createSafOutput(customFolderUriStr, PHOTOS_DIR, fileName, "image/jpeg")?.let { return it }

        val dir = localTypeDir(PHOTOS_DIR)
        return MediaOutputLocation.LocalFile(File(dir, fileName))
    }

    fun createVideoOutputLocation(customFolderUriStr: String?): MediaOutputLocation {
        val fileName = "VID_${generateTimestamp()}.mp4"
        createSafOutput(customFolderUriStr, VIDEOS_DIR, fileName, "video/mp4")?.let { return it }

        val dir = localTypeDir(VIDEOS_DIR)
        return MediaOutputLocation.LocalFile(File(dir, fileName))
    }

    fun createAudioOutputLocation(customFolderUriStr: String?): MediaOutputLocation {
        val fileName = "AUD_${generateTimestamp()}.m4a"
        createSafOutput(customFolderUriStr, AUDIOS_DIR, fileName, "audio/mp4")?.let { return it }

        val dir = localTypeDir(AUDIOS_DIR)
        return MediaOutputLocation.LocalFile(File(dir, fileName))
    }

    /**
     * Generic vault import for on-device media picked through the in-app
     * device picker (`content://media/...` from `MediaStore`).
     *
     * - Resolves the display name + MIME via ContentResolver (caller must be off Main).
     * - Detects the media type from the filename first, then the MIME as fallback.
     * - Routes into the per-type vault subfolder (`photos/` `videos/` `audios/`
     *   `documents/`) with a timestamped, sanitized name that preserves the
     *   original extension (important for the expanded audio set).
     * - Streams with a 64 KB buffer so large videos/audios never load fully
     *   into memory; deletes the pre-created stub when the copy fails.
     *
     * Returns the output location, or null when the type is unsupported or the
     * copy fails. Blocking I/O — call off Main.
     */
    fun importMediaDocument(
        sourceUri: Uri,
        displayName: String?,
        mimeType: String?,
        customFolderUriStr: String?,
        fallbackType: String? = null
    ): MediaOutputLocation? {
        val resolvedName = displayName ?: queryDisplayName(sourceUri)
        val resolvedMime = mimeType ?: queryMimeType(sourceUri)
        var type = typeForFileName(resolvedName)
            ?: mimeTypeToMediaType(resolvedMime)
            ?: fallbackType
            ?: typeForFileName(inferNameFromMime(resolvedName, resolvedMime))
            ?: return null

        // A generic "*/*" pick can hand us a PDF with a misleading name —
        // trust an explicit PDF mime over the filename guess.
        if (resolvedMime.equals("application/pdf", ignoreCase = true)) type = "pdf"

        val dirName = targetDirForType(type) ?: return null
        // Prefer the extension-derived MIME when the resolver MIME is generic
        // (octet-stream) or disagrees with the detected type — e.g. a .opus
        // reported as application/octet-stream, or a .jpg misreported as
        // video/mp4. SAF createFile() uses this MIME for external handlers.
        val extMime = mimeTypeForType(type, resolvedName)
        val cleanResolved = resolvedMime?.substringBefore(";")?.trim()
            ?.takeIf { it.isNotBlank() }
        val outputMime = if (cleanResolved == null) {
            extMime
        } else {
            val lower = cleanResolved.lowercase(AppConstants.APP_LOCALE)
            val isGeneric = lower in setOf(
                "application/octet-stream",
                "application/binary",
                "binary/octet-stream",
                "*/*"
            )
            val resolvedType = mimeTypeToMediaType(lower)
            if (isGeneric || (resolvedType != null && resolvedType != type)) extMime
            else cleanResolved
        }
        val fileName = buildImportFileName(type, resolvedName, resolvedMime)

        // SAF tree first, local vault fallback.
        val safOutput = createSafOutput(customFolderUriStr, dirName, fileName, outputMime)
        if (safOutput != null) {
            return try {
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    context.contentResolver.openOutputStream(safOutput.uri)?.use { output ->
                        copyBuffered(input, output)
                    } ?: run {
                        safOutput.documentFile.delete()
                        return null
                    }
                } ?: run {
                    safOutput.documentFile.delete()
                    return null
                }
                // Treat 0-byte copies as failures (matches the local branch) so
                // empty stubs are never indexed.
                if (safOutput.documentFile.length() == 0L) {
                    try {
                        safOutput.documentFile.delete()
                    } catch (_: Exception) {
                    }
                    return null
                }
                safOutput
            } catch (e: Exception) {
                e.printStackTrace()
                try {
                    safOutput.documentFile.delete()
                } catch (_: Exception) {
                }
                null
            }
        }

        val dir = localTypeDir(dirName)
        val file = uniqueLocalFile(dir, fileName)
        return try {
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                file.outputStream().use { output -> copyBuffered(input, output) }
            } ?: run {
                try {
                    if (file.exists()) file.delete()
                } catch (_: Exception) {
                }
                return null
            }
            if (!file.exists() || file.length() == 0L) {
                try {
                    if (file.exists()) file.delete()
                } catch (_: Exception) {
                }
                return null
            }
            MediaOutputLocation.LocalFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                if (file.exists()) file.delete()
            } catch (_: Exception) {
            }
            null
        }
    }

    /**
     * Convenience overload that queries the display name + MIME from the
     * ContentResolver itself. Blocking I/O — call off Main.
     */
    fun importMediaDocument(
        sourceUri: Uri,
        customFolderUriStr: String?
    ): MediaOutputLocation? {
        return importMediaDocument(
            sourceUri = sourceUri,
            displayName = queryDisplayName(sourceUri),
            mimeType = queryMimeType(sourceUri),
            customFolderUriStr = customFolderUriStr
        )
    }

    fun queryDisplayName(uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun queryMimeType(uri: Uri): String? {
        return try {
            context.contentResolver.getType(uri)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * True when the custom SAF tree is present and readable. A vault folder
     * whose permission was lost/revoked must never trigger index pruning —
     * callers keep the DB index as-is instead.
     */
    fun isTreeReadable(customFolderUriStr: String): Boolean {
        return try {
            val tree = DocumentFile.fromTreeUri(context, Uri.parse(customFolderUriStr))
            tree != null && tree.canRead()
        } catch (_: Exception) {
            false
        }
    }

    /**
     * True when a stored media path still exists (SAF `content://` uri or
     * absolute local path). Returns true when existence can't be determined
     * (e.g. revoked SAF permission) so callers conservatively keep the row
     * instead of wiping the index.
     */
    fun storedFileExists(path: String): Boolean {
        return try {
            if (path.startsWith("content://")) {
                DocumentFile.fromSingleUri(context, Uri.parse(path))?.exists() == true
            } else {
                File(path).exists()
            }
        } catch (_: Exception) {
            true
        }
    }

    /**
     * Deletes one stored media file. SAF tree documents go through
     * DocumentFile (resolver delete alone returns 0 and leaves the file
     * behind, which a later vault scan would re-index as a ghost).
     */
    fun deleteStoredFile(path: String) {
        try {
            if (path.startsWith("content://")) {
                try {
                    val doc = DocumentFile.fromSingleUri(context, Uri.parse(path))
                    if (doc != null && doc.delete()) return
                } catch (_: Exception) {
                }
                try {
                    context.contentResolver.delete(Uri.parse(path), null, null)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                File(path).delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Deletes a pre-created capture output (empty stub cleanup). */
    fun deleteLocation(location: MediaOutputLocation) {
        try {
            when (location) {
                is MediaOutputLocation.DocumentFileUri -> location.documentFile.delete()
                is MediaOutputLocation.LocalFile -> {
                    if (location.file.exists()) location.file.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun copyBuffered(input: InputStream, output: OutputStream) {
        val buffer = ByteArray(IMPORT_BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read == -1) break
            if (read <= 0) continue
            output.write(buffer, 0, read)
        }
        output.flush()
    }

    /**
     * Lists every media asset physically present in the chosen vault folder:
     * SAF tree (root + per-type subfolders, one level deep) or the local vault
     * root (+ per-type subfolders). Runs blocking I/O — call off Main.
     */
    fun listVaultMedia(customFolderUriStr: String?): List<VaultMediaFile> {
        val root = vaultTree(customFolderUriStr)
        if (root != null) {
            return listSafVaultMedia(root)
        }
        return listLocalVaultMedia()
    }

    private fun listSafVaultMedia(root: DocumentFile): List<VaultMediaFile> {
        val out = mutableListOf<VaultMediaFile>()
        try {
            val topLevel = root.listFiles()
            // Root-level files (captures stored directly in the vault root).
            for (file in topLevel) {
                if (file.isFile) {
                    toVaultMediaFile(file.name, file.uri.toString())?.let { out += it }
                }
            }
            // Per-type subfolders (photos/videos/audios/documents) plus any
            // user-created dir, one level deep, so moved media still shows up.
            // `_assets`, dot-dirs and `.templates` are notes infra, not media.
            for (file in topLevel) {
                if (!file.isDirectory) continue
                val name = file.name ?: continue
                if (name.startsWith(".") || name == "_assets") continue
                try {
                    for (child in file.listFiles()) {
                        if (!child.isFile) continue
                        toVaultMediaFile(child.name, child.uri.toString())?.let { out += it }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return out.distinctBy { it.pathString }
            .sortedBy { it.displayName.lowercase(AppConstants.APP_LOCALE) }
    }

    private fun listLocalVaultMedia(): List<VaultMediaFile> {
        val out = mutableListOf<VaultMediaFile>()
        val roots = mutableListOf<File>()
        try {
            val vaultRoot = localVaultRoot()
            roots += vaultRoot
            for (dirName in MEDIA_DIRS) {
                val sub = File(vaultRoot, dirName)
                if (sub.isDirectory) roots += sub
            }
            val seen = mutableSetOf<String>()
            for (root in roots.distinctBy {
                try {
                    it.canonicalPath
                } catch (_: Exception) {
                    it.absolutePath
                }
            }) {
                val files = try {
                    root.listFiles() ?: continue
                } catch (_: Exception) {
                    continue
                }
                for (file in files) {
                    if (!file.isFile) continue
                    val canonical = try {
                        file.canonicalPath
                    } catch (_: Exception) {
                        file.absolutePath
                    }
                    if (!seen.add(canonical)) continue
                    toVaultMediaFile(file.name, file.absolutePath)?.let { out += it }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return out.distinctBy { it.pathString }
            .sortedBy { it.displayName.lowercase(AppConstants.APP_LOCALE) }
    }

    companion object {
        const val PHOTOS_DIR = "photos"
        const val VIDEOS_DIR = "videos"
        const val AUDIOS_DIR = "audios"
        const val DOCUMENTS_DIR = "documents"
        const val TEMPLATES_DIR = ".templates"

        val MEDIA_DIRS = listOf(PHOTOS_DIR, VIDEOS_DIR, AUDIOS_DIR, DOCUMENTS_DIR)

        /** 64 KB streaming buffer for vault imports (large video/audio safe). */
        const val IMPORT_BUFFER_SIZE = 64 * 1024

        private val PHOTO_EXTS = setOf(
            "jpg", "jpeg", "png", "webp", "gif", "bmp",
            "heic", "heif", "avif", "tif", "tiff", "svg", "ico", "dng"
        )
        private val VIDEO_EXTS = setOf(
            "mp4", "m4v", "mov", "3gp", "3g2", "webm", "mkv",
            "ts", "m2ts", "mts", "mpg", "mpeg", "mpe", "ogv", "flv"
        )

        /**
         * Every audio container/codec the app can index and attempt playback:
         * platform MediaCodec + ExoPlayer extractors (MP3/AAC/Vorbis/Opus/FLAC/
         * WAV/AMR/MIDI-family). Undecodable files still import but surface an
         * inline playback error instead of crashing.
         */
        private val AUDIO_EXTS = setOf(
            "m4a", "mp3", "mp2", "mp1", "aac", "adts", "ac3", "eac3", "dts",
            "ogg", "oga", "opus", "weba", "wav", "wave", "flac", "alac",
            "amr", "awb", "mid", "midi", "xmf", "mxmf", "rtttl", "rtx",
            "ota", "imy", "3ga", "mka", "aiff", "aif", "aifc", "wma"
        )
        private const val PDF_EXT = "pdf"

        fun isMediaDir(name: String): Boolean = MEDIA_DIRS.contains(name)

        /**
         * Maps a filename to its vault media type (`photo`/`video`/`audio`/`pdf`),
         * or null when the extension is not a media asset. English-locale
         * lowercasing avoids Turkish-`i` style mismatches on any device.
         */
        fun typeForFileName(fileName: String?): String? {
            if (fileName.isNullOrBlank()) return null
            return when (fileName.substringAfterLast(".", "").lowercase(AppConstants.APP_LOCALE)) {
                in PHOTO_EXTS -> "photo"
                in VIDEO_EXTS -> "video"
                in AUDIO_EXTS -> "audio"
                PDF_EXT -> "pdf"
                else -> null
            }
        }

        /**
         * MIME fallback when the picked file has no (or a misleading) extension —
         * e.g. Drive/WhatsApp shares. Covers image, video and audio MIME families
         * plus the common OGG/FLAC/MIDI application mimes.
         */
        fun mimeTypeToMediaType(mimeType: String?): String? {
            if (mimeType.isNullOrBlank()) return null
            val mime = mimeType.lowercase(AppConstants.APP_LOCALE).substringBefore(";").trim()
            return when {
                mime.startsWith("image/") -> "photo"
                mime.startsWith("video/") -> "video"
                mime.startsWith("audio/") -> "audio"
                mime in setOf(
                    "application/ogg", "application/x-ogg",
                    "application/flac", "application/x-flac",
                    "audio/midi", "audio/x-midi",
                    "application/midi", "application/x-midi"
                ) -> "audio"

                mime == "application/pdf" -> "pdf"
                else -> null
            }
        }

        fun targetDirForType(type: String): String? {
            return when (type) {
                "photo" -> PHOTOS_DIR
                "video" -> VIDEOS_DIR
                "audio" -> AUDIOS_DIR
                "pdf" -> DOCUMENTS_DIR
                else -> null
            }
        }

        fun mimeTypeForType(type: String, fileName: String?): String {
            // Prefer the real extension mapping so the vault file opens in
            // external viewers with the right handler.
            fileName?.substringAfterLast(".", "")?.takeIf { it.isNotBlank() }?.let { ext ->
                val fromExt = try {
                    MimeTypeMap.getSingleton()
                        .getMimeTypeFromExtension(ext.lowercase(AppConstants.APP_LOCALE))
                } catch (_: Exception) {
                    null
                }
                if (!fromExt.isNullOrBlank()) return fromExt
            }
            return when (type) {
                "photo" -> "image/jpeg"
                "video" -> "video/mp4"
                "audio" -> "audio/mpeg"
                else -> "application/pdf"
            }
        }

        internal fun toVaultMediaFile(
            fileName: String?,
            pathString: String
        ): VaultMediaFile? {
            if (fileName.isNullOrBlank()) return null
            val type = typeForFileName(fileName) ?: return null
            return VaultMediaFile(
                pathString = pathString,
                type = type,
                displayName = fileName
            )
        }

        /**
         * Strips a picker-provided display name down to a safe filename stem
         * (no separators, no `.pdf` duplication, capped length).
         */
        fun sanitizePdfFileName(displayName: String?): String {
            val stem = (displayName?.substringAfterLast("/")?.substringAfterLast("\\")
                ?.substringAfterLast(":")
                ?.removeSuffix(".pdf")?.removeSuffix(".PDF")
                ?.filter { it.isLetterOrDigit() || it == '-' || it == '_' || it == ' ' }
                ?.trim()
                .takeUnless { it.isNullOrEmpty() } ?: "document")
            return stem.replace("\\s+".toRegex(), "_").take(60)
        }

        /**
         * Sanitizes any picked display name into a safe stem while preserving
         * its original extension (critical for the expanded audio set — a
         * `.opus`/`.flac`/`.mid` must keep its suffix to stay playable).
         */
        fun sanitizeImportedFileName(displayName: String?, fallbackExt: String): String {
            val raw = displayName?.substringAfterLast("/")?.substringAfterLast("\\")
                ?.substringAfterLast(":")?.trim().takeUnless { it.isNullOrEmpty() }
                ?: "file.$fallbackExt"
            val ext = raw.substringAfterLast(".", fallbackExt)
                .lowercase(AppConstants.APP_LOCALE)
                .filter { it.isLetterOrDigit() }
                .take(10)
                .takeIf { it.isNotEmpty() } ?: fallbackExt.lowercase(AppConstants.APP_LOCALE)
            val stemBase = if ("." in raw) raw.substringBeforeLast(".") else raw
            val stem = stemBase
                .filter { it.isLetterOrDigit() || it == '-' || it == '_' || it == ' ' }
                .trim()
                .takeIf { it.isNotEmpty() } ?: "file"
            return stem.replace("\\s+".toRegex(), "_").take(60) + "." + ext
        }

        internal fun buildImportFileName(
            type: String,
            displayName: String?,
            mimeType: String?
        ): String {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", AppConstants.APP_LOCALE)
                .format(Date())
            val prefix = when (type) {
                "photo" -> "IMG"
                "video" -> "VID"
                "audio" -> "AUD"
                else -> "DOC"
            }
            val fallbackExt = when (type) {
                "photo" -> "jpg"
                "video" -> "mp4"
                "audio" -> "m4a"
                else -> extensionForMime(mimeType) ?: "pdf"
            }
            // PDFs always end in .pdf so listVaultMedia() types them as `pdf`
            // and the Room index never disagrees (e.g. a misnamed photo.jpg
            // picked via the PDF picker must not stay .jpg in documents/).
            val sanitized = if (type == "pdf") {
                sanitizePdfFileName(displayName) + "." + fallbackExt.lowercase(AppConstants.APP_LOCALE)
            } else {
                sanitizeImportedFileName(displayName, fallbackExt)
            }
            // Prefix keeps formatMediaDisplayName() pretty labels working for
            // imported files from any source (Gallery/Files/Drive/SD card).
            return "${prefix}_${timestamp}_${sanitized}"
        }

        internal fun extensionForMime(mimeType: String?): String? {
            if (mimeType.isNullOrBlank()) return null
            return try {
                MimeTypeMap.getSingleton().getExtensionFromMimeType(
                    mimeType.substringBefore(";").trim().lowercase(AppConstants.APP_LOCALE)
                )
            } catch (_: Exception) {
                null
            }
        }

        internal fun inferNameFromMime(displayName: String?, mimeType: String?): String? {
            if (displayName != null && "." in displayName) return displayName
            val ext = extensionForMime(mimeType) ?: return displayName
            return if (displayName.isNullOrBlank()) "file.$ext" else "$displayName.$ext"
        }

        /** English-locale lowercase helper for filenames from any device locale. */
        internal fun String.lowerEnglish(): String = lowercase(AppConstants.APP_LOCALE)

        /**
         * Extracts the vault filename from a stored path: absolute local path
         * or `content://` SAF uri. SAF document URIs percent-encode the
         * subfolder separator (`...%2Fvideos%2FVID_....mp4`), in either upper-
         * or lower-case hex, so normalize both before taking the last segment.
         */
        fun storedFileNameFromPath(path: String): String {
            val lastSegment = path.substringAfterLast("/")
            val withSeparators = lastSegment
                .replace("%2F", "/")
                .replace("%2f", "/")
            val name = withSeparators.substringAfterLast("/")
            return try {
                java.net.URLDecoder.decode(name, "UTF-8")
            } catch (_: Exception) {
                name
            }
        }

        @Suppress("unused")
        internal fun localeForTests(): Locale = AppConstants.APP_LOCALE
    }
}
