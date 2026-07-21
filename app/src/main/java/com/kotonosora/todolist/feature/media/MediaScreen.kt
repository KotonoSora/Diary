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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaScreen(viewModel: MediaViewModel = viewModel()) {
    val context = LocalContext.current
    val capturedPhotos by viewModel.capturedPhotoPaths.collectAsState()
    val recordedAudios by viewModel.recordedAudioPaths.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()

    var photoUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) photoUri?.path?.let { viewModel.onPhotoCaptured(it) }
    }

    val multiplePermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] == true
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] == true
        if (cameraGranted) {
            val imageFile = File(
                context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES),
                "IMG_${System.currentTimeMillis()}.jpg"
            )
            val uri = FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", imageFile
            )
            photoUri = uri
            cameraLauncher.launch(uri)
        }
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
                        multiplePermissionsLauncher.launch(
                            arrayOf(Manifest.permission.CAMERA)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("📷  Take Photo")
                }
            }
            items(capturedPhotos) { path ->
                MediaFileItem(
                    name = File(path).name,
                    subtitle = path,
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
                MediaFileItem(
                    name = File(path).name,
                    subtitle = path,
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
}

@Composable
private fun MediaFileItem(name: String, subtitle: String, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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

