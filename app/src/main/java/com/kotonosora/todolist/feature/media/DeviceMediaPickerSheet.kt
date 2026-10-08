package com.kotonosora.todolist.feature.media

import android.content.res.Configuration
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.kotonosora.todolist.common.AppConstants
import com.kotonosora.todolist.domain.model.DeviceMediaFile
import com.kotonosora.todolist.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class DeviceMediaTab { All, Photos, Videos, Audio, Pdfs }

/**
 * In-app device-media picker: grid/list sourced exclusively from
 * `MediaStore` (on-device files only). No system picker is involved, so
 * Google Photos / Drive can never appear here — unlike `ACTION_GET_CONTENT`,
 * which always offers them as sources even with `EXTRA_LOCAL_ONLY`.
 *
 * PDFs are best-effort in this sheet: on API 33+ scoped storage
 * `MediaStore.Files` no longer exposes PDFs/Downloads to normal apps, so the
 * Pdfs tab may be empty. Pass [onBrowsePdfs] to offer the SAF document
 * picker (`ACTION_OPEN_DOCUMENT`, `application/pdf`) as a fallback — Photos
 * holds no PDFs so it can never appear there.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceMediaPickerSheet(
    initialTab: DeviceMediaTab,
    onDismiss: () -> Unit,
    onImport: (List<Uri>) -> Unit,
    onBrowsePdfs: (() -> Unit)? = null,
    queryDeviceMedia: suspend () -> List<DeviceMediaFile>
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var allItems by remember { mutableStateOf<List<DeviceMediaFile>?>(null) }
    var tab by remember { mutableStateOf(initialTab) }
    var selected by remember { mutableStateOf(setOf<Uri>()) }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        allItems = withContext(Dispatchers.IO) {
            try {
                queryDeviceMedia()
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
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search by file name") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val counts = remember(allItems) {
                    val items = allItems.orEmpty()
                    mapOf(
                        DeviceMediaTab.All to items.size,
                        DeviceMediaTab.Photos to items.count { it.type == "photo" },
                        DeviceMediaTab.Videos to items.count { it.type == "video" },
                        DeviceMediaTab.Audio to items.count { it.type == "audio" },
                        DeviceMediaTab.Pdfs to items.count { it.type == "pdf" }
                    )
                }
                DeviceMediaTab.entries.forEach { entry ->
                    val count = counts[entry] ?: 0
                    FilterChip(
                        selected = tab == entry,
                        onClick = { tab = entry },
                        label = { Text("${entry.name} ($count)") }
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
                    val trimmedQuery = query.trim().lowercase(AppConstants.APP_LOCALE)
                    val visible = items.filter {
                        val tabOk = when (tab) {
                            DeviceMediaTab.All -> true
                            DeviceMediaTab.Photos -> it.type == "photo"
                            DeviceMediaTab.Videos -> it.type == "video"
                            DeviceMediaTab.Audio -> it.type == "audio"
                            DeviceMediaTab.Pdfs -> it.type == "pdf"
                        }
                        tabOk && (trimmedQuery.isBlank() ||
                                it.displayName.lowercase(AppConstants.APP_LOCALE)
                                    .contains(trimmedQuery))
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Selection is cumulative across tabs but Import acts
                        // on this view only (see button below), so show both
                        // counts — total alone would promise too much.
                        val visibleUris = visible.map { it.uri }.toSet()
                        val visibleSelectedCount = selected.count { it in visibleUris }
                        Text(
                            text = if (selected.isEmpty()) "${visible.size} file(s)"
                            else "$visibleSelectedCount in this view • ${selected.size} selected total",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row {
                            TextButton(
                                onClick = {
                                    selected = selected + visible.map { it.uri }
                                },
                                enabled = visible.isNotEmpty()
                            ) { Text("Select visible") }
                            if (selected.isNotEmpty()) {
                                TextButton(onClick = { selected = selected - visibleUris }) {
                                    Text("Clear visible")
                                }
                            }
                        }
                    }
                    if (visible.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (tab == DeviceMediaTab.Pdfs && onBrowsePdfs != null) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                ) {
                                    Text(
                                        text = if (query.isBlank()) "No PDFs found on this device."
                                        else "No matches for \"$query\".",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Downloads & documents aren't listed here on newer Android versions.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Button(onClick = onBrowsePdfs) {
                                        Text("Browse PDF files")
                                    }
                                }
                            } else {
                                Text(
                                    text = if (query.isBlank()) "Nothing on this device yet."
                                    else "No matches for \"$query\".",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
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
                    if (tab == DeviceMediaTab.Pdfs && onBrowsePdfs != null) {
                        TextButton(
                            onClick = onBrowsePdfs,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                        ) {
                            Text("Can't find it? Browse PDF files")
                        }
                    }
                    // Import exactly what is visible: selection is cumulative
                    // across tabs, but the button sits under this list —
                    // importing hidden rows would surprise. The All tab still
                    // covers multi-type picks in one go. The header above
                    // shows both the in-view and total counts.
                    val visibleUris = visible.map { it.uri }.toSet()
                    val visibleSelected = selected.filter { it in visibleUris }
                    Button(
                        onClick = { onImport(visibleSelected) },
                        enabled = visibleSelected.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .height(50.dp)
                    ) {
                        Text(if (visibleSelected.isEmpty()) "Select files to import" else "Import ${visibleSelected.size} file(s) in this view")
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerPhotoTile(
    item: DeviceMediaFile,
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
    item: DeviceMediaFile,
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
    item: DeviceMediaFile,
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
        AppConstants.APP_LOCALE,
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
            AppConstants.APP_LOCALE,
            "%.1f MB",
            mb
        )
    } else {
        String.format(
            AppConstants.APP_LOCALE,
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

// ── PREVIEWS: static replicas only — the sheet itself queries MediaStore via
// a suspend lambda (spinner in preview) and photo tiles load Coil images, so
// previews render the stateless row/body pieces instead. ──

private fun previewPickerAudio(): DeviceMediaFile = DeviceMediaFile(
    uri = Uri.parse("content://preview/audio1.mp3"),
    displayName = "audio1.mp3",
    mimeType = "audio/mpeg",
    type = "audio",
    dateAddedSec = 1750000000L,
    sizeBytes = 2500000L,
    durationMs = 95000L
)

private fun previewPickerPdf(): DeviceMediaFile = DeviceMediaFile(
    uri = Uri.parse("content://preview/notes.pdf"),
    displayName = "notes.pdf",
    mimeType = "application/pdf",
    type = "pdf",
    dateAddedSec = 1750000000L,
    sizeBytes = 1200000L
)

@Preview(
    showBackground = true,
    name = "1. DeviceMediaPicker Audio Selected - Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun DeviceMediaPickerAudioSelectedPreview_Dark() {
    AppTheme(darkTheme = true) {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                PickerDocRow(
                    item = previewPickerAudio(),
                    icon = Icons.Default.AudioFile,
                    isSelected = true,
                    onToggle = {}
                )
            }
        }
    }
}

@Preview(
    showBackground = true,
    name = "2. DeviceMediaPicker Audio Selected - Light",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
fun DeviceMediaPickerAudioSelectedPreview_Light() {
    AppTheme(darkTheme = false) {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                PickerDocRow(
                    item = previewPickerAudio(),
                    icon = Icons.Default.AudioFile,
                    isSelected = true,
                    onToggle = {}
                )
            }
        }
    }
}

@Preview(
    showBackground = true,
    name = "3. DeviceMediaPicker Pdf Unselected - Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun DeviceMediaPickerPdfUnselectedPreview_Dark() {
    AppTheme(darkTheme = true) {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                PickerDocRow(
                    item = previewPickerPdf(),
                    icon = Icons.Default.PictureAsPdf,
                    isSelected = false,
                    onToggle = {}
                )
            }
        }
    }
}

@Preview(
    showBackground = true,
    name = "4. DeviceMediaPicker Pdf Unselected - Light",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
fun DeviceMediaPickerPdfUnselectedPreview_Light() {
    AppTheme(darkTheme = false) {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                PickerDocRow(
                    item = previewPickerPdf(),
                    icon = Icons.Default.PictureAsPdf,
                    isSelected = false,
                    onToggle = {}
                )
            }
        }
    }
}

@Preview(
    showBackground = true,
    name = "5. DeviceMediaPicker Sheet Body - Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun DeviceMediaPickerBodyPreview_Dark() {
    AppTheme(darkTheme = true) {
        Surface {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PickerDocRow(
                        item = previewPickerAudio(),
                        icon = Icons.Default.AudioFile,
                        isSelected = true,
                        onToggle = {}
                    )
                    PickerDocRow(
                        item = previewPickerPdf(),
                        icon = Icons.Default.PictureAsPdf,
                        isSelected = false,
                        onToggle = {}
                    )
                }
                Button(
                    onClick = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(50.dp)
                ) {
                    Text("Import 1 file(s) in this view")
                }
            }
        }
    }
}

@Preview(
    showBackground = true,
    name = "6. DeviceMediaPicker Sheet Body - Light",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
fun DeviceMediaPickerBodyPreview_Light() {
    AppTheme(darkTheme = false) {
        Surface {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PickerDocRow(
                        item = previewPickerAudio(),
                        icon = Icons.Default.AudioFile,
                        isSelected = true,
                        onToggle = {}
                    )
                    PickerDocRow(
                        item = previewPickerPdf(),
                        icon = Icons.Default.PictureAsPdf,
                        isSelected = false,
                        onToggle = {}
                    )
                }
                Button(
                    onClick = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(50.dp)
                ) {
                    Text("Import 1 file(s) in this view")
                }
            }
        }
    }
}

@Preview(
    showBackground = true,
    name = "7. DeviceMediaPicker Empty State - Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun DeviceMediaPickerEmptyPreview_Dark() {
    AppTheme(darkTheme = true) {
        Surface {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nothing on this device yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(50.dp)
                ) {
                    Text("Select files to import")
                }
            }
        }
    }
}

@Preview(
    showBackground = true,
    name = "8. DeviceMediaPicker Empty State - Light",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
fun DeviceMediaPickerEmptyPreview_Light() {
    AppTheme(darkTheme = false) {
        Surface {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nothing on this device yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(50.dp)
                ) {
                    Text("Select files to import")
                }
            }
        }
    }
}
