package com.kotonosora.todolist.feature.media

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotonosora.todolist.data.database.MediaDao
import com.kotonosora.todolist.data.database.MediaEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class MediaViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val mediaDao: MediaDao
) : ViewModel() {

    private val _capturedPhotoPaths = MutableStateFlow<List<String>>(emptyList())
    val capturedPhotoPaths: StateFlow<List<String>> = _capturedPhotoPaths.asStateFlow()

    private val _recordedAudioPaths = MutableStateFlow<List<String>>(emptyList())
    val recordedAudioPaths: StateFlow<List<String>> = _recordedAudioPaths.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private var mediaRecorder: MediaRecorder? = null
    private var currentAudioFile: File? = null

    // In-memory map from filePath -> MediaEntity for deletion lookups
    private val mediaEntityCache = mutableMapOf<String, MediaEntity>()

    init {
        loadMediaFromDb()
    }

    private fun loadMediaFromDb() = viewModelScope.launch {
        mediaDao.getAllMedia().collect { entities ->
            mediaEntityCache.clear()
            entities.forEach { mediaEntityCache[it.filePath] = it }
            _capturedPhotoPaths.value = entities
                .filter { it.type == "photo" }
                .map { it.filePath }
            _recordedAudioPaths.value = entities
                .filter { it.type == "audio" }
                .map { it.filePath }
        }
    }

    fun onPhotoCaptured(path: String) = viewModelScope.launch {
        val entity = MediaEntity(todoId = null, type = "photo", filePath = path)
        mediaDao.insertMedia(entity)
        // StateFlow updated reactively via loadMediaFromDb collector
    }

    fun startRecording() {
        val audioDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC)
            ?: context.filesDir
        audioDir.mkdirs()
        val audioFile = File(audioDir, "REC_${System.currentTimeMillis()}.m4a")
        currentAudioFile = audioFile

        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        mediaRecorder?.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(audioFile.absolutePath)
            prepare()
            start()
        }
        _isRecording.value = true
    }

    fun stopRecording() {
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaRecorder = null
        }
        currentAudioFile?.absolutePath?.let { path ->
            viewModelScope.launch {
                val entity = MediaEntity(todoId = null, type = "audio", filePath = path)
                mediaDao.insertMedia(entity)
            }
        }
        currentAudioFile = null
        _isRecording.value = false
    }

    fun deletePhoto(path: String) = viewModelScope.launch {
        File(path).delete()
        mediaEntityCache[path]?.let { mediaDao.deleteMedia(it) }
    }

    fun deleteAudio(path: String) = viewModelScope.launch {
        File(path).delete()
        mediaEntityCache[path]?.let { mediaDao.deleteMedia(it) }
    }

    override fun onCleared() {
        super.onCleared()
        if (_isRecording.value) stopRecording()
    }
}



