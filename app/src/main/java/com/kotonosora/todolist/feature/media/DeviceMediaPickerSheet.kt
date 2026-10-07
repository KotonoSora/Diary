package com.kotonosora.todolist.feature.media

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.kotonosora.todolist.data.file.DeviceMediaItem
import com.kotonosora.todolist.data.file.DeviceMediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class DeviceMediaTab { All, Photos, Videos, Audio, Pdfs }

/**
 * In-app device-media picker: grid/list sourced exclusively from
 * `MediaStore` (on-device files only). No system picker is involved, so
 * Google Photos / Drive can never appear here — unlike `ACTION_GET_CONTENT`,
 * which always offers them as sources even with `EXTRA_LOCAL_ONLY`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceMediaPickerSheet(
    initialTab: DeviceMediaTab,
    onDismiss: () -> Unit,
    onImport: (List<Uri>) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var allItems by remember { mutableStateOf<List<DeviceMediaItem>?>(null) }
    var tab by remember { mutableStateOf(initialTab) }
    var selected by remember { mutableStateOf(setOf<Uri>()) }

    LaunchedEffect(Unit) {
        allItems = withContext(Dispatchers.IO) {
            try {
                DeviceMediaStore.queryDeviceMedia(context.applicationContext)
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Pick from this device",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Text(
                text = "Only files stored on this device are shown.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DeviceMediaTab.entries.forEach { entry ->
                    FilterChip(
                        selected = tab == entry,
                        onClick = { tab = entry },
                        label = { Text(entry.name) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            val items = allItems
            when {
                items == null -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                else -> {
                    val visible = items.filter {
                        when (tab) {
                            DeviceMediaTab.All -> true
                            DeviceMediaTab.Photos -> it.type == "photo"
                            DeviceMediaTab.Videos -> it.type == "video"
                            DeviceMediaTab.Audio -> it.type == "audio"
                            DeviceMediaTab.Pdfs -> it.type == "pdf"
                        }
                    }
                    if (visible.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Nothing on this device yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else if (tab == DeviceMediaTab.Audio || tab == DeviceMediaTab.Pdfs) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(380.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(visible, key = { it.uri.toString() }) { item ->
                                val isSelected = item.uri in selected
                                PickerDocRow(
                                    item = item,
                                    icon = if (item.type == "pdf") Icons.Default.PictureAsPdf
                                    else Icons.Default.AudioFile,
                                    isSelected = isSelected,
                                    onToggle = {
                                        selected = if (isSelected) selected - item.uri
                                        else selected + item.uri
                                    }
                                )
                            }
                        }
                    } else {
                        // Visual grid (photos + videos) with document rows for
                        // audio/PDF underneath, so every type renders its own
                        // tile even in the All tab.
                        val visual = visible.filter { it.type == "photo" || it.type == "video" }
                        val docs = visible.filter { it.type != "photo" && it.type != "video" }
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(380.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(visual.chunked(3)) { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowItems.forEach { item ->
                                        val isSelected = item.uri in selected
                                        val onToggle = {
                                            selected = if (isSelected) selected - item.uri
                                            else selected + item.uri
                                        }
                                        if (item.type == "photo") {
                                            PickerPhotoTile(
                                                item = item,
                                                isSelected = isSelected,
                                                onToggle = onToggle,
                                                modifier = Modifier.weight(1f)
                                            )
                                        } else {
                                            PickerVideoTile(
                                                item = item,
                                                isSelected = isSelected,
                                                onToggle = onToggle,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                    repeat(3 - rowItems.size) {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                            items(docs, key = { it.uri.toString() }) { item ->
                                val isSelected = item.uri in selected
                                PickerDocRow(
                                    item = item,
                                    icon = if (item.type == "pdf") Icons.Default.PictureAsPdf
                                    else Icons.Default.AudioFile,
                                    isSelected = isSelected,
                                    onToggle = {
                                        selected = if (isSelected) selected - item.uri
                                        else selected + item.uri
                                    }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { onImport(selected.toList()) },
                        enabled = selected.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .height(50.dp)
                    ) {
                        Text(if (selected.isEmpty()) "Select files to import" else "Import ${selected.size} file(s)")
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerPhotoTile(
    item: DeviceMediaItem,
    isSelected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onToggle() }
            .then(
                if (isSelected) Modifier.border(
                    3.dp,
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(12.dp)
                ) else Modifier
            )
    ) {
        AsyncImage(
            model = item.uri,
            contentDescription = item.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        if (isSelected) PickerSelectedBadge(Modifier.align(Alignment.TopEnd))
    }
}

/**
 * Video tile with a real preview thumbnail loaded from `MediaStore` via
 * `ContentResolver.loadThumbnail` (API 29+; icon fallback below that or
 * while loading / on failure).
 */
@Composable
private fun PickerVideoTile(
    item: DeviceMediaItem,
    isSelected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    var thumbnail by remember(item.uri) { mutableStateOf<android.graphics.Bitmap?>(null) }
    val resolver = LocalContext.current.contentResolver
    LaunchedEffect(item.uri) {
        thumbnail = withContext(Dispatchers.IO) {
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    resolver.loadThumbnail(item.uri, android.util.Size(320, 320), null)
                } else null
            } catch (_: Exception) {
                null
            }
        }
    }
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onToggle() }
            .then(
                if (isSelected) Modifier.border(
                    3.dp,
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(12.dp)
                ) else Modifier
            )
    ) {
        val thumb = thumbnail
        if (thumb != null) {
            androidx.compose.foundation.Image(
                bitmap = thumb.asImageBitmap(),
                contentDescription = item.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Videocam,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        if (item.durationMs > 0) {
            Text(
                text = formatPickerDuration(item.durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .background(
                        androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.65f),
                        RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
        if (isSelected) PickerSelectedBadge(Modifier.align(Alignment.TopEnd))
    }
}

@Composable
private fun PickerSelectedBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(6.dp)
            .background(MaterialTheme.colorScheme.primary, CircleShape)
            .padding(4.dp)
    ) {
        Icon(
            Icons.Default.Check,
            contentDescription = "Selected",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun PickerDocRow(
    item: DeviceMediaItem,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            .clickable { onToggle() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (item.durationMs > 0) {
                Text(
                    text = formatPickerDuration(item.durationMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (item.sizeBytes > 0) {
                Text(
                    text = formatPickerSize(item.sizeBytes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (isSelected) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun formatPickerDuration(durationMs: Long): String {
    val totalSec = (durationMs / 1000).toInt().coerceAtLeast(0)
    return String.format(
        com.kotonosora.todolist.common.AppConstants.APP_LOCALE,
        "%02d:%02d",
        totalSec / 60,
        totalSec % 60
    )
}

private fun formatPickerSize(bytes: Long): String {
    if (bytes <= 0) return ""
    val mb = bytes / (1024f * 1024f)
    return if (mb >= 1) {
        String.format(
            com.kotonosora.todolist.common.AppConstants.APP_LOCALE,
            "%.1f MB",
            mb
        )
    } else {
        String.format(
            com.kotonosora.todolist.common.AppConstants.APP_LOCALE,
            "%d KB",
            (bytes / 1024).coerceAtLeast(1)
        )
    }
}

/**
 * Read permissions required to list on-device media through `MediaStore`:
 * granular media permissions on API 33+, broad storage below. Scoped per
 * tab so denying one media class never blocks picking another.
 */
fun permissionsForTab(tab: DeviceMediaTab): Array<String> {
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
        return arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    return when (tab) {
        DeviceMediaTab.Photos -> arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES)
        DeviceMediaTab.Videos -> arrayOf(android.Manifest.permission.READ_MEDIA_VIDEO)
        DeviceMediaTab.Audio -> arrayOf(android.Manifest.permission.READ_MEDIA_AUDIO)
        // All/Pdfs span every class (PDF visibility itself is best-effort on
        // API 33+, where MediaStore.Files only exposes accessible files).
        DeviceMediaTab.All,
        DeviceMediaTab.Pdfs -> arrayOf(
            android.Manifest.permission.READ_MEDIA_IMAGES,
            android.Manifest.permission.READ_MEDIA_VIDEO,
            android.Manifest.permission.READ_MEDIA_AUDIO
        )
    }
}

fun hasDeviceMediaAccess(
    context: android.content.Context,
    tab: DeviceMediaTab = DeviceMediaTab.All
): Boolean =
    permissionsForTab(tab).all {
        androidx.core.content.ContextCompat.checkSelfPermission(context, it) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
    }
