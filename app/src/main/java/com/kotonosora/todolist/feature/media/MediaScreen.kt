package com.kotonosora.todolist.feature.media

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.kotonosora.todolist.ui.components.AudioPlayerView
import com.kotonosora.todolist.ui.components.CameraCaptureView
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaScreen(viewModel: MediaViewModel = viewModel()) {
    val capturedPhotos by viewModel.capturedPhotoPaths.collectAsState()
    val recordedAudios by viewModel.recordedAudioPaths.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val customFolderUri by viewModel.customFolderUri.collectAsState()

    var showCameraView by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) showCameraView = true
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.startRecording()
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Media") }) }) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            // ── Photos section ──
            item {
                Text("Photos", style = MaterialTheme.typography.titleMedium)
            }
            item {
                Button(
                    onClick = {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("📷  Take Photo")
                }
            }
            items(capturedPhotos) { path ->
                MediaFileItem(
                    name = formatMediaDisplayName(path, isAudio = false),
                    subtitle = path,
                    filePath = path,
                    onDelete = { viewModel.deletePhoto(path) }
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
                Text("Audio Recordings", style = MaterialTheme.typography.titleMedium)
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                        enabled = !isRecording,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("🎙  Start Recording")
                    }
                    OutlinedButton(
                        onClick = { viewModel.stopRecording() },
                        enabled = isRecording,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("⏹  Stop")
                    }
                }
            }
            if (isRecording) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Recording…",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            items(recordedAudios) { path ->
                AudioFileItem(
                    name = formatMediaDisplayName(path, isAudio = true),
                    filePath = path,
                    onDelete = { viewModel.deleteAudio(path) }
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

    // Camera Capture Dialog
    if (showCameraView) {
        Dialog(
            onDismissRequest = { showCameraView = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            CameraCaptureView(
                onPhotoCaptured = { pathStr ->
                    viewModel.onPhotoCaptured(pathStr)
                    showCameraView = false
                },
                onDismiss = { showCameraView = false },
                customFolderUriStr = customFolderUri
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
                Text(name, style = MaterialTheme.typography.bodyMedium)
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            AudioPlayerView(filePath = filePath)
        }
    }
}

@Composable
private fun MediaFileItem(
    name: String,
    subtitle: String,
    filePath: String? = null,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
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
                    modifier = Modifier.size(60.dp)
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
                Text(name, style = MaterialTheme.typography.bodyMedium)
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

@Preview(showBackground = true, name = "Media Item Preview")
@Composable
fun MediaFileItemPreview() {
    MaterialTheme {
        MediaFileItem(
            name = "Photo - Mar 1, 2026",
            subtitle = "_assets/IMG_20260301_120000.jpg",
            filePath = null,
            onDelete = {}
        )
    }
}
