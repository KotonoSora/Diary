package com.kotonosora.todolist.feature.media

import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotonosora.todolist.data.database.MediaDao
import com.kotonosora.todolist.data.database.MediaEntity
import com.kotonosora.todolist.data.factory.MediaRecorderFactory
import com.kotonosora.todolist.data.file.MediaFileManager
import com.kotonosora.todolist.data.file.MediaOutputLocation
import com.kotonosora.todolist.data.repository.UserPreferencesRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class MediaViewModel(
    private val context: Context,
    private val mediaDao: MediaDao,
    private val userPreferencesRepository: UserPreferencesRepository? = null,
    private val mediaFileManager: MediaFileManager? = null,
    private val mediaRecorderFactory: MediaRecorderFactory? = null
) : ViewModel() {

    private val _capturedPhotoPaths = MutableStateFlow<List<String>>(emptyList())
    val capturedPhotoPaths: StateFlow<List<String>> = _capturedPhotoPaths.asStateFlow()

    private val _recordedAudioPaths = MutableStateFlow<List<String>>(emptyList())
    val recordedAudioPaths: StateFlow<List<String>> = _recordedAudioPaths.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _recordingDurationSeconds = MutableStateFlow(0)
    val recordingDurationSeconds: StateFlow<Int> = _recordingDurationSeconds.asStateFlow()

    private val _currentAmplitude = MutableStateFlow(0)
    val currentAmplitude: StateFlow<Int> = _currentAmplitude.asStateFlow()

    val customFolderUri: StateFlow<String?> = userPreferencesRepository?.customStorageFolderUri
        ?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
        ?: MutableStateFlow(null)

    private var mediaRecorder: MediaRecorder? = null
    private var currentPfd: ParcelFileDescriptor? = null
    private var currentAudioPath: String? = null
    private var recordingJob: Job? = null

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
    }

    fun startRecording() = viewModelScope.launch {
        val customFolderUriStr = userPreferencesRepository?.customStorageFolderUri?.firstOrNull()
        val manager = mediaFileManager ?: MediaFileManager(context)
        val location = manager.createAudioOutputLocation(customFolderUriStr)

        val recorder = mediaRecorderFactory?.createMediaRecorder()
            ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
        mediaRecorder = recorder

        try {
            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)

                when (location) {
                    is MediaOutputLocation.DocumentFileUri -> {
                        val pfd = context.contentResolver.openFileDescriptor(location.uri, "w")
                        if (pfd != null) {
                            currentPfd = pfd
                            setOutputFile(pfd.fileDescriptor)
                            currentAudioPath = location.pathString
                        }
                    }

                    is MediaOutputLocation.LocalFile -> {
                        setOutputFile(location.file.absolutePath)
                        currentAudioPath = location.file.absolutePath
                    }
                }

                prepare()
                start()
            }
            _isRecording.value = true
            _isPaused.value = false
            _recordingDurationSeconds.value = 0

            startRecordingTimer()
        } catch (e: Exception) {
            e.printStackTrace()
            _isRecording.value = false
        }
    }

    private fun startRecordingTimer() {
        recordingJob?.cancel()
        recordingJob = viewModelScope.launch {
            while (_isRecording.value) {
                delay(200)
                if (!_isPaused.value) {
                    try {
                        val amp = mediaRecorder?.maxAmplitude ?: 0
                        _currentAmplitude.value = amp
                    } catch (_: Exception) {
                        // ignore
                    }
                    _recordingDurationSeconds.value += 1
                }
            }
        }
    }

    fun pauseRecording() {
        if (_isRecording.value && !_isPaused.value) {
            try {
                mediaRecorder?.pause()
                _isPaused.value = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun resumeRecording() {
        if (_isRecording.value && _isPaused.value) {
            try {
                mediaRecorder?.resume()
                _isPaused.value = false
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun stopRecording() {
        recordingJob?.cancel()
        recordingJob = null

        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
            currentPfd?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaRecorder = null
            currentPfd = null
        }
        currentAudioPath?.let { path ->
            viewModelScope.launch {
                val entity = MediaEntity(todoId = null, type = "audio", filePath = path)
                mediaDao.insertMedia(entity)
            }
        }
        currentAudioPath = null
        _isRecording.value = false
        _isPaused.value = false
        _recordingDurationSeconds.value = 0
        _currentAmplitude.value = 0
    }

    fun deletePhoto(path: String) = viewModelScope.launch {
        try {
            if (path.startsWith("content://")) {
                context.contentResolver.delete(Uri.parse(path), null, null)
            } else {
                File(path).delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaEntityCache[path]?.let { mediaDao.deleteMedia(it) }
    }

    fun deleteAudio(path: String) = viewModelScope.launch {
        try {
            if (path.startsWith("content://")) {
                context.contentResolver.delete(Uri.parse(path), null, null)
            } else {
                File(path).delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaEntityCache[path]?.let { mediaDao.deleteMedia(it) }
    }

    override fun onCleared() {
        super.onCleared()
        if (_isRecording.value) stopRecording()
    }
}
