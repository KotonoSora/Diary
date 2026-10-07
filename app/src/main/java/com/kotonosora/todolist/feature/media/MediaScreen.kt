package com.kotonosora.todolist.feature.media

import android.Manifest
import android.content.res.Configuration
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.kotonosora.todolist.common.AppConstants
import com.kotonosora.todolist.data.file.MediaFileManager
import com.kotonosora.todolist.ui.components.AudioPlayerView
import com.kotonosora.todolist.ui.components.AudioWaveformBars
import com.kotonosora.todolist.ui.components.CameraCaptureView
import com.kotonosora.todolist.ui.components.ImageLightboxDialog
import com.kotonosora.todolist.ui.components.PdfReaderDialog
import com.kotonosora.todolist.ui.components.VideoPlayerView
import com.kotonosora.todolist.ui.theme.AppTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaScreen(
    viewModel: MediaViewModel = viewModel(),
    onOpenDrawer: (() -> Unit)? = null
) {
    val capturedPhotos by viewModel.capturedPhotoPaths.collectAsState()
    val recordedVideos by viewModel.recordedVideoPaths.collectAsState()
    val recordedAudios by viewModel.recordedAudioPaths.collectAsState()
    val pdfDocuments by viewModel.pdfDocumentPaths.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val isPaused by viewModel.isPaused.collectAsState()
    val recordingDuration by viewModel.recordingDurationSeconds.collectAsState()
    val currentAmplitude by viewModel.currentAmplitude.collectAsState()
    val customFolderUri by viewModel.customFolderUri.collectAsState()
    val isVaultSyncing by viewModel.isVaultSyncing.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()
    val recordingError by viewModel.recordingError.collectAsState()
    val importError by viewModel.importError.collectAsState()
    val lastImportSummary by viewModel.lastImportSummary.collectAsState()

    var showCameraSheet by remember { mutableStateOf(false) }
    var selectedLightboxPhoto by remember { mutableStateOf<String?>(null) }
    var selectedPdfPath by remember { mutableStateOf<String?>(null) }
    var selectedVideoPath by remember { mutableStateOf<String?>(null) }
    var selectedAudioPath by remember { mutableStateOf<String?>(null) }
    var isGridView by rememberSaveable { mutableStateOf(true) }
    var selectedFilter by rememberSaveable { mutableStateOf(MediaFilter.All) }
    val context = LocalContext.current

    LaunchedEffect(recordingError) {
        recordingError?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearRecordingError()
        }
    }
    LaunchedEffect(importError) {
        importError?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearImportMessages()
        }
    }
    LaunchedEffect(lastImportSummary) {
        lastImportSummary?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearImportMessages()
        }
    }

    // Video needs CAMERA + mic audio; photo needs CAMERA only. Request both
    // upfront so switching to video mode inside the sheet never fails.
    val cameraPermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val cameraOk = grants[Manifest.permission.CAMERA] == true
        if (cameraOk) showCameraSheet = true
        else Toast.makeText(context, "Camera permission is needed.", Toast.LENGTH_SHORT).show()
    }

    // Dedicated mic grant for VIDEO recording inside the camera sheet.
    // (The audio recorder's launcher below auto-starts audio recording on
    // grant, so it must not be reused here.)
    val cameraMicLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            Toast.makeText(context, "Microphone granted — tap record to start.", Toast.LENGTH_SHORT)
                .show()
        } else {
            Toast.makeText(
                context,
                "Microphone denied — video will have no sound. Enable it in Settings to record with audio.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.startRecording()
        else Toast.makeText(context, "Microphone permission is needed.", Toast.LENGTH_SHORT).show()
    }

    // NOTE: photo/video/audio/pdf/file imports intentionally do NOT use
    // system pickers: ACTION_GET_CONTENT always offers Google Photos as a
    // source. Imports go through the in-app DeviceMediaPickerSheet (MediaStore
    // = on-device files only).

    // ── Device-only media picker (MediaStore, never Google Photos) ──
    var devicePickerTab by remember { mutableStateOf<DeviceMediaTab?>(null) }
    var pendingPickerTab by remember { mutableStateOf<DeviceMediaTab?>(null) }
    var pendingPerms by remember { mutableStateOf(emptyArray<String>()) }
    val deviceMediaPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (pendingPerms.isNotEmpty() && pendingPerms.all { grants[it] == true }) {
            devicePickerTab = pendingPickerTab ?: DeviceMediaTab.Photos
        } else {
            Toast.makeText(
                context,
                "Allow access to photos & videos to pick files from this device.",
                Toast.LENGTH_LONG
            ).show()
        }
        pendingPickerTab = null
        pendingPerms = emptyArray()
    }

    fun openDevicePicker(tab: DeviceMediaTab) {
        if (hasDeviceMediaAccess(context, tab)) {
            devicePickerTab = tab
        } else {
            pendingPickerTab = tab
            pendingPerms = permissionsForTab(tab)
            deviceMediaPermLauncher.launch(pendingPerms)
        }
    }

    val vaultLabel = if (!customFolderUri.isNullOrBlank()) {
        "Vault folder • photos/ videos/ audios/ documents/"
    } else {
        "App vault • photos/ videos/ audios/ documents/"
    }

    MediaScreenContent(
        capturedPhotos = capturedPhotos,
        recordedVideos = recordedVideos,
        recordedAudios = recordedAudios,
        pdfDocuments = pdfDocuments,
        isRecording = isRecording,
        isPaused = isPaused,
        recordingDurationSeconds = recordingDuration,
        currentAmplitude = currentAmplitude,
        vaultLabel = vaultLabel,
        isVaultSyncing = isVaultSyncing,
        isGridView = isGridView,
        selectedFilter = selectedFilter,
        isImporting = isImporting,
        onToggleView = { isGridView = !isGridView },
        onFilterSelect = { selectedFilter = it },
        onCapturePhotoClick = {
            cameraPermissionsLauncher.launch(
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            )
        },
        onStartRecordClick = { audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
        onPauseRecordClick = { viewModel.pauseRecording() },
        onResumeRecordClick = { viewModel.resumeRecording() },
        onStopRecordClick = { viewModel.stopRecording() },
        onDeletePhoto = { viewModel.deletePhoto(it) },
        onDeleteVideo = { viewModel.deleteVideo(it) },
        onDeleteAudio = { viewModel.deleteAudio(it) },
        onDeletePdf = { viewModel.deletePdf(it) },
        onImportPdfClick = { openDevicePicker(DeviceMediaTab.Pdfs) },
        onImportPhotosClick = { openDevicePicker(DeviceMediaTab.Photos) },
        onImportVideosClick = { openDevicePicker(DeviceMediaTab.Videos) },
        onImportAudioClick = { openDevicePicker(DeviceMediaTab.Audio) },
        onImportFilesClick = { openDevicePicker(DeviceMediaTab.All) },
        onRefreshVaultClick = { viewModel.refreshVaultMedia() },
        onPhotoClick = { selectedLightboxPhoto = it },
        onVideoClick = { selectedVideoPath = it },
        onAudioClick = { selectedAudioPath = it },
        onPdfClick = { selectedPdfPath = it },
        onOpenDrawer = onOpenDrawer
    )

    // Full-screen Image Lightbox Dialog
    selectedLightboxPhoto?.let { photoPath ->
        ImageLightboxDialog(
            filePath = photoPath,
            onDismiss = { selectedLightboxPhoto = null }
        )
    }

    // Full-screen PDF Reader Dialog
    selectedPdfPath?.let { pdfPath ->
        PdfReaderDialog(
            filePath = pdfPath,
            onDismiss = { selectedPdfPath = null }
        )
    }

    // Grid video/audio previews — player is created only for the open dialog.
    selectedVideoPath?.let { videoPath ->
        VideoPreviewDialog(
            filePath = videoPath,
            onDismiss = { selectedVideoPath = null },
            onDelete = { viewModel.deleteVideo(it) }
        )
    }
    selectedAudioPath?.let { audioPath ->
        AudioPreviewDialog(
            filePath = audioPath,
            onDismiss = { selectedAudioPath = null },
            onDelete = { viewModel.deleteAudio(it) }
        )
    }

    // In-app device media picker (MediaStore only — Google Photos can never appear).
    devicePickerTab?.let { tab ->
        DeviceMediaPickerSheet(
            initialTab = tab,
            onDismiss = { devicePickerTab = null },
            onImport = { uris ->
                devicePickerTab = null
                if (uris.isNotEmpty()) viewModel.importMediaUris(uris)
            }
        )
    }

    // Camera Capture Bottom Sheet (photo + video)
    if (showCameraSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showCameraSheet = false },
            sheetState = sheetState
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(560.dp)
            ) {
                CameraCaptureView(
                    onPhotoCaptured = { pathStr ->
                        viewModel.onPhotoCaptured(pathStr)
                        showCameraSheet = false
                    },
                    onDismiss = { showCameraSheet = false },
                    customFolderUriStr = customFolderUri,
                    onVideoCaptured = { pathStr ->
                        viewModel.onVideoCaptured(pathStr)
                        showCameraSheet = false
                    },
                    onCaptureError = { message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    },
                    onMicPermissionNeeded = {
                        cameraMicLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaScreenContent(
    capturedPhotos: List<String>,
    recordedAudios: List<String>,
    pdfDocuments: List<String> = emptyList(),
    recordedVideos: List<String> = emptyList(),
    isRecording: Boolean,
    isPaused: Boolean = false,
    recordingDurationSeconds: Int = 0,
    currentAmplitude: Int = 0,
    vaultLabel: String = "App vault • photos/ videos/ audios/ documents/",
    isVaultSyncing: Boolean = false,
    isGridView: Boolean = true,
    selectedFilter: MediaFilter = MediaFilter.All,
    isImporting: Boolean = false,
    onToggleView: () -> Unit = {},
    onFilterSelect: (MediaFilter) -> Unit = {},
    onCapturePhotoClick: () -> Unit = {},
    onStartRecordClick: () -> Unit = {},
    onPauseRecordClick: () -> Unit = {},
    onResumeRecordClick: () -> Unit = {},
    onStopRecordClick: () -> Unit = {},
    onImportPdfClick: () -> Unit = {},
    onImportPhotosClick: () -> Unit = {},
    onImportVideosClick: () -> Unit = {},
    onImportAudioClick: () -> Unit = {},
    onImportFilesClick: () -> Unit = {},
    onRefreshVaultClick: () -> Unit = {},
    onDeletePhoto: (String) -> Unit = {},
    onDeleteVideo: (String) -> Unit = {},
    onDeleteAudio: (String) -> Unit = {},
    onDeletePdf: (String) -> Unit = {},
    onPhotoClick: (String) -> Unit = {},
    onVideoClick: (String) -> Unit = {},
    onAudioClick: (String) -> Unit = {},
    onPdfClick: (String) -> Unit = {},
    onOpenDrawer: (() -> Unit)? = null
) {
    Scaffold { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onOpenDrawer != null) {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Default.Menu, contentDescription = "Open Sidebar")
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        text = "Media & Captures",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onToggleView,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (isGridView) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Icon(
                            if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                            contentDescription = if (isGridView) "Switch to list view" else "Switch to grid view"
                        )
                    }
                    IconButton(onClick = onImportFilesClick) {
                        Icon(Icons.Default.UploadFile, contentDescription = "Import files")
                    }
                    IconButton(onClick = onRefreshVaultClick) {
                        Icon(Icons.Default.Refresh, contentDescription = "Rescan vault media")
                    }
                }
                Text(
                    text = if (isVaultSyncing) "Scanning vault…" else vaultLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (isImporting || isVaultSyncing) {
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(
                        text = if (isImporting) "Importing files…" else "Scanning vault…",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (isGridView) {
                item {
                    MediaFilterRow(
                        selected = selectedFilter,
                        onSelect = onFilterSelect,
                        counts = mapOf(
                            MediaFilter.Photos to capturedPhotos.size,
                            MediaFilter.Videos to recordedVideos.size,
                            MediaFilter.Audio to recordedAudios.size,
                            MediaFilter.Pdfs to pdfDocuments.size
                        )
                    )
                }
                item {
                    AudioRecorderGridBar(
                        isRecording = isRecording,
                        isPaused = isPaused,
                        recordingDurationSeconds = recordingDurationSeconds,
                        currentAmplitude = currentAmplitude,
                        onStartRecordClick = onStartRecordClick,
                        onPauseRecordClick = onPauseRecordClick,
                        onResumeRecordClick = onResumeRecordClick,
                        onStopRecordClick = onStopRecordClick,
                        onImportAudioClick = onImportAudioClick
                    )
                }
                item {
                    val galleryItems = collectGalleryItems(
                        photos = capturedPhotos,
                        videos = recordedVideos,
                        audios = recordedAudios,
                        pdfs = pdfDocuments,
                        filter = selectedFilter
                    )
                    MediaGalleryGrid(
                        items = galleryItems,
                        onPhotoClick = onPhotoClick,
                        onVideoClick = onVideoClick,
                        onAudioClick = onAudioClick,
                        onPdfClick = onPdfClick,
                        onDelete = { item ->
                            when (item.type) {
                                "photo" -> onDeletePhoto(item.path)
                                "video" -> onDeleteVideo(item.path)
                                "audio" -> onDeleteAudio(item.path)
                                else -> onDeletePdf(item.path)
                            }
                        }
                    )
                }
                item {
                    Text(
                        "Tap a tile to preview. Long-press or tap × to delete. Use the list view for inline playback and recording controls.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!isGridView) {
                // ── Photos section ──
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Photos",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(
                                onClick = onImportPhotosClick,
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            ) {
                                Icon(
                                    Icons.Default.PhotoLibrary,
                                    contentDescription = "Import photos",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            IconButton(
                                onClick = onCapturePhotoClick,
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Icon(
                                    Icons.Default.PhotoCamera,
                                    contentDescription = "Capture Photo or Video",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
                items(capturedPhotos, key = { it }) { path ->
                    MediaFileItem(
                        name = formatMediaDisplayName(path, isAudio = false),
                        subtitle = path,
                        filePath = path,
                        onPhotoClick = { onPhotoClick(path) },
                        onDelete = { onDeletePhoto(path) }
                    )
                }
                if (capturedPhotos.isEmpty()) {
                    item {
                        Text(
                            "No photos captured yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                item { HorizontalDivider() }

                // ── Videos section ──
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Videos",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(
                                onClick = onImportVideosClick,
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            ) {
                                Icon(
                                    Icons.Default.VideoLibrary,
                                    contentDescription = "Import videos",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            IconButton(
                                onClick = onCapturePhotoClick,
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Icon(
                                    Icons.Default.Videocam,
                                    contentDescription = "Record Video",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
                items(recordedVideos, key = { it }) { path ->
                    VideoFileItem(
                        name = formatMediaDisplayName(path, isAudio = false),
                        filePath = path,
                        onDelete = { onDeleteVideo(path) }
                    )
                }
                if (recordedVideos.isEmpty()) {
                    item {
                        Text(
                            "No videos recorded yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                item { HorizontalDivider() }

                // ── Audio section ──
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Audio Recordings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(
                                onClick = onImportAudioClick,
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            ) {
                                Icon(
                                    Icons.Default.AudioFile,
                                    contentDescription = "Import audio (MP3, WAV, FLAC, OGG, OPUS, MIDI and more)",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            if (!isRecording) {
                                IconButton(
                                    onClick = onStartRecordClick,
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.Mic,
                                        contentDescription = "Start Recording",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            } else {
                                // Pause / Resume Button
                                IconButton(
                                    onClick = { if (isPaused) onResumeRecordClick() else onPauseRecordClick() },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                                    )
                                ) {
                                    Icon(
                                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                        contentDescription = if (isPaused) "Resume Recording" else "Pause Recording",
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                // Stop Button
                                IconButton(
                                    onClick = onStopRecordClick,
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.Stop,
                                        contentDescription = "Stop Recording",
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                }

                if (isRecording) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(
                                    alpha = 0.3f
                                )
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.error,
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.padding(end = 8.dp)
                                        ) {
                                            Text(
                                                text = if (isPaused) "PAUSED" else "REC",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onError,
                                                modifier = Modifier.padding(
                                                    horizontal = 6.dp,
                                                    vertical = 2.dp
                                                )
                                            )
                                        }
                                        Text(
                                            text = formatRecordingTimer(recordingDurationSeconds),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                AudioWaveformBars(
                                    isPlaying = !isPaused,
                                    amplitude = currentAmplitude,
                                    isLiveMeter = true
                                )
                            }
                        }
                    }
                }

                items(recordedAudios, key = { it }) { path ->
                    AudioFileItem(
                        name = formatMediaDisplayName(path, isAudio = true),
                        filePath = path,
                        onDelete = { onDeleteAudio(path) }
                    )
                }
                if (recordedAudios.isEmpty()) {
                    item {
                        Text(
                            "No recordings yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                item { HorizontalDivider() }

                // ── PDF documents section ──
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "PDF Documents",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(
                                onClick = onImportFilesClick,
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            ) {
                                Icon(
                                    Icons.Default.AddPhotoAlternate,
                                    contentDescription = "Import any media file",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            IconButton(
                                onClick = onImportPdfClick,
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Icon(
                                    Icons.Default.PictureAsPdf,
                                    contentDescription = "Import PDF",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
                items(pdfDocuments, key = { it }) { path ->
                    MediaFileItem(
                        name = formatMediaDisplayName(path, isAudio = false),
                        subtitle = path,
                        filePath = null,
                        onPhotoClick = { onPdfClick(path) },
                        onDelete = { onDeletePdf(path) }
                    )
                }
                if (pdfDocuments.isEmpty()) {
                    item {
                        Text(
                            "No PDF documents yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

private fun formatRecordingTimer(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format(AppConstants.APP_LOCALE, "%02d:%02d", mins, secs)
}

/**
 * Compact audio recorder shown in grid mode so recording stays reachable
 * without switching to list view (list mode keeps its own full controls).
 */
@Composable
private fun AudioRecorderGridBar(
    isRecording: Boolean,
    isPaused: Boolean,
    recordingDurationSeconds: Int,
    currentAmplitude: Int = 0,
    onStartRecordClick: () -> Unit,
    onPauseRecordClick: () -> Unit,
    onResumeRecordClick: () -> Unit,
    onStopRecordClick: () -> Unit,
    onImportAudioClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isRecording) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isRecording) {
                    Surface(
                        color = MaterialTheme.colorScheme.error,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = if (isPaused) "PAUSED" else "REC",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onError,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = formatRecordingTimer(recordingDurationSeconds),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(
                        text = "Audio",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(onClick = onImportAudioClick) {
                    Icon(
                        Icons.Default.AudioFile,
                        contentDescription = "Import audio",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!isRecording) {
                    IconButton(
                        onClick = onStartRecordClick,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = "Start Recording",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                } else {
                    IconButton(
                        onClick = { if (isPaused) onResumeRecordClick() else onPauseRecordClick() },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (isPaused) "Resume Recording" else "Pause Recording",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    IconButton(
                        onClick = onStopRecordClick,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Icon(
                            Icons.Default.Stop,
                            contentDescription = "Stop Recording",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
        if (isRecording) {
            AudioWaveformBars(
                isPlaying = !isPaused,
                amplitude = currentAmplitude,
                isLiveMeter = true,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }
}

fun formatMediaDisplayName(pathOrName: String, isAudio: Boolean): String {
    val fileName = try {
        if (pathOrName.startsWith("content://")) {
            val uri = Uri.parse(pathOrName)
            uri.lastPathSegment?.substringAfterLast("/") ?: pathOrName
        } else {
            File(pathOrName).name
        }
    } catch (e: Exception) {
        pathOrName
    }

    val regex = Regex("""(IMG|VID|AUD|REC|DOC)_(\d{8}_\d{6}|\d+)""")
    val match = regex.find(fileName)

    if (match != null) {
        val type = match.groupValues[1]
        val rawTime = match.groupValues[2]

        val typeLabel = when (type) {
            "IMG" -> "Photo"
            "VID" -> "Video"
            "AUD", "REC" -> "Audio"
            "DOC" -> "Document"
            else -> if (isAudio) "Audio" else "Photo"
        }

        if (rawTime.contains("_")) {
            try {
                val inputFormat = SimpleDateFormat("yyyyMMdd_HHmmss", AppConstants.APP_LOCALE)
                val outputFormat = SimpleDateFormat("MMM d, yyyy, h:mm a", AppConstants.APP_LOCALE)
                val date = inputFormat.parse(rawTime)
                if (date != null) {
                    return "$typeLabel - ${outputFormat.format(date)}"
                }
            } catch (e: Exception) {
                // ignore
            }
        } else {
            rawTime.toLongOrNull()?.let { millis ->
                try {
                    val outputFormat =
                        SimpleDateFormat("MMM d, yyyy, h:mm a", AppConstants.APP_LOCALE)
                    return "$typeLabel - ${outputFormat.format(Date(millis))}"
                } catch (e: Exception) {
                    // ignore
                }
            }
        }
    }

    // Fall back to extension-aware labels for vault files without a prefix.
    // English-locale lowercasing keeps this stable on Turkish/Azeri devices.
    val lower = fileName.lowercase(AppConstants.APP_LOCALE)
    val fallbackLabel = when {
        lower.endsWith(".mp4") || lower.endsWith(".m4v") || lower.endsWith(".mov") ||
                lower.endsWith(".mkv") || lower.endsWith(".webm") || lower.endsWith(".3gp") ||
                lower.endsWith(".3g2") || lower.endsWith(".ts") || lower.endsWith(".m2ts") ||
                lower.endsWith(".mts") || lower.endsWith(".mpg") || lower.endsWith(".mpeg") ||
                lower.endsWith(".ogv") || lower.endsWith(".flv") -> "Video - "

        lower.endsWith(".m4a") || lower.endsWith(".mp3") || lower.endsWith(".mp2") ||
                lower.endsWith(".aac") || lower.endsWith(".adts") || lower.endsWith(".ac3") ||
                lower.endsWith(".ogg") || lower.endsWith(".oga") || lower.endsWith(".opus") ||
                lower.endsWith(".weba") || lower.endsWith(".wav") || lower.endsWith(".wave") ||
                lower.endsWith(".flac") || lower.endsWith(".alac") || lower.endsWith(".amr") ||
                lower.endsWith(".awb") || lower.endsWith(".mid") || lower.endsWith(".midi") ||
                lower.endsWith(".xmf") || lower.endsWith(".mxmf") || lower.endsWith(".rtttl") ||
                lower.endsWith(".3ga") || lower.endsWith(".mka") || lower.endsWith(".aiff") ||
                lower.endsWith(".aif") || lower.endsWith(".wma") -> "Audio - "

        lower.endsWith(".pdf") -> "Document - "
        isAudio -> "Audio - "
        else -> "Photo - "
    }
    // Strip only the trailing extension (any supported type) for a clean label.
    val stem = fileName.substringBeforeLast(".", fileName)
    return fallbackLabel + stem.substringAfterLast("/").substringAfterLast("\\")
}

@Composable
private fun AudioFileItem(
    name: String,
    filePath: String,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            if (!LocalInspectionMode.current) {
                AudioPlayerView(filePath = filePath)
            }
        }
    }
}

@Composable
private fun VideoFileItem(
    name: String,
    filePath: String,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            if (!LocalInspectionMode.current) {
                VideoPlayerView(filePath = filePath)
            }
        }
    }
}

@Composable
private fun MediaFileItem(
    name: String,
    subtitle: String,
    filePath: String? = null,
    onPhotoClick: () -> Unit = {},
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPhotoClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (filePath != null) {
                val imageModel = remember(filePath) {
                    if (filePath.startsWith("content://")) Uri.parse(filePath)
                    else File(filePath)
                }
                Card(
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .size(60.dp)
                        .clickable { onPhotoClick() }
                ) {
                    AsyncImage(
                        model = imageModel,
                        contentDescription = name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// ── FULL CASE-BY-CASE PREVIEWS ──

@Preview(
    showBackground = true,
    name = "1. Media Screen - Grid Gallery (Dark)",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun MediaScreenPreview_GridGallery_Dark() {
    val samplePhotos = listOf(
        "${MediaFileManager.PHOTOS_DIR}/IMG_20260301_120000.jpg",
        "${MediaFileManager.PHOTOS_DIR}/IMG_20260301_121000.png"
    )
    val sampleVideos = listOf("${MediaFileManager.VIDEOS_DIR}/VID_20260301_120500.mp4")
    val sampleAudios = listOf(
        "${MediaFileManager.AUDIOS_DIR}/AUD_20260301_120000.m4a",
        "${MediaFileManager.AUDIOS_DIR}/song.opus",
        "${MediaFileManager.AUDIOS_DIR}/theme.mid"
    )
    val samplePdfs = listOf("${MediaFileManager.DOCUMENTS_DIR}/DOC_20260301_notes.pdf")

    AppTheme(darkTheme = true) {
        MediaScreenContent(
            capturedPhotos = samplePhotos,
            recordedVideos = sampleVideos,
            recordedAudios = sampleAudios,
            pdfDocuments = samplePdfs,
            isRecording = false,
            isGridView = true,
            selectedFilter = MediaFilter.All
        )
    }
}

@Preview(
    showBackground = true,
    name = "2. Media Screen - Grid Gallery (Light)",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
fun MediaScreenPreview_GridGallery_Light() {
    val samplePhotos = listOf("${MediaFileManager.PHOTOS_DIR}/IMG_20260301_120000.jpg")
    val sampleVideos = listOf("${MediaFileManager.VIDEOS_DIR}/VID_20260301_120500.mp4")
    val sampleAudios = listOf("${MediaFileManager.AUDIOS_DIR}/song.flac")

    AppTheme(darkTheme = false) {
        MediaScreenContent(
            capturedPhotos = samplePhotos,
            recordedVideos = sampleVideos,
            recordedAudios = sampleAudios,
            isRecording = false,
            isGridView = true,
            selectedFilter = MediaFilter.Audio
        )
    }
}

@Preview(
    showBackground = true,
    name = "3. Media Screen - List Populated (Dark)",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun MediaScreenPreview_Populated_Dark() {
    val samplePhotos = listOf("${MediaFileManager.PHOTOS_DIR}/IMG_20260301_120000.jpg")
    val sampleVideos = listOf("${MediaFileManager.VIDEOS_DIR}/VID_20260301_120500.mp4")
    val sampleAudios = listOf("${MediaFileManager.AUDIOS_DIR}/REC_20260301_120000.m4a")

    AppTheme(darkTheme = true) {
        MediaScreenContent(
            capturedPhotos = samplePhotos,
            recordedVideos = sampleVideos,
            recordedAudios = sampleAudios,
            isRecording = false,
            isGridView = false
        )
    }
}

@Preview(
    showBackground = true,
    name = "4. Media Screen - List Populated (Light)",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
fun MediaScreenPreview_Populated_Light() {
    val samplePhotos = listOf("${MediaFileManager.PHOTOS_DIR}/IMG_20260301_120000.jpg")
    val sampleVideos = listOf("${MediaFileManager.VIDEOS_DIR}/VID_20260301_120500.mp4")
    val sampleAudios = listOf("${MediaFileManager.AUDIOS_DIR}/REC_20260301_120000.m4a")

    AppTheme(darkTheme = false) {
        MediaScreenContent(
            capturedPhotos = samplePhotos,
            recordedVideos = sampleVideos,
            recordedAudios = sampleAudios,
            isRecording = false,
            isGridView = false
        )
    }
}

@Preview(
    showBackground = true,
    name = "5. Media Screen - Active Recording (Dark)",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun MediaScreenPreview_ActiveRecording_Dark() {
    AppTheme(darkTheme = true) {
        MediaScreenContent(
            capturedPhotos = emptyList(),
            recordedAudios = emptyList(),
            isRecording = true,
            isPaused = false,
            recordingDurationSeconds = 15,
            isGridView = false
        )
    }
}

@Preview(
    showBackground = true,
    name = "6. Media Screen - Active Recording (Light)",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
fun MediaScreenPreview_ActiveRecording_Light() {
    AppTheme(darkTheme = false) {
        MediaScreenContent(
            capturedPhotos = emptyList(),
            recordedAudios = emptyList(),
            isRecording = true,
            isPaused = false,
            recordingDurationSeconds = 15,
            isGridView = false
        )
    }
}

@Preview(
    showBackground = true,
    name = "7. Media Screen - Empty State (Dark)",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun MediaScreenPreview_EmptyState_Dark() {
    AppTheme(darkTheme = true) {
        MediaScreenContent(
            capturedPhotos = emptyList(),
            recordedAudios = emptyList(),
            isRecording = false,
            isGridView = true
        )
    }
}

@Preview(
    showBackground = true,
    name = "8. Media Screen - Empty State (Light)",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
fun MediaScreenPreview_EmptyState_Light() {
    AppTheme(darkTheme = false) {
        MediaScreenContent(
            capturedPhotos = emptyList(),
            recordedAudios = emptyList(),
            isRecording = false,
            isGridView = true
        )
    }
}
