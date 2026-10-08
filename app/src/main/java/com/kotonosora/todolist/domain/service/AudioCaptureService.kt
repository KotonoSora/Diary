package com.kotonosora.todolist.domain.service

import kotlinx.coroutines.flow.StateFlow

/**
 * Audio capture bounded service — application boundary (DDD).
 *
 * Owns the `MediaRecorder` lifecycle (MIC → MPEG_4/AAC into the vault
 * `audios/` subfolder), the duration/amplitude polling loops and the
 * pre-created stub cleanup. Implemented in the data layer
 * (`AudioCaptureServiceImpl`); the ViewModel only drives UI state and
 * indexes the returned path via [com.kotonosora.todolist.domain.usecase.MediaUseCases].
 */
interface AudioCaptureService {
    val isRecording: StateFlow<Boolean>
    val isPaused: StateFlow<Boolean>
    val durationSeconds: StateFlow<Int>
    val amplitude: StateFlow<Int>
    val error: StateFlow<String?>

    fun start(customFolderUriStr: String?)
    fun pause()
    fun resume()

    /**
     * Stops the recorder synchronously. Returns the stored vault path on
     * success (caller indexes it), or null when nothing was kept
     * (failure discards the stub and sets [error]).
     */
    fun stop(): String?

    fun clearError()

    /** Releases timer jobs. Call from `ViewModel.onCleared`. */
    fun close()
}
