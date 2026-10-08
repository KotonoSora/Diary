package com.kotonosora.todolist.feature.media

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotonosora.todolist.data.file.MediaFileManager
import com.kotonosora.todolist.data.file.MediaOutputLocation
import com.kotonosora.todolist.domain.service.AudioCaptureService
import com.kotonosora.todolist.domain.usecase.MediaUseCases
import com.kotonosora.todolist.domain.usecase.PdfUseCases
import com.kotonosora.todolist.domain.usecase.PreferencesUseCases
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

/**
 * DDD: media index via [MediaUseCases], PDF reader state via [PdfUseCases],
 * settings via [PreferencesUseCases], recording via [AudioCaptureService],
 * file I/O via [MediaFileManager]. Never repositories, WorkManager or
 * `MediaRecorder` directly.
 */
class MediaViewModel(
    private val media: MediaUseCases,
    private val prefs: PreferencesUseCases,
    private val pdf: PdfUseCases,
    private val mediaFileManager: MediaFileManager,
    private val audioCapture: AudioCaptureService,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _capturedPhotoPaths = MutableStateFlow<List<String>>(emptyList())
    val capturedPhotoPaths: StateFlow<List<String>> = _capturedPhotoPaths.asStateFlow()

    private val _recordedVideoPaths = MutableStateFlow<List<String>>(emptyList())
    val recordedVideoPaths: StateFlow<List<String>> = _recordedVideoPaths.asStateFlow()

    private val _recordedAudioPaths = MutableStateFlow<List<String>>(emptyList())
    val recordedAudioPaths: StateFlow<List<String>> = _recordedAudioPaths.asStateFlow()

    private val _pdfDocumentPaths = MutableStateFlow<List<String>>(emptyList())
    val pdfDocumentPaths: StateFlow<List<String>> = _pdfDocumentPaths.asStateFlow()

    val isRecording: StateFlow<Boolean> = audioCapture.isRecording
    val isPaused: StateFlow<Boolean> = audioCapture.isPaused
    val recordingDurationSeconds: StateFlow<Int> = audioCapture.durationSeconds
    val currentAmplitude: StateFlow<Int> = audioCapture.amplitude
    val recordingError: StateFlow<String?> = audioCapture.error

    private val _importError = MutableStateFlow<String?>(null)
    val importError: StateFlow<String?> = _importError.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    private val _lastImportSummary = MutableStateFlow<String?>(null)
    val lastImportSummary: StateFlow<String?> = _lastImportSummary.asStateFlow()

    private val _isVaultSyncing = MutableStateFlow(false)
    val isVaultSyncing: StateFlow<Boolean> = _isVaultSyncing.asStateFlow()

    val customFolderUri: StateFlow<String?> = prefs.observeCustomStorageFolder()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // Serializes vault reconciliations so concurrent refresh/import calls can't
    // both pass the guard and insert the same file twice (the index holds one
    // row per filePath — see MediaRepository).
    // The `is*` flags always change together with the mutex — only via the
    // acquire/release helpers below, never directly at callsites.
    private val vaultSyncMutex = Mutex()
    private val importMutex = Mutex()

    private fun acquireVaultSync(): Boolean {
        if (!vaultSyncMutex.tryLock()) return false
        _isVaultSyncing.value = true
        return true
    }

    private fun releaseVaultSync() {
        _isVaultSyncing.value = false
        vaultSyncMutex.unlock()
    }

    private fun acquireImport(): Boolean {
        if (!importMutex.tryLock()) return false
        _isImporting.value = true
        return true
    }

    private fun releaseImport() {
        _isImporting.value = false
        importMutex.unlock()
    }

    init {
        loadMediaFromDb()
        refreshVaultMedia()
    }

    private fun loadMediaFromDb() = viewModelScope.launch {
        media.observeAll().collect { items ->
            _capturedPhotoPaths.value = items
                .filter { it.type == "photo" }
                .map { it.filePath }
            _recordedVideoPaths.value = items
                .filter { it.type == "video" }
                .map { it.filePath }
            _recordedAudioPaths.value = items
                .filter { it.type == "audio" }
                .map { it.filePath }
            _pdfDocumentPaths.value = items
                .filter { it.type == "pdf" }
                .map { it.filePath }
        }
    }

    /**
     * Reconciles the Room index with the files physically present in the
     * chosen vault folder: inserts rows for sideloaded captures, drops rows
     * whose file is gone. Filesystem is the source of truth.
     */
    fun refreshVaultMedia() = viewModelScope.launch {
        // Atomic guard: concurrent refresh calls (init + pull-to-refresh) must
        // not both enter and insert duplicate rows for the same filePath (the
        // index holds one row per filePath — see MediaRepository).
        if (!acquireVaultSync()) return@launch
        try {
            val customFolderUriStr = prefs.observeCustomStorageFolder().firstOrNull()
            if (!customFolderUriStr.isNullOrBlank() &&
                !mediaFileManager.isTreeReadable(customFolderUriStr)
            ) {
                // Vault permission lost/revoked: keep the DB index as-is instead
                // of pruning every SAF row whose file we can no longer see.
                return@launch
            }
            val vaultFiles = withContext(ioDispatcher) {
                try {
                    mediaFileManager.listVaultMedia(customFolderUriStr)
                } catch (e: Exception) {
                    e.printStackTrace()
                    emptyList()
                }
            }
            // Don't rely only on the in-memory UI state: on cold start the
            // index flow may not have emitted yet, so merge it with a direct
            // repository snapshot to avoid inserting duplicates.
            val knownPaths = try {
                media.snapshotPaths()
            } catch (e: Exception) {
                e.printStackTrace()
                emptySet()
            } + _capturedPhotoPaths.value + _recordedVideoPaths.value +
                    _recordedAudioPaths.value + _pdfDocumentPaths.value
            val vaultPaths = vaultFiles.map { it.pathString }.toSet()
            // Index sideloaded files (e.g. copied via file manager).
            for (found in vaultFiles) {
                if (!knownPaths.contains(found.pathString)) {
                    try {
                        media.addMedia(found.type, found.pathString)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            // Prune rows whose file vanished from the vault. Rows outside the
            // current vault scan are kept while the file still exists on disk —
            // they stay playable/deletable via the gallery until removed.
            val stale = knownPaths.filter { cached ->
                !vaultPaths.contains(cached) && !mediaFileManager.storedFileExists(cached)
            }
            for (path in stale) {
                try {
                    media.removeByPath(path)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                // PDFs deleted externally leave no other cleaner: drop their
                // reader state with the media row so the PDF tables can't
                // accumulate unreachable rows.
                val wasPdf = MediaFileManager.typeForFileName(
                    MediaFileManager.storedFileNameFromPath(path)
                ) == "pdf"
                if (wasPdf) {
                    try {
                        pdf.deleteForFile(path)
                    } catch (_: Exception) {
                    }
                }
            }
            // Catch-up for orphans that predate the stale-path cleanup above
            // (e.g. PDF removed while its media row was already gone): drop
            // reader state whose file no longer exists anywhere.
            try {
                pdf.cleanupOrphans(vaultPaths, mediaFileManager::storedFileExists)
            } catch (_: Exception) {
            }
        } finally {
            releaseVaultSync()
        }
    }

    fun onPhotoCaptured(path: String) = onMediaCaptured("photo", path)

    fun onVideoCaptured(path: String) = onMediaCaptured("video", path)

    private fun onMediaCaptured(type: String, path: String) = viewModelScope.launch {
        try {
            media.addMedia(type, path)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun startRecording() = viewModelScope.launch {
        val customFolderUriStr = prefs.observeCustomStorageFolder().firstOrNull()
        audioCapture.start(customFolderUriStr)
    }

    fun pauseRecording() = audioCapture.pause()

    fun resumeRecording() = audioCapture.resume()

    fun stopRecording() {
        val path = audioCapture.stop() ?: return
        viewModelScope.launch {
            try {
                media.addMedia("audio", path)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deletePhoto(path: String) = deleteMedia(path)

    fun deleteVideo(path: String) = deleteMedia(path)

    fun deleteAudio(path: String) = deleteMedia(path)

    fun deletePdf(path: String) = viewModelScope.launch {
        try {
            deleteMedia(path).join()
        } catch (_: Exception) {
        }
        // Reader state is keyed by path — drop it with the file so the
        // tables can't accumulate unreachable rows.
        withContext(ioDispatcher) {
            try {
                pdf.deleteForFile(path)
            } catch (_: Exception) {
            }
        }
    }

    fun deleteMedia(path: String) = viewModelScope.launch {
        withContext(ioDispatcher) { mediaFileManager.deleteStoredFile(path) }
        try {
            media.removeByPath(path)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Vault import for on-device media selected in the in-app device picker
     * (`content://media/...` from `MediaStore`): images, videos, any audio
     * format and PDFs. Copies off Main with a streaming buffer, indexes each
     * success into Room, and reports a summary.
     * `onResult(success, failed)` runs on Main.
     */
    fun importMediaUris(
        uris: List<Uri>,
        onResult: (success: Int, failed: Int) -> Unit = { _, _ -> }
    ) {
        if (uris.isEmpty()) {
            onResult(0, 0)
            return
        }
        viewModelScope.launch {
            if (!acquireImport()) {
                onResult(0, uris.size)
                return@launch
            }
            _importError.value = null
            _lastImportSummary.value = null
            var success = 0
            var failed = 0
            try {
                val customFolderUriStr = prefs.observeCustomStorageFolder().firstOrNull()
                for (uri in uris) {
                    val stored: Pair<String, String>? = try {
                        withContext(ioDispatcher) {
                            val location = mediaFileManager.importMediaDocument(
                                uri,
                                customFolderUriStr
                            ) ?: return@withContext null
                            val path = when (location) {
                                is MediaOutputLocation.DocumentFileUri -> location.pathString
                                is MediaOutputLocation.LocalFile -> location.file.absolutePath
                            }
                            // The stored filename is authoritative (timestamped + sanitized
                            // with the original extension preserved, except PDFs forced
                            // to .pdf). Deriving the index type from it guarantees the
                            // Room row matches what listVaultMedia() will scan, without
                            // extra ContentResolver queries per file.
                            val storedName = MediaFileManager.storedFileNameFromPath(path)
                            val storedType = MediaFileManager.typeForFileName(storedName)
                                ?: return@withContext null
                            path to storedType
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                    if (stored != null) {
                        val (path, type) = stored
                        try {
                            media.addMedia(type, path)
                            success++
                        } catch (e: Exception) {
                            e.printStackTrace()
                            failed++
                        }
                    } else {
                        failed++
                    }
                }
            } finally {
                releaseImport()
            }
            if (failed > 0 && success == 0) {
                _importError.value =
                    "Couldn't import ${uris.size} file(s) — unsupported type or copy failed."
            } else if (failed > 0) {
                _lastImportSummary.value = "Imported $success file(s), $failed failed."
            } else {
                _lastImportSummary.value = "Imported $success file(s) into the vault."
            }
            onResult(success, failed)
        }
    }

    fun clearImportMessages() {
        _importError.value = null
        _lastImportSummary.value = null
    }

    /** On-device `MediaStore` listing for the in-app picker. Call off Main. */
    suspend fun queryDeviceMedia() = media.queryDeviceMedia()

    fun clearRecordingError() = audioCapture.clearError()

    override fun onCleared() {
        super.onCleared()
        if (audioCapture.isRecording.value) {
            audioCapture.stop()?.let { path ->
                viewModelScope.launch {
                    try {
                        media.addMedia("audio", path)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
        audioCapture.close()
    }
}
