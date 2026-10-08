package com.kotonosora.todolist.ui.components.pdf

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kotonosora.todolist.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

/**
 * One page of the reader (a scroll row or the single-page view): renders on
 * demand off Main and caches the bitmap, so scrolling back is instant while
 * distant pages can be evicted. Shows a page-shaped placeholder while
 * rendering and an inline error when just this page fails — one bad page
 * never hides the document. Zoom lives on the container, never here, so
 * pages can't overlap.
 */
@Composable
internal fun PdfContinuousPage(
    filePath: String,
    pageIndex: Int,
    targetWidthPx: Int,
    handle: PdfHandle?,
    cache: PdfPageCache,
    lock: Mutex,
    modifier: Modifier = Modifier
) {
    var bitmap by remember(filePath, pageIndex, targetWidthPx) {
        mutableStateOf<Bitmap?>(null)
    }
    var failed by remember(filePath, pageIndex, targetWidthPx) {
        mutableStateOf(false)
    }
    // handle is a key: without it this effect runs while the renderer is
    // still opening, reads null once, and never retries.
    LaunchedEffect(filePath, handle, pageIndex, targetWidthPx) {
        if (handle == null) return@LaunchedEffect
        failed = false
        val rendered = withContext(Dispatchers.IO) {
            handle.page(
                index = pageIndex,
                targetWidthPx = targetWidthPx,
                cache = cache,
                lock = lock
            )
        }
        if (rendered != null) bitmap = rendered else failed = true
    }

    val rendered = bitmap
    when {
        rendered != null -> Image(
            bitmap = rendered.asImageBitmap(),
            contentDescription = "PDF page ${pageIndex + 1}",
            modifier = modifier.fillMaxWidth()
        )

        failed -> Text(
            text = "Couldn't render page ${pageIndex + 1}.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(24.dp)
        )

        else -> Box(
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(1f / 1.414f),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

// ── PREVIEWS: static replicas of the loading/error states only — the real
// composable needs a PdfRenderer handle + page cache, so it cannot render in
// the IDE preview. Production code above is untouched. ──

@Preview(
    showBackground = true,
    name = "1. PdfContinuousPage Loading - Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun PdfContinuousPageLoadingPreview_Dark() {
    AppTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f / 1.414f),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@Preview(
    showBackground = true,
    name = "2. PdfContinuousPage Loading - Light",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
fun PdfContinuousPageLoadingPreview_Light() {
    AppTheme(darkTheme = false) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f / 1.414f),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@Preview(
    showBackground = true,
    name = "3. PdfContinuousPage Error - Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun PdfContinuousPageErrorPreview_Dark() {
    AppTheme(darkTheme = true) {
        Text(
            text = "Couldn't render page 3.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(24.dp)
        )
    }
}

@Preview(
    showBackground = true,
    name = "4. PdfContinuousPage Error - Light",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
fun PdfContinuousPageErrorPreview_Light() {
    AppTheme(darkTheme = false) {
        Text(
            text = "Couldn't render page 3.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(24.dp)
        )
    }
}
