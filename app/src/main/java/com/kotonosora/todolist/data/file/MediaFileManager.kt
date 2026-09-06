package com.kotonosora.todolist.data.file

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

sealed class MediaOutputLocation {
    data class LocalFile(val file: File) : MediaOutputLocation()
    data class DocumentFileUri(
        val documentFile: DocumentFile,
        val uri: Uri,
        val pathString: String
    ) : MediaOutputLocation()
}

@Singleton
class MediaFileManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    private fun generateTimestamp(): String {
        return SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
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
}
