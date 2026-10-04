package com.kotonosora.todolist.ui.components

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kotonosora.todolist.ui.theme.AppTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Full-screen PDF reader dialog backed by the platform [PdfRenderer] (no extra
 * dependencies — the Jetpack `androidx.pdf` viewer is still beta and needs a
 * FragmentActivity, so it was deliberately not adopted).
 *
 * Performance: pages render once at display width into an LRU bitmap cache
 * (current + neighbors preloaded), so page turns after first view are instant.
 */
@Composable
fun PdfReaderDialog(
    filePath: String,
    onDismiss: () -> Unit
) {
    if (LocalInspectionMode.current) {
        // Preview placeholder: PdfRenderer needs a real file + native init.
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close PDF reader"
                        )
                    }
                    Text(
                        text = "Page 1 of 12",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(onClick = {}, enabled = false) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = Color.Transparent
                        )
                    }
                }
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = "PDF preview",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(48.dp)
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {}, enabled = false) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous page"
                        )
                    }
                    IconButton(onClick = {}, enabled = true) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next page"
                        )
                    }
                }
            }
        }
        return
    }
    val context = LocalContext.current
    val density = LocalDensity.current
    var pageIndex by rememberSaveable(filePath) { mutableIntStateOf(0) }
    var scale by remember(filePath) { mutableFloatStateOf(1f) }
    var offsetX by remember(filePath) { mutableFloatStateOf(0f) }
    var offsetY by remember(filePath) { mutableFloatStateOf(0f) }

    var handle by remember(filePath) { mutableStateOf<PdfHandle?>(null) }
    // Open the renderer off Main — ContentResolver + PdfRenderer init can block.
    LaunchedEffect(filePath) {
        handle = withContext(Dispatchers.IO) {
            openPdfRenderer(context.applicationContext, filePath)
        }
    }
    val pageCache = remember(filePath) { PdfPageCache() }
    DisposableEffect(filePath, handle) {
        onDispose {
            pageCache.evictAll()
            handle?.close()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top bar: close + page indicator
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            CircleShape
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close PDF reader"
                        )
                    }
                    val currentHandle = handle
                    if (currentHandle != null) {
                        Text(
                            text = "Page ${pageIndex + 1} of ${currentHandle.pageCount}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Balance row ends so the indicator stays centered
                    IconButton(onClick = {}, enabled = false) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = Color.Transparent
                        )
                    }
                }

                // Page canvas
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(0.8f, 5f)
                                if (scale > 1f) {
                                    offsetX += pan.x
                                    offsetY += pan.y
                                } else {
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val targetWidthPx = with(density) { maxWidth.toPx() }.toInt()
                        .coerceAtLeast(1)
                    var pageBitmap by remember(filePath, pageIndex, targetWidthPx) {
                        mutableStateOf<Bitmap?>(null)
                    }
                    LaunchedEffect(filePath, pageIndex, targetWidthPx) {
                        pageBitmap = withContext(Dispatchers.IO) {
                            handle?.page(
                                index = pageIndex,
                                targetWidthPx = targetWidthPx,
                                cache = pageCache
                            )
                        }
                        // Preload neighbors so the next turn is instant.
                        withContext(Dispatchers.IO) {
                            handle?.page(pageIndex - 1, targetWidthPx, pageCache)
                            handle?.page(pageIndex + 1, targetWidthPx, pageCache)
                        }
                    }

                    val bitmap = pageBitmap
                    val renderHandle = handle
                    if (renderHandle == null) {
                        Text(
                            text = "Couldn't open this PDF.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "PDF page ${pageIndex + 1}",
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offsetX,
                                    translationY = offsetY
                                )
                        )
                    }
                }

                // Bottom page navigation
                val navHandle = handle
                if (navHandle != null && navHandle.pageCount > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                pageIndex = (pageIndex - 1).coerceAtLeast(0)
                                scale = 1f
                                offsetX = 0f
                                offsetY = 0f
                            },
                            enabled = pageIndex > 0
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Previous page"
                            )
                        }
                        IconButton(
                            onClick = {
                                pageIndex = (pageIndex + 1).coerceAtMost(navHandle.pageCount - 1)
                                scale = 1f
                                offsetX = 0f
                                offsetY = 0f
                            },
                            enabled = pageIndex < navHandle.pageCount - 1
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next page"
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * In-memory bitmap cache keyed by page + render width (~3 display-sized pages).
 * Evicted bitmaps are left for GC — never recycled, since the same instance
 * may still be composed on screen.
 */
private class PdfPageCache : LruCache<String, Bitmap>(32 * 1024) {
    override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
}

private class PdfHandle(
    private val pfd: ParcelFileDescriptor,
    private val renderer: PdfRenderer
) {
    val pageCount: Int get() = renderer.pageCount

    /**
     * Returns the page rendered at [targetWidthPx] (aspect preserved), from cache
     * when available. Out-of-range indexes return null.
     */
    fun page(index: Int, targetWidthPx: Int, cache: PdfPageCache): Bitmap? {
        if (index !in 0 until renderer.pageCount) return null
        val key = "$index:$targetWidthPx"
        cache.get(key)?.let { return it }
        return try {
            renderer.openPage(index).use { page ->
                val scale = (targetWidthPx.toFloat() / page.width).coerceIn(0.5f, 4f)
                val width = (page.width * scale).toInt().coerceIn(1, 4096)
                val height = (page.height * scale).toInt().coerceIn(1, 4096)
                Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    cache.put(key, bitmap)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    fun close() {
        try {
            renderer.close()
        } catch (e: Exception) {
            // ignore
        }
        try {
            pfd.close()
        } catch (e: Exception) {
            // ignore
        }
    }
}

private fun openPdfRenderer(context: Context, filePath: String): PdfHandle? {
    return try {
        val pfd = if (filePath.startsWith("content://")) {
            context.contentResolver.openFileDescriptor(Uri.parse(filePath), "r")
        } else {
            val file = File(filePath)
            if (!file.exists()) return null
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        } ?: return null
        PdfHandle(pfd, PdfRenderer(pfd))
    } catch (e: Exception) {
        null
    }
}

@Preview(
    showBackground = true,
    name = "1. PDF Reader - Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun PdfReaderDialogPreview_Dark() {
    AppTheme(darkTheme = true) {
        PdfReaderDialog(filePath = "preview_sample.pdf", onDismiss = {})
    }
}

@Preview(
    showBackground = true,
    name = "2. PDF Reader - Light",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
fun PdfReaderDialogPreview_Light() {
    AppTheme(darkTheme = false) {
        PdfReaderDialog(filePath = "preview_sample.pdf", onDismiss = {})
    }
}
