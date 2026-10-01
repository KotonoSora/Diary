package com.kotonosora.todolist.data.file

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.documentfile.provider.DocumentFile
import com.kotonosora.todolist.common.AppConstants
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

sealed class MediaOutputLocation {
    data class LocalFile(val file: File) : MediaOutputLocation()
    data class DocumentFileUri(
        val documentFile: DocumentFile,
        val uri: Uri,
        val pathString: String
    ) : MediaOutputLocation()
}

class MediaFileManager(
    private val context: Context
) {

    private fun generateTimestamp(): String {
        return SimpleDateFormat("yyyyMMdd_HHmmss_SSS", AppConstants.APP_LOCALE).format(Date())
    }

    fun createPhotoOutputLocation(customFolderUriStr: String?): MediaOutputLocation {
        val fileName = "IMG_${generateTimestamp()}.jpg"
        if (!customFolderUriStr.isNullOrBlank()) {
            try {
                val treeUri = Uri.parse(customFolderUriStr)
                val treeFile = DocumentFile.fromTreeUri(context, treeUri)
                if (treeFile != null && treeFile.canWrite()) {
                    val docFile = treeFile.createFile("image/jpeg", fileName)
                    if (docFile != null) {
                        return MediaOutputLocation.DocumentFileUri(
                            documentFile = docFile,
                            uri = docFile.uri,
                            pathString = docFile.uri.toString()
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Fallback to local pictures directory
        val picturesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            ?: context.filesDir
        picturesDir.mkdirs()
        val file = File(picturesDir, fileName)
        return MediaOutputLocation.LocalFile(file)
    }

    fun createAudioOutputLocation(customFolderUriStr: String?): MediaOutputLocation {
        val fileName = "AUD_${generateTimestamp()}.m4a"
        if (!customFolderUriStr.isNullOrBlank()) {
            try {
                val treeUri = Uri.parse(customFolderUriStr)
                val treeFile = DocumentFile.fromTreeUri(context, treeUri)
                if (treeFile != null && treeFile.canWrite()) {
                    val docFile = treeFile.createFile("audio/mp4", fileName)
                    if (docFile != null) {
                        return MediaOutputLocation.DocumentFileUri(
                            documentFile = docFile,
                            uri = docFile.uri,
                            pathString = docFile.uri.toString()
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Fallback to local music directory
        val musicDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
            ?: context.filesDir
        musicDir.mkdirs()
        val file = File(musicDir, fileName)
        return MediaOutputLocation.LocalFile(file)
    }

    /**
     * Copies a user-picked PDF (`content://` from the system file picker) into the
     * vault: the SAF tree when a custom folder is set, else the local Documents dir.
     * Returns the output location, or null when the copy fails.
     */
    fun importPdfDocument(
        sourceUri: Uri,
        displayName: String?,
        customFolderUriStr: String?
    ): MediaOutputLocation? {
        val fileName = "DOC_${generateTimestamp()}_${sanitizePdfFileName(displayName)}.pdf"
        if (!customFolderUriStr.isNullOrBlank()) {
            try {
                val treeUri = Uri.parse(customFolderUriStr)
                val treeFile = DocumentFile.fromTreeUri(context, treeUri)
                if (treeFile != null && treeFile.canWrite()) {
                    val docFile = treeFile.createFile("application/pdf", fileName)
                    if (docFile != null) {
                        val copied = try {
                            val input = context.contentResolver.openInputStream(sourceUri)
                                ?: return null
                            val output = context.contentResolver.openOutputStream(docFile.uri)
                                ?: run {
                                    input.close()
                                    docFile.delete()
                                    return null
                                }
                            input.use { inputStream ->
                                output.use { outputStream ->
                                    inputStream.copyTo(outputStream)
                                }
                            }
                            true
                        } catch (e: Exception) {
                            e.printStackTrace()
                            try {
                                docFile.delete()
                            } catch (_: Exception) {
                            }
                            return null
                        }
                        if (!copied) return null
                        return MediaOutputLocation.DocumentFileUri(
                            documentFile = docFile,
                            uri = docFile.uri,
                            pathString = docFile.uri.toString()
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Fallback to local documents directory
        return try {
            val documentsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                ?: context.filesDir
            documentsDir.mkdirs()
            val file = File(documentsDir, fileName)
            val input = context.contentResolver.openInputStream(sourceUri) ?: return null
            try {
                input.use { inputStream ->
                    file.outputStream().use { output -> inputStream.copyTo(output) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                try {
                    if (file.exists()) file.delete()
                } catch (_: Exception) {
                }
                return null
            }
            if (!file.exists()) return null
            MediaOutputLocation.LocalFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    companion object {
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
    }
}
