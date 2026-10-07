package com.kotonosora.todolist.feature.media

import android.content.res.Configuration
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.kotonosora.todolist.ui.components.AudioPlayerView
import com.kotonosora.todolist.ui.components.VideoPlayerView
import com.kotonosora.todolist.ui.theme.AppTheme
import java.io.File

/**
 * Gallery filter — English labels throughout (see AppConstants.APP_LOCALE).
 */
enum class MediaFilter(val label: String) {
    All("All"),
    Photos("Photos"),
    Videos("Videos"),
    Audio("Audio"),
    Pdfs("PDFs")
}

/**
 * Unified gallery item for the grid. [type] is one of
 * `photo` / `video` / `audio` / `pdf` (matches `media_attachments.type`).
 */
data class GalleryMediaItem(
    val path: String,
    val type: String
)

fun collectGalleryItems(
    photos: List<String>,
    videos: List<String>,
    audios: List<String>,
    pdfs: List<String>,
    filter: MediaFilter
): List<GalleryMediaItem> {
    val all = mutableListOf<GalleryMediaItem>()
    if (filter == MediaFilter.All || filter == MediaFilter.Photos) {
        all += photos.map { GalleryMediaItem(it, "photo") }
    }
    if (filter == MediaFilter.All || filter == MediaFilter.Videos) {
        all += videos.map { GalleryMediaItem(it, "video") }
    }
    if (filter == MediaFilter.All || filter == MediaFilter.Audio) {
        all += audios.map { GalleryMediaItem(it, "audio") }
    }
    if (filter == MediaFilter.All || filter == MediaFilter.Pdfs) {
        all += pdfs.map { GalleryMediaItem(it, "pdf") }
    }
    // Newest-first by embedded capture timestamp (IMG_/VID_/AUD_/DOC_ +
    // yyyyMMdd_HHmmss). Sorting by full path or display label would group by
    // type prefix / subfolder instead of time; input lists are already
    // createdAt DESC per type, this restores global DESC across types.
    return all.sortedByDescending { galleryTimestampKey(it.path) }
}

private val GALLERY_TS_REGEX = Regex("""(IMG|VID|AUD|REC|DOC)_(\d{8}_\d{6}(?:_\d+)?)""")

internal fun galleryTimestampKey(path: String): String {
    val encodedLast = path.substringAfterLast("/")
        .replace("%2F", "/")
        .replace("%2f", "/")
        .substringAfterLast("/")
    val name = try {
        java.net.URLDecoder.decode(encodedLast, "UTF-8")
    } catch (_: Exception) {
        encodedLast
    }
    return GALLERY_TS_REGEX.find(name)?.groupValues?.getOrNull(2) ?: name
}

@Composable
fun MediaFilterRow(
    selected: MediaFilter,
    onSelect: (MediaFilter) -> Unit,
    counts: Map<MediaFilter, Int> = emptyMap(),
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MediaFilter.entries.forEach { filter ->
            val count = counts[filter]
            FilterChip(
                selected = selected == filter,
                onClick = { onSelect(filter) },
                label = {
                    Text(
                        if (count != null) "${filter.label} ($count)" else filter.label,
                        maxLines = 1
                    )
                }
            )
        }
    }
}

/**
 * Gallery grid rendered as a non-lazy Column of Rows (3 cells each).
 *
 * This grid is always hosted inside the outer LazyColumn in MediaScreen, so a
 * LazyVerticalGrid here would nest scrollables with unbounded height
 * constraints (measure crash / clipped cells / scroll fighting). Chunked Rows
 * avoid nesting entirely; photos + video thumbnails still load via Coil
 * (memory/disk cached, off Main; video frames via `coil-video`), while audio
 * and PDF cells are lightweight icon tiles. Tapping a cell opens the matching
 * full preview dialog — no ExoPlayer instance per cell.
 */
@Composable
fun MediaGalleryGrid(
    items: List<GalleryMediaItem>,
    onPhotoClick: (String) -> Unit = {},
    onVideoClick: (String) -> Unit = {},
    onAudioClick: (String) -> Unit = {},
    onPdfClick: (String) -> Unit = {},
    onDelete: (GalleryMediaItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) {
        Text(
            "No media in this view yet. Capture or import files to fill the gallery.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(vertical = 8.dp)
        )
        return
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (rowItems in items.chunked(3)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (item in rowItems) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                    ) {
                        MediaGridCell(
                            item = item,
                            onClick = {
                                when (item.type) {
                                    "photo" -> onPhotoClick(item.path)
                                    "video" -> onVideoClick(item.path)
                                    "audio" -> onAudioClick(item.path)
                                    else -> onPdfClick(item.path)
                                }
                            },
                            onDelete = { onDelete(item) }
                        )
                    }
                }
                // Keep 3-column alignment on the last partial row.
                repeat(3 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaGridCell(
    item: GalleryMediaItem,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .combinedClickable(onClick = onClick, onLongClick = onDelete),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (item.type) {
                "photo" -> PhotoGridThumbnail(path = item.path)
                "video" -> VideoGridThumbnail(path = item.path)
                "audio" -> AudioGridTile(path = item.path)
                else -> PdfGridTile(path = item.path)
            }
            // Delete affordance (long-press also deletes for touch users).
            Surface(
                color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f),
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(24.dp)
                    .clickable { onDelete() }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun gridImageModel(path: String): Any {
    return remember(path) {
        if (path.startsWith("content://")) Uri.parse(path) else File(path)
    }
}

@Composable
private fun PhotoGridThumbnail(path: String) {
    if (LocalInspectionMode.current) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                formatMediaDisplayName(path, false),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(6.dp)
            )
        }
        return
    }
    AsyncImage(
        model = gridImageModel(path),
        contentDescription = formatMediaDisplayName(path, false),
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
    )
}

@Composable
private fun VideoGridThumbnail(path: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (LocalInspectionMode.current) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PlayCircleFilled,
                    contentDescription = "Video",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(32.dp)
                )
            }
        } else {
            // Coil-video resolves a frame via MediaMetadataRetriever (cached).
            AsyncImage(
                model = gridImageModel(path),
                contentDescription = formatMediaDisplayName(path, false),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f),
                    shape = CircleShape
                ) {
                    Icon(
                        Icons.Default.PlayCircleFilled,
                        contentDescription = "Play video",
                        tint = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .padding(4.dp)
                            .size(28.dp)
                    )
                }
            }
        }
        Text(
            text = formatMediaDisplayName(path, false),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.surface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun AudioGridTile(path: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Audiotrack,
            contentDescription = "Audio",
            tint = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.size(30.dp)
        )
        Text(
            text = formatMediaDisplayName(path, true),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PdfGridTile(path: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.PictureAsPdf,
            contentDescription = "PDF document",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(30.dp)
        )
        Text(
            text = formatMediaDisplayName(path, false),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Full preview for a grid video cell — ExoPlayer is created only for the open
 * dialog, not per grid cell.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPreviewDialog(
    filePath: String,
    onDismiss: () -> Unit,
    onDelete: ((String) -> Unit)? = null
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = null
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close video preview")
                    }
                    Text(
                        formatMediaDisplayName(filePath, false),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )
                    if (onDelete != null) {
                        IconButton(onClick = { onDelete(filePath); onDismiss() }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete video",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                ) {
                    if (!LocalInspectionMode.current) {
                        VideoPlayerView(filePath = filePath)
                    }
                }
            }
        }
    }
}

/**
 * Full preview for a grid audio cell — waveform + transport controls.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPreviewDialog(
    filePath: String,
    onDismiss: () -> Unit,
    onDelete: ((String) -> Unit)? = null
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            formatMediaDisplayName(filePath, true),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    Row {
                        if (onDelete != null) {
                            IconButton(onClick = { onDelete(filePath); onDismiss() }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete audio",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close audio preview")
                        }
                    }
                }
                if (!LocalInspectionMode.current) {
                    AudioPlayerView(filePath = filePath)
                }
            }
        }
    }
}

@Preview(
    showBackground = true,
    name = "1. Gallery Grid - Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun MediaGalleryGridPreview_Dark() {
    AppTheme(darkTheme = true) {
        Column(Modifier.padding(12.dp)) {
            MediaFilterRow(selected = MediaFilter.All, onSelect = {})
            MediaGalleryGrid(
                items = listOf(
                    GalleryMediaItem("photos/IMG_20260301_120000.jpg", "photo"),
                    GalleryMediaItem("videos/VID_20260301_120500.mp4", "video"),
                    GalleryMediaItem("audios/AUD_20260301_121000.m4a", "audio"),
                    GalleryMediaItem("audios/song.opus", "audio"),
                    GalleryMediaItem("audios/theme.mid", "audio"),
                    GalleryMediaItem("documents/DOC_20260301_notes.pdf", "pdf")
                )
            )
        }
    }
}

@Preview(
    showBackground = true,
    name = "2. Gallery Grid - Light",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
fun MediaGalleryGridPreview_Light() {
    AppTheme(darkTheme = false) {
        Column(Modifier.padding(12.dp)) {
            MediaFilterRow(selected = MediaFilter.Audio, onSelect = {})
            MediaGalleryGrid(
                items = listOf(
                    GalleryMediaItem("audios/voice.mp3", "audio"),
                    GalleryMediaItem("audios/song.flac", "audio"),
                    GalleryMediaItem("audios/theme.mid", "audio")
                )
            )
        }
    }
}
