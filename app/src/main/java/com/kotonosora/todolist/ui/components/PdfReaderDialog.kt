package com.kotonosora.todolist.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropPortrait
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kotonosora.todolist.domain.model.PdfBookmark
import com.kotonosora.todolist.feature.media.PdfReaderViewModel
import com.kotonosora.todolist.ui.components.pdf.PdfAddBookmarkDialog
import com.kotonosora.todolist.ui.components.pdf.PdfBookmarkSheet
import com.kotonosora.todolist.ui.components.pdf.PdfContinuousPage
import com.kotonosora.todolist.ui.components.pdf.PdfGoToPageDialog
import com.kotonosora.todolist.ui.components.pdf.PdfHandle
import com.kotonosora.todolist.ui.components.pdf.PdfPageCache
import com.kotonosora.todolist.ui.components.pdf.openPdfRenderer
import com.kotonosora.todolist.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

/**
 * Full-screen PDF reader dialog backed by the platform [PdfRenderer] (no extra
 * dependencies — the Jetpack `androidx.pdf` viewer is still beta and needs a
 * FragmentActivity, so it was deliberately not adopted).
 *
 * Continuous vertical scroll (default): every page renders at display width
 * into a shared LRU bitmap cache as its row composes, so long documents read
 * like an article instead of page-by-page taps. [PdfViewMode.SINGLE] shows
 * one page at a time with tap zones + buttons.
 */
/**
 * Reader view mode: continuous vertical scroll (default) or one page at a
 * time with tap zones + buttons.
 */
enum class PdfViewMode { SCROLL, SINGLE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfReaderDialog(
    filePath: String,
    onDismiss: () -> Unit,
    readerViewModel: PdfReaderViewModel? = null
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
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Continuous scroll preview",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        return
    }
    val context = LocalContext.current
    val density = LocalDensity.current
    var scale by remember(filePath) { mutableFloatStateOf(1f) }
    var offsetX by remember(filePath) { mutableFloatStateOf(0f) }
    var offsetY by remember(filePath) { mutableFloatStateOf(0f) }
    val listState = key(filePath) { rememberLazyListState() }
    val scope = rememberCoroutineScope()
    // Default mode is continuous scroll; single-page is one tap away.
    var viewMode by remember(filePath) { mutableStateOf(PdfViewMode.SCROLL) }
    var singlePageIndex by remember(filePath) { mutableStateOf(0) }
    // Live pinch scale (smooth visuals) vs committed zoom (crisp renders —
    // committing only on gesture end avoids a re-render storm mid-pinch).
    var committedZoom by remember(filePath) { mutableFloatStateOf(1f) }
    var showOverflow by remember(filePath) { mutableStateOf(false) }
    var showGoToPage by remember(filePath) { mutableStateOf(false) }
    var showBookmarks by remember(filePath) { mutableStateOf(false) }
    var showAddBookmark by remember(filePath) { mutableStateOf(false) }
    var bookmarks by remember(filePath) {
        mutableStateOf(emptyList<PdfBookmark>())
    }
    LaunchedEffect(filePath, readerViewModel) {
        readerViewModel?.bookmarks?.collect { bookmarks = it }
    }

    var handle by remember(filePath) { mutableStateOf<PdfHandle?>(null) }
    // Set once the async open finishes (success or failure) so the UI can
    // tell "still opening" (spinner) apart from "open failed" (error).
    var openFinished by remember(filePath) { mutableStateOf(false) }
    // Open the renderer off Main — ContentResolver + PdfRenderer init can block.
    LaunchedEffect(filePath) {
        handle = withContext(Dispatchers.IO) {
            openPdfRenderer(context.applicationContext, filePath)
        }
        openFinished = true
    }
    // PdfRenderer is not thread-safe: fast scrolling composes several page
    // rows at once, and overlapping native renders crash. Every renderer
    // call goes through this lock (see PdfHandle.page).
    val rendererLock = remember(filePath) { Mutex() }
    val pageCache = remember(filePath) { PdfPageCache() }
    // Snapshot the page count once the renderer is open. pageCount touches
    // native state that throws once the renderer is closed, so never read it
    // bare during composition — a dismiss racing a recomposition would crash.
    val pageCount: Int? = remember(handle, openFinished) {
        if (!openFinished) null else try {
            val count = handle?.pageCount
            count?.takeIf { it > 0 }
        } catch (_: Exception) {
            null
        }
    }
    // Snapshot the handle into a plain val INSIDE the effect: onDispose
    // must close the handle that was current when the effect was created.
    // Reading the `handle` state directly in onDispose re-reads LIVE state
    // at dispose time — when this effect restarts on the null → opened
    // transition, that immediately closes the just-opened renderer, so every
    // PDF opened fine (page count > 0) yet every page failed to render.
    DisposableEffect(filePath, handle) {
        val opened = handle
        onDispose {
            pageCache.evictAll()
            opened?.close()
        }
    }
    // Resume position: once the document AND the saved position have loaded,
    // jump once — scroll list, single-page state, or stay at 0 for a fresh
    // file. `restored` gates the auto-save below so it never persists page 0
    // over a real position before the restore runs.
    var restored by remember(filePath) { mutableStateOf(false) }
    LaunchedEffect(filePath, openFinished, pageCount) {
        if (!restored && openFinished && pageCount != null) {
            val vm = readerViewModel
            var target = 0
            if (vm != null) {
                try {
                    vm.positionLoaded.first { it }
                    target = vm.savedPage.value ?: 0
                } catch (_: Exception) {
                }
            }
            target = target.coerceIn(0, pageCount - 1)
            singlePageIndex = target
            if (target > 0) {
                try {
                    listState.scrollToItem(target)
                } catch (_: Exception) {
                }
            }
            restored = true
        }
    }
    val currentPage = if (viewMode == PdfViewMode.SCROLL) {
        listState.firstVisibleItemIndex
    } else {
        singlePageIndex
    }
    // Auto-save the position after it settles (restart cancels the pending
    // write, so fast scrolling saves once).
    LaunchedEffect(currentPage, restored) {
        if (!restored) return@LaunchedEffect
        delay(600)
        try {
            readerViewModel?.savePage(currentPage)
        } catch (_: Exception) {
        }
    }
    fun jumpTo(pageIndex: Int) {
        val count = pageCount ?: return
        val target = pageIndex.coerceIn(0, count - 1)
        singlePageIndex = target
        scope.launch {
            try {
                listState.scrollToItem(target)
            } catch (_: Exception) {
            }
        }
    }

    // Every exit path saves first: the debounced write above may not have
    // fired yet for the final position.
    fun dismissSaving() {
        if (restored) {
            try {
                readerViewModel?.savePage(currentPage)
            } catch (_: Exception) {
            }
        }
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = { dismissSaving() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = null
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top bar: close + position + mode + bookmark + menu
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = { dismissSaving() },
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
                    val snapshotCount = pageCount
                    if (snapshotCount != null) {
                        // Position indicator follows the scroll in SCROLL mode
                        // and the pager state in SINGLE mode.
                        val shownPage = (currentPage + 1).coerceAtMost(snapshotCount)
                        Text(
                            text = "Page $shownPage of $snapshotCount",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    IconButton(
                        onClick = {
                            if (viewMode == PdfViewMode.SCROLL) {
                                // Keep the reader on the same page when
                                // switching to single-page view.
                                val count = snapshotCount ?: 0
                                singlePageIndex = if (count > 0) {
                                    listState.firstVisibleItemIndex.coerceIn(0, count - 1)
                                } else {
                                    listState.firstVisibleItemIndex.coerceAtLeast(0)
                                }
                                viewMode = PdfViewMode.SINGLE
                            } else {
                                viewMode = PdfViewMode.SCROLL
                                val target = singlePageIndex
                                scope.launch {
                                    try {
                                        listState.scrollToItem(target)
                                    } catch (_: Exception) {
                                    }
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (viewMode == PdfViewMode.SCROLL) {
                                Icons.Default.CropPortrait
                            } else {
                                Icons.Default.ViewAgenda
                            },
                            contentDescription = if (viewMode == PdfViewMode.SCROLL) {
                                "Switch to single-page view"
                            } else {
                                "Switch to continuous scroll"
                            }
                        )
                    }
                    if (readerViewModel != null && snapshotCount != null) {
                        val bookmarked = bookmarks.any { it.pageIndex == currentPage }
                        IconButton(
                            onClick = {
                                if (bookmarked) {
                                    readerViewModel.removeBookmarkAt(currentPage)
                                } else {
                                    showAddBookmark = true
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (bookmarked) {
                                    Icons.Default.Bookmark
                                } else {
                                    Icons.Default.BookmarkBorder
                                },
                                contentDescription = if (bookmarked) {
                                    "Remove bookmark on this page"
                                } else {
                                    "Bookmark this page"
                                },
                                tint = if (bookmarked) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                    Box {
                        IconButton(onClick = { showOverflow = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Reader options"
                            )
                        }
                        DropdownMenu(
                            expanded = showOverflow,
                            onDismissRequest = { showOverflow = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Go to page") },
                                onClick = {
                                    showOverflow = false
                                    if ((snapshotCount ?: 0) > 1) showGoToPage = true
                                },
                                enabled = (snapshotCount ?: 0) > 1
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (bookmarks.isEmpty()) "Bookmarks"
                                        else "Bookmarks (${bookmarks.size})"
                                    )
                                },
                                onClick = {
                                    showOverflow = false
                                    showBookmarks = true
                                },
                                enabled = readerViewModel != null
                            )
                        }
                    }
                }

                // Page canvas: continuous scroll (default) or single page.
                // Zoom is applied to the whole container, never per page:
                // every page scales together so pages can't overlap. Bitmaps
                // re-render at the committed zoom width for crisp text.
                val snapshotHandle = handle
                val snapshotCount = pageCount
                val zoomGestures = Modifier.zoomPanGestures(
                    isZoomed = { scale > 1f },
                    onGesture = { zoom, pan ->
                        scale = (scale * zoom).coerceIn(0.8f, 5f)
                        if (scale > 1f) {
                            offsetX += pan.x
                            offsetY += pan.y
                        } else {
                            offsetX = 0f
                            offsetY = 0f
                        }
                    },
                    onEnd = {
                        val settled = scale.coerceIn(1f, 4f)
                        if (settled <= 1.01f) {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                            committedZoom = 1f
                        } else {
                            committedZoom = settled
                        }
                    }
                )
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    val baseWidthPx = with(density) { maxWidth.toPx() }.toInt()
                        .coerceAtLeast(1)
                    // Crisp zoom width, capped so one ARGB_8888 bitmap stays
                    // ≤ 16 MB. Layout width never changes — only the container
                    // transform scales — so zoom can't overlap pages.
                    val renderWidthPx =
                        (baseWidthPx * committedZoom).toInt().coerceIn(1, 2048)
                    if (!openFinished) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = "Opening PDF…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else if (snapshotHandle == null || snapshotCount == null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                text = "Couldn't open this PDF.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(onClick = { dismissSaving() }) { Text("Close") }
                        }
                    } else if (viewMode == PdfViewMode.SCROLL) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offsetX,
                                    translationY = offsetY,
                                    // Clip to the canvas: zoomed pages must not
                                    // bleed over the top bar / bottom nav.
                                    clip = true
                                )
                                .then(zoomGestures),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(snapshotCount) { pageIndex ->
                                PdfContinuousPage(
                                    filePath = filePath,
                                    pageIndex = pageIndex,
                                    targetWidthPx = renderWidthPx,
                                    handle = snapshotHandle,
                                    cache = pageCache,
                                    lock = rendererLock
                                )
                            }
                        }
                    } else {
                        // Single page: edge tap zones turn pages, bottom bar
                        // has the same buttons for discoverability.
                        Box(modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale,
                                        translationX = offsetX,
                                        translationY = offsetY,
                                        clip = true
                                    )
                                    .then(zoomGestures),
                                contentAlignment = Alignment.Center
                            ) {
                                PdfContinuousPage(
                                    filePath = filePath,
                                    pageIndex = singlePageIndex.coerceIn(
                                        0,
                                        snapshotCount - 1
                                    ),
                                    targetWidthPx = renderWidthPx,
                                    handle = snapshotHandle,
                                    cache = pageCache,
                                    lock = rendererLock,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            // Edge tap zones only at 1x: while zoomed they would
                            // steal edge pans and turn pages by accident.
                            if (scale <= 1.01f) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .fillMaxHeight()
                                        .width(64.dp)
                                        .clickable(
                                            interactionSource = remember {
                                                MutableInteractionSource()
                                            },
                                            indication = null
                                        ) { jumpTo(singlePageIndex - 1) }
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .fillMaxHeight()
                                        .width(64.dp)
                                        .clickable(
                                            interactionSource = remember {
                                                MutableInteractionSource()
                                            },
                                            indication = null
                                        ) { jumpTo(singlePageIndex + 1) }
                                )
                            }
                        }
                    }
                }
                if (viewMode == PdfViewMode.SINGLE && snapshotCount != null &&
                    snapshotCount > 1 && openFinished
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { jumpTo(singlePageIndex - 1) },
                            enabled = singlePageIndex > 0
                        ) { Text("Previous") }
                        TextButton(
                            onClick = { jumpTo(singlePageIndex + 1) },
                            enabled = singlePageIndex < snapshotCount - 1
                        ) { Text("Next") }
                    }
                }
            }
        }
    }

    // Reader overlays: go-to-page, bookmarks, add-bookmark. Siblings of the
    // sheet content so they draw above it.
    val overlayCount = pageCount
    if (showGoToPage && overlayCount != null) {
        PdfGoToPageDialog(
            pageCount = overlayCount,
            initialPage = currentPage + 1,
            onGo = {
                showGoToPage = false
                jumpTo(it - 1)
            },
            onDismiss = { showGoToPage = false }
        )
    }
    if (showBookmarks && readerViewModel != null) {
        PdfBookmarkSheet(
            bookmarks = bookmarks,
            onJump = {
                showBookmarks = false
                jumpTo(it)
            },
            onDelete = { readerViewModel.removeBookmark(it) },
            onDismiss = { showBookmarks = false }
        )
    }
    if (showAddBookmark && readerViewModel != null && overlayCount != null) {
        PdfAddBookmarkDialog(
            pageIndex = currentPage,
            onConfirm = { label ->
                readerViewModel.addBookmark(currentPage, label)
                showAddBookmark = false
            },
            onDismiss = { showAddBookmark = false }
        )
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
