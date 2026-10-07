package com.kotonosora.todolist.feature.media

import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotonosora.todolist.data.database.MediaDao
import com.kotonosora.todolist.data.database.MediaEntity
import com.kotonosora.todolist.data.factory.MediaRecorderFactory
import com.kotonosora.todolist.data.file.MediaFileManager
import com.kotonosora.todolist.data.file.MediaOutputLocation
import com.kotonosora.todolist.data.repository.UserPreferencesRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class MediaViewModel(
    private val context: Context,
    private val mediaDao: MediaDao,
    private val userPreferencesRepository: UserPreferencesRepository? = null,
    private val mediaFileManager: MediaFileManager? = null,
    private val mediaRecorderFactory: MediaRecorderFactory? = null,
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

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _recordingDurationSeconds = MutableStateFlow(0)
    val recordingDurationSeconds: StateFlow<Int> = _recordingDurationSeconds.asStateFlow()

    private val _importError = MutableStateFlow<String?>(null)
    val importError: StateFlow<String?> = _importError.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    private val _lastImportSummary = MutableStateFlow<String?>(null)
    val lastImportSummary: StateFlow<String?> = _lastImportSummary.asStateFlow()

    private val _recordingError = MutableStateFlow<String?>(null)
    val recordingError: StateFlow<String?> = _recordingError.asStateFlow()

    private val _isVaultSyncing = MutableStateFlow(false)
    val isVaultSyncing: StateFlow<Boolean> = _isVaultSyncing.asStateFlow()

    private val _currentAmplitude = MutableStateFlow(0)
    val currentAmplitude: StateFlow<Int> = _currentAmplitude.asStateFlow()

    val customFolderUri: StateFlow<String?> = userPreferencesRepository?.customStorageFolderUri
        ?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
        ?: MutableStateFlow(null)

    private var mediaRecorder: MediaRecorder? = null
    private var currentPfd: ParcelFileDescriptor? = null
    private var currentAudioPath: String? = null
    private var recordingJob: Job? = null
    private var amplitudeJob: Job? = null

    // In-memory map from filePath -> MediaEntity for deletion lookups
    private val mediaEntityCache = mutableMapOf<String, MediaEntity>()
    // Serializes vault reconciliations so concurrent refresh/import calls can't
    // both pass the guard and insert the same file twice.
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
        mediaDao.getAllMedia().collect { entities ->
            mediaEntityCache.clear()
            entities.forEach { mediaEntityCache[it.filePath] = it }
            _capturedPhotoPaths.value = entities
                .filter { it.type == "photo" }
                .map { it.filePath }
            _recordedVideoPaths.value = entities
                .filter { it.type == "video" }
                .map { it.filePath }
            _recordedAudioPaths.value = entities
                .filter { it.type == "audio" }
                .map { it.filePath }
            _pdfDocumentPaths.value = entities
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
        // not both enter and insert duplicate rows for the same filePath
        // (MediaEntity.id is a random UUID, so filePath duplicates aren't
        // replaced by Room's REPLACE strategy).
        if (!acquireVaultSync()) return@launch
        try {
            val customFolderUriStr =
                userPreferencesRepository?.customStorageFolderUri?.firstOrNull()
            if (!customFolderUriStr.isNullOrBlank() && !isSafTreeReadable(customFolderUriStr)) {
                // Vault permission lost/revoked: keep the DB index as-is instead
                // of pruning every SAF row whose file we can no longer see.
                return@launch
            }
            val manager = mediaFileManager ?: MediaFileManager(context)
            val vaultFiles = withContext(ioDispatcher) {
                try {
                    manager.listVaultMedia(customFolderUriStr)
                } catch (e: Exception) {
                    e.printStackTrace()
                    emptyList()
                }
            }
            // Don't rely only on the in-memory cache: on cold start
            // loadMediaFromDb() may not have emitted yet, so merge the cache
            // with a direct DAO snapshot to avoid inserting duplicates.
            val dbPaths = try {
                mediaDao.getAllMedia().firstOrNull()?.map { it.filePath }?.toSet()
                    ?: emptySet()
            } catch (e: Exception) {
                e.printStackTrace()
                emptySet()
            }
            val knownPaths = mediaEntityCache.keys + dbPaths
            val vaultPaths = vaultFiles.map { it.pathString }.toSet()
            // Index sideloaded files (e.g. copied via file manager).
            for (found in vaultFiles) {
                if (!knownPaths.contains(found.pathString) &&
                    !mediaEntityCache.containsKey(found.pathString)
                ) {
                    val entity =
                        MediaEntity(todoId = null, type = found.type, filePath = found.pathString)
                    try {
                        mediaDao.insertMedia(entity)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    mediaEntityCache[found.pathString] = entity
                }
            }
            // Prune rows whose file vanished from the vault. Rows outside the
            // current vault scan are kept while the file still exists on disk —
            // they stay playable/deletable via the gallery until removed.
            val stale = mediaEntityCache.keys.filter { cached ->
                !vaultPaths.contains(cached) && !mediaFileExists(cached)
            }
            for (path in stale) {
                mediaEntityCache.remove(path)
                try {
                    mediaDao.deleteByPath(path)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } finally {
            releaseVaultSync()
        }
    }

    private fun isSafTreeReadable(customFolderUriStr: String): Boolean {
        return try {
            val tree = DocumentFile.fromTreeUri(context, Uri.parse(customFolderUriStr))
            tree != null && tree.canRead()
        } catch (e: Exception) {
            false
        }
    }

    private fun mediaFileExists(path: String): Boolean {
        return try {
            if (path.startsWith("content://")) {
                DocumentFile.fromSingleUri(context, Uri.parse(path))?.exists() == true
            } else {
                File(path).exists()
            }
        } catch (e: Exception) {
            // Conservatively keep the row when existence can't be determined
            // (e.g. revoked SAF permission) instead of wiping the index.
            true
        }
    }

    fun onPhotoCaptured(path: String) = onMediaCaptured("photo", path)

    fun onVideoCaptured(path: String) = onMediaCaptured("video", path)

    private fun onMediaCaptured(type: String, path: String) = viewModelScope.launch {
        val entity = MediaEntity(todoId = null, type = type, filePath = path)
        try {
            mediaDao.insertMedia(entity)
        } catch (e: Exception) {
            android.util.Log.w("MediaViewModel", "insert $type failed", e)
        }
        mediaEntityCache[path] = entity
    }

    fun startRecording() = viewModelScope.launch {
        if (_isRecording.value) return@launch
        _recordingError.value = null
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
            _recordingDurationSeconds.value = 0

            startRecordingTimer()
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
            withContext(ioDispatcher) { deleteLocation(location) }
            _isRecording.value = false
            _recordingError.value = "Couldn't start recording (${e.message})."
        }
    }

    private fun startRecordingTimer() {
        recordingJob?.cancel()
        amplitudeJob?.cancel()
        recordingJob = viewModelScope.launch {
            while (_isRecording.value) {
                delay(1000)
                if (!_isPaused.value) {
                    _recordingDurationSeconds.value += 1
                }
            }
        }
        // Mic level polling stays responsive without inflating the counter.
        amplitudeJob = viewModelScope.launch {
            while (_isRecording.value) {
                delay(200)
                if (!_isPaused.value) {
                    try {
                        _currentAmplitude.value = mediaRecorder?.maxAmplitude ?: 0
                    } catch (_: Exception) {
                        // ignore
                    }
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
        if (path != null) {
            if (stopOk) {
                viewModelScope.launch {
                    val entity = MediaEntity(todoId = null, type = "audio", filePath = path)
                    mediaDao.insertMedia(entity)
                    mediaEntityCache[path] = entity
                }
            } else {
                // Short/corrupt clip: discard the file instead of indexing it.
                viewModelScope.launch(ioDispatcher) { deleteMediaFile(path) }
                _recordingError.value = "Couldn't save this recording — file was discarded."
            }
        }
        _isRecording.value = false
        _isPaused.value = false
        _recordingDurationSeconds.value = 0
        _currentAmplitude.value = 0
    }

    fun deletePhoto(path: String) = deleteMedia(path)

    fun deleteVideo(path: String) = deleteMedia(path)

    fun deleteAudio(path: String) = deleteMedia(path)

    fun deletePdf(path: String) = deleteMedia(path)

    fun deleteMedia(path: String) = viewModelScope.launch {
        withContext(ioDispatcher) { deleteMediaFile(path) }
        val removed = mediaEntityCache.remove(path)
        try {
            mediaDao.deleteByPath(path)
        } catch (e: Exception) {
            android.util.Log.w("MediaViewModel", "deleteByPath failed, fallback", e)
            removed?.let {
                try {
                    mediaDao.deleteMedia(it)
                } catch (e2: Exception) {
                    android.util.Log.w("MediaViewModel", "deleteMedia fallback failed", e2)
                }
            }
        }
    }

    fun importPdf(sourceUri: Uri, onImported: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            _importError.value = null
            val location = try {
                withContext(ioDispatcher) {
                    val customFolderUriStr =
                        userPreferencesRepository?.customStorageFolderUri?.firstOrNull()
                    val manager = mediaFileManager ?: MediaFileManager(context)
                    // Display-name lookup is a ContentResolver query (IPC) — keep it
                    // off Main along with the file copy.
                    val displayName = queryDisplayName(sourceUri)
                    manager.importPdfDocument(sourceUri, displayName, customFolderUriStr)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
            val storedPath = when (location) {
                is MediaOutputLocation.DocumentFileUri -> location.pathString
                is MediaOutputLocation.LocalFile -> location.file.absolutePath
                null -> null
            }
            if (storedPath != null) {
                val entity = MediaEntity(todoId = null, type = "pdf", filePath = storedPath)
                mediaDao.insertMedia(entity)
                mediaEntityCache[storedPath] = entity
                onImported()
            } else {
                val message = "Couldn't import this PDF — copy failed."
                _importError.value = message
                onError(message)
            }
        }
    }

    /**
     * Generic vault import for user-picked media from any source (Gallery,
     * Files, Downloads, Drive, SD card): images, videos, any audio format and
     * PDFs. Copies off Main with a streaming buffer, indexes each success into
     * Room, and reports a summary. `onResult(success, failed)` runs on Main.
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
                val customFolderUriStr =
                    userPreferencesRepository?.customStorageFolderUri?.firstOrNull()
                val manager = mediaFileManager ?: MediaFileManager(context)
                for (uri in uris) {
                    val stored: Pair<String, String>? = try {
                        withContext(ioDispatcher) {
                            val location = manager.importMediaDocument(uri, customFolderUriStr)
                                ?: return@withContext null
                            val path = when (location) {
                                is MediaOutputLocation.DocumentFileUri -> location.pathString
                                is MediaOutputLocation.LocalFile -> location.file.absolutePath
                            }
                            // The stored filename is authoritative (timestamped + sanitized
                            // with the original extension preserved, except PDFs forced
                            // to .pdf). Deriving the index type from it guarantees the
                            // Room row matches what listVaultMedia() will scan, without
                            // extra ContentResolver queries per file.
                            val storedName = storedFileNameFromPath(path)
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
                            val entity = MediaEntity(todoId = null, type = type, filePath = path)
                            mediaDao.insertMedia(entity)
                            mediaEntityCache[path] = entity
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
                _importError.value = "Couldn't import ${uris.size} file(s) — unsupported type or copy failed."
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

    fun clearRecordingError() {
        _recordingError.value = null
    }

    private fun deleteMediaFile(path: String) {
        try {
            if (path.startsWith("content://")) {
                // SAF tree documents must go through DocumentFile; resolver
                // delete alone returns 0 and leaves the file behind, which
                // refreshVaultMedia() would then re-index as a ghost.
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

    private fun deleteLocation(location: MediaOutputLocation) {
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

    private fun queryDisplayName(uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex =
                    cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (_isRecording.value) stopRecording()
    }

    companion object {
        /**
         * Extracts the vault filename from a stored path: absolute local path
         * or `content://` SAF uri. SAF document URIs percent-encode the
         * subfolder separator (`...%2Fvideos%2FVID_....mp4`), in either upper-
         * or lower-case hex, so normalize both before taking the last segment.
         */
        internal fun storedFileNameFromPath(path: String): String {
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
    }
}
