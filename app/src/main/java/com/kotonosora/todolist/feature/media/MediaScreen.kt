package com.kotonosora.todolist.feature.media

import android.Manifest
import android.content.res.Configuration
import android.net.Uri
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.kotonosora.todolist.ui.components.AudioPlayerView
import com.kotonosora.todolist.ui.components.AudioWaveformBars
import com.kotonosora.todolist.ui.components.CameraCaptureView
import com.kotonosora.todolist.ui.components.ImageLightboxDialog
import com.kotonosora.todolist.ui.theme.TodoListTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaScreen(
    viewModel: MediaViewModel = viewModel(),
    onOpenDrawer: (() -> Unit)? = null
) {
    val capturedPhotos by viewModel.capturedPhotoPaths.collectAsState()
    val recordedAudios by viewModel.recordedAudioPaths.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val isPaused by viewModel.isPaused.collectAsState()
    val recordingDuration by viewModel.recordingDurationSeconds.collectAsState()
    val customFolderUri by viewModel.customFolderUri.collectAsState()

    var showCameraSheet by remember { mutableStateOf(false) }
    var selectedLightboxPhoto by remember { mutableStateOf<String?>(null) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) showCameraSheet = true
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.startRecording()
    }

    MediaScreenContent(
        capturedPhotos = capturedPhotos,
        recordedAudios = recordedAudios,
        isRecording = isRecording,
        isPaused = isPaused,
        recordingDurationSeconds = recordingDuration,
        onCapturePhotoClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
        onStartRecordClick = { audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
        onPauseRecordClick = { viewModel.pauseRecording() },
        onResumeRecordClick = { viewModel.resumeRecording() },
        onStopRecordClick = { viewModel.stopRecording() },
        onDeletePhoto = { viewModel.deletePhoto(it) },
        onDeleteAudio = { viewModel.deleteAudio(it) },
        onPhotoClick = { selectedLightboxPhoto = it },
        onOpenDrawer = onOpenDrawer
    )

    // Full-screen Image Lightbox Dialog
    selectedLightboxPhoto?.let { photoPath ->
        ImageLightboxDialog(
            filePath = photoPath,
            onDismiss = { selectedLightboxPhoto = null }
        )
    }

    // Camera Capture Bottom Sheet
    if (showCameraSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showCameraSheet = false },
            sheetState = sheetState
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(480.dp)
            ) {
                CameraCaptureView(
                    onPhotoCaptured = { pathStr ->
                        viewModel.onPhotoCaptured(pathStr)
                        showCameraSheet = false
                    },
                    onDismiss = { showCameraSheet = false },
                    customFolderUriStr = customFolderUri
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
    isRecording: Boolean,
    isPaused: Boolean = false,
    recordingDurationSeconds: Int = 0,
    onCapturePhotoClick: () -> Unit = {},
    onStartRecordClick: () -> Unit = {},
    onPauseRecordClick: () -> Unit = {},
    onResumeRecordClick: () -> Unit = {},
    onStopRecordClick: () -> Unit = {},
    onDeletePhoto: (String) -> Unit = {},
    onDeleteAudio: (String) -> Unit = {},
    onPhotoClick: (String) -> Unit = {},
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
                        fontWeight = FontWeight.Bold
                    )
                }
            }

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
                    IconButton(
                        onClick = onCapturePhotoClick,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Icon(
                            Icons.Default.PhotoCamera,
                            contentDescription = "Capture Photo",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
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
                            AudioWaveformBars(isPlaying = !isPaused)
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
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

private fun formatRecordingTimer(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
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

    val regex = Regex("""(IMG|AUD|REC)_(\d{8}_\d{6}|\d+)""")
    val match = regex.find(fileName)

    if (match != null) {
        val type = match.groupValues[1]
        val rawTime = match.groupValues[2]

        val typeLabel = when (type) {
            "IMG" -> "Photo"
            "AUD", "REC" -> "Audio"
            else -> if (isAudio) "Audio" else "Photo"
        }

        if (rawTime.contains("_")) {
            try {
                val inputFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                val outputFormat = SimpleDateFormat("MMM d, yyyy, h:mm a", Locale.getDefault())
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
                    val outputFormat = SimpleDateFormat("MMM d, yyyy, h:mm a", Locale.getDefault())
                    return "$typeLabel - ${outputFormat.format(Date(millis))}"
                } catch (e: Exception) {
                    // ignore
                }
            }
        }
    }

    val prefix = if (isAudio) "Audio - " else "Photo - "
    return prefix + fileName.removeSuffix(".jpg").removeSuffix(".png").removeSuffix(".m4a")
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
    name = "1. Media Screen - Populated (Dark)",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun MediaScreenPreview_Populated_Dark() {
    val samplePhotos = listOf("_assets/IMG_20260301_120000.jpg")
    val sampleAudios = listOf("_assets/REC_20260301_120000.m4a")

    TodoListTheme(darkTheme = true) {
        MediaScreenContent(
            capturedPhotos = samplePhotos,
            recordedAudios = sampleAudios,
            isRecording = false
        )
    }
}

@Preview(
    showBackground = true,
    name = "2. Media Screen - Active Recording (Dark)",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun MediaScreenPreview_ActiveRecording_Dark() {
    TodoListTheme(darkTheme = true) {
        MediaScreenContent(
            capturedPhotos = emptyList(),
            recordedAudios = emptyList(),
            isRecording = true,
            isPaused = false,
            recordingDurationSeconds = 15
        )
    }
}

@Preview(showBackground = true, name = "3. Media Screen - Empty State")
@Composable
fun MediaScreenPreview_EmptyState() {
    TodoListTheme(darkTheme = true) {
        MediaScreenContent(
            capturedPhotos = emptyList(),
            recordedAudios = emptyList(),
            isRecording = false
        )
    }
}
