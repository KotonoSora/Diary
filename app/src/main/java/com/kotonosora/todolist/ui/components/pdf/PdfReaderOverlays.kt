package com.kotonosora.todolist.ui.components.pdf

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kotonosora.todolist.domain.model.PdfBookmark
import com.kotonosora.todolist.ui.theme.AppTheme

/**
 * Jump-to-page dialog: numeric field plus a slider for long documents.
 * [onGo] receives the 1-based page number.
 */
@Composable
internal fun PdfGoToPageDialog(
    pageCount: Int,
    initialPage: Int,
    onGo: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember(pageCount, initialPage) {
        mutableStateOf(initialPage.coerceIn(1, pageCount).toString())
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Go to page") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.filter { ch -> ch.isDigit() }.take(6) },
                    label = { Text("Page (1–$pageCount)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                val sliderPage = text.toIntOrNull()?.coerceIn(1, pageCount)
                    ?: initialPage.coerceIn(1, pageCount)
                Slider(
                    value = sliderPage.toFloat(),
                    onValueChange = { text = it.toInt().toString() },
                    valueRange = 1f..pageCount.toFloat(),
                    steps = (pageCount - 1).coerceIn(0, 200)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val target = text.toIntOrNull()?.coerceIn(1, pageCount)
                    if (target != null) onGo(target)
                },
                enabled = text.toIntOrNull()?.let { it in 1..pageCount } == true
            ) { Text("Go") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * Bookmark list sheet: tap jumps, trash deletes. Jump also closes the sheet
 * so the document is visible immediately.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PdfBookmarkSheet(
    bookmarks: List<PdfBookmark>,
    onJump: (Int) -> Unit,
    onDelete: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = if (bookmarks.isEmpty()) "Bookmarks"
                else "Bookmarks (${bookmarks.size})",
                style = MaterialTheme.typography.titleMedium
            )
            if (bookmarks.isEmpty()) {
                Text(
                    text = "No bookmarks yet — tap the bookmark icon on any page to add one.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(bookmarks, key = { it.id }) { bookmark ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onJump(bookmark.pageIndex) }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Bookmark,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = bookmark.label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Page ${bookmark.pageIndex + 1}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onDelete(bookmark.id) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete bookmark",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

/**
 * New-bookmark dialog with an editable label prefilled as "Page N".
 */
@Composable
internal fun PdfAddBookmarkDialog(
    pageIndex: Int,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var label by remember(pageIndex) { mutableStateOf("Page ${pageIndex + 1}") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bookmark page ${pageIndex + 1}") },
        text = {
            OutlinedTextField(
                value = label,
                onValueChange = { label = it.take(80) },
                label = { Text("Label") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(label) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ── PREVIEWS ──

private fun previewPdfBookmarks() = listOf(
    PdfBookmark(id = 1, filePath = "preview_sample.pdf", pageIndex = 2, label = "Chapter 1"),
    PdfBookmark(id = 2, filePath = "preview_sample.pdf", pageIndex = 7, label = "Page 8")
)

@Preview(
    showBackground = true,
    name = "1. PdfBookmarkSheet Empty - Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun PdfBookmarkSheetEmptyPreview_Dark() {
    AppTheme(darkTheme = true) {
        PdfBookmarkSheet(
            bookmarks = emptyList(),
            onJump = {},
            onDelete = {},
            onDismiss = {}
        )
    }
}

@Preview(
    showBackground = true,
    name = "2. PdfBookmarkSheet Empty - Light",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
fun PdfBookmarkSheetEmptyPreview_Light() {
    AppTheme(darkTheme = false) {
        PdfBookmarkSheet(
            bookmarks = emptyList(),
            onJump = {},
            onDelete = {},
            onDismiss = {}
        )
    }
}

@Preview(
    showBackground = true,
    name = "3. PdfBookmarkSheet Populated - Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun PdfBookmarkSheetPopulatedPreview_Dark() {
    AppTheme(darkTheme = true) {
        PdfBookmarkSheet(
            bookmarks = previewPdfBookmarks(),
            onJump = {},
            onDelete = {},
            onDismiss = {}
        )
    }
}

@Preview(
    showBackground = true,
    name = "4. PdfBookmarkSheet Populated - Light",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
fun PdfBookmarkSheetPopulatedPreview_Light() {
    AppTheme(darkTheme = false) {
        PdfBookmarkSheet(
            bookmarks = previewPdfBookmarks(),
            onJump = {},
            onDelete = {},
            onDismiss = {}
        )
    }
}

@Preview(
    showBackground = true,
    name = "5. PdfGoToPage - Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun PdfGoToPagePreview_Dark() {
    AppTheme(darkTheme = true) {
        PdfGoToPageDialog(
            pageCount = 12,
            initialPage = 1,
            onGo = {},
            onDismiss = {}
        )
    }
}

@Preview(
    showBackground = true,
    name = "6. PdfGoToPage - Light",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
fun PdfGoToPagePreview_Light() {
    AppTheme(darkTheme = false) {
        PdfGoToPageDialog(
            pageCount = 12,
            initialPage = 1,
            onGo = {},
            onDismiss = {}
        )
    }
}

@Preview(
    showBackground = true,
    name = "7. PdfAddBookmark - Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun PdfAddBookmarkPreview_Dark() {
    AppTheme(darkTheme = true) {
        PdfAddBookmarkDialog(
            pageIndex = 3,
            onConfirm = {},
            onDismiss = {}
        )
    }
}

@Preview(
    showBackground = true,
    name = "8. PdfAddBookmark - Light",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
fun PdfAddBookmarkPreview_Light() {
    AppTheme(darkTheme = false) {
        PdfAddBookmarkDialog(
            pageIndex = 3,
            onConfirm = {},
            onDismiss = {}
        )
    }
}
