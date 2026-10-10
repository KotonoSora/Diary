package com.kotonosora.todolist.data.media

import android.content.Context
import android.media.MediaRecorder
import android.os.ParcelFileDescriptor
import com.kotonosora.todolist.data.factory.MediaRecorderFactory
import com.kotonosora.todolist.data.file.MediaFileManager
import com.kotonosora.todolist.data.file.MediaOutputLocation
import com.kotonosora.todolist.domain.service.AudioCaptureService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * Data-layer audio recorder: `MediaRecorder` lifecycle, SAF `content://`
 * outputs (via `ParcelFileDescriptor`) vs local paths, duration/amplitude
 * polling, and empty-stub cleanup. Indexing stays with the caller (it owns
 * the [com.kotonosora.todolist.domain.usecase.MediaUseCases] boundary).
 *
 * Owns an internal scope for the polling loops; [close] cancels it.
 * `pause()`/`resume()` need no API guard (`minSdk 24` = API level of
 * `MediaRecorder.pause()`).
 */
class AudioCaptureServiceImpl(
    private val context: Context,
    private val mediaFileManager: MediaFileManager,
    private val mediaRecorderFactory: MediaRecorderFactory,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AudioCaptureService {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _isRecording = MutableStateFlow(false)
    override val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    override val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _durationSeconds = MutableStateFlow(0)
    override val durationSeconds: StateFlow<Int> = _durationSeconds.asStateFlow()

    private val _amplitude = MutableStateFlow(0)
    override val amplitude: StateFlow<Int> = _amplitude.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    override val error: StateFlow<String?> = _error.asStateFlow()

    private var mediaRecorder: MediaRecorder? = null
    private var currentPfd: ParcelFileDescriptor? = null
    private var currentAudioPath: String? = null
    private var recordingJob: Job? = null
    private var amplitudeJob: Job? = null

    override fun start(customFolderUriStr: String?) {
        // Re-entry guard: a second tap must not leak a recorder + pfd.
        if (_isRecording.value) return
        _error.value = null
        val location = mediaFileManager.createAudioOutputLocation(customFolderUriStr)

        val recorder = mediaRecorderFactory.createMediaRecorder()
        mediaRecorder = recorder

        try {
            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)

                when (location) {
                    is MediaOutputLocation.DocumentFileUri -> {
                        val pfd = context.contentResolver.openFileDescriptor(location.uri, "w")
                            ?: throw IOException("Couldn't open vault audio file.")
                        currentPfd = pfd
                        setOutputFile(pfd.fileDescriptor)
                        currentAudioPath = location.pathString
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
            _durationSeconds.value = 0

            startPolling()
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                recorder.release()
            } catch (_: Exception) {
            }
            try {
                currentPfd?.close()
            } catch (_: Exception) {
            }
            mediaRecorder = null
            currentPfd = null
            currentAudioPath = null
            // The output file was pre-created before recording started —
            // remove the empty stub so the vault doesn't fill with dead files.
            scope.launch(ioDispatcher) { mediaFileManager.deleteLocation(location) }
            _isRecording.value = false
            _error.value = "Couldn't start recording (${e.message})."
        }
    }

    private fun startPolling() {
        recordingJob?.cancel()
        amplitudeJob?.cancel()
        recordingJob = scope.launch {
            while (_isRecording.value) {
                delay(1000)
                if (!_isPaused.value) {
                    _durationSeconds.value += 1
                }
            }
        }
        // Mic level polling stays responsive without inflating the counter.
        amplitudeJob = scope.launch {
            while (_isRecording.value) {
                delay(200)
                if (!_isPaused.value) {
                    try {
                        _amplitude.value = mediaRecorder?.maxAmplitude ?: 0
                    } catch (_: Exception) {
                        // ignore
                    }
                }
            }
        }
    }

    override fun pause() {
        if (_isRecording.value && !_isPaused.value) {
            try {
                mediaRecorder?.pause()
                _isPaused.value = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun resume() {
        if (_isRecording.value && _isPaused.value) {
            try {
                mediaRecorder?.resume()
                _isPaused.value = false
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun stop(): String? {
        recordingJob?.cancel()
        recordingJob = null
        amplitudeJob?.cancel()
        amplitudeJob = null

        var stopOk = true
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
            currentPfd?.close()
        } catch (e: Exception) {
            e.printStackTrace()
            stopOk = false
        } finally {
            mediaRecorder = null
            currentPfd = null
        }
        val path = currentAudioPath
        currentAudioPath = null
        if (path != null && !stopOk) {
            // Short/corrupt clip: discard the file instead of keeping it.
            scope.launch(ioDispatcher) { mediaFileManager.deleteStoredFile(path) }
            _error.value = "Couldn't save this recording — file was discarded."
            resetState()
            return null
        }
        resetState()
        return path
    }

    private fun resetState() {
        _isRecording.value = false
        _isPaused.value = false
        _durationSeconds.value = 0
        _amplitude.value = 0
    }

    override fun clearError() {
        _error.value = null
    }

    override fun close() {
        recordingJob?.cancel()
        amplitudeJob?.cancel()
        scope.coroutineContext[Job]?.cancel()
        try {
            mediaRecorder?.release()
        } catch (_: Exception) {
        }
        try {
            currentPfd?.close()
        } catch (_: Exception) {
        }
        mediaRecorder = null
        currentPfd = null
        currentAudioPath = null
    }
}
