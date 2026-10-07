package com.kotonosora.todolist.feature.editor

import android.os.Environment
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.StrikethroughS
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kotonosora.todolist.domain.model.TaskListEntry
import com.kotonosora.todolist.domain.model.TaskToggleHelper
import com.kotonosora.todolist.ui.components.MarkdownLinks
import com.kotonosora.todolist.ui.components.MarkdownView
import com.kotonosora.todolist.ui.components.MermaidDiagramView
import com.kotonosora.todolist.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class EditorViewMode {
    EDITING,   // Live Source / Markdown Editing
    READING    // Rendered Reading View (Obsidian Reading Mode)
}

/**
 * Obsidian-style Single Unified Live Document Editor
 * Enhanced with MikePenz Multiplatform Markdown Renderer M3 & Mermaid Flowchart / UML Diagrams
 */
@Composable
fun LivePreviewEditor(
    content: String,
    onContentChange: (String) -> Unit,
    onWikiLinkClick: (String) -> Unit,
    viewMode: EditorViewMode = EditorViewMode.EDITING,
    modifier: Modifier = Modifier,
    noteDir: String = ""
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (viewMode) {
                    EditorViewMode.EDITING -> {
                        // Live Editor Field
                        OutlinedTextField(
                            value = content,
                            onValueChange = onContentChange,
                            modifier = Modifier.fillMaxSize(),
                            placeholder = {
                                Text(
                                    text = "Start typing your note in Markdown...",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            ),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontFamily = FontFamily.Default
                            )
                        )
                    }

                    EditorViewMode.READING -> {
                        // Rendered Reading View (Obsidian Style with MikePenz AST Engine + Mermaid Diagram Support)
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 4.dp, vertical = 4.dp)
                        ) {
                            MarkdownContentRenderer(
                                text = content,
                                onWikiLinkClick = onWikiLinkClick,
                                onTaskToggle = { lineIndex ->
                                    val updatedContent = toggleTaskAtLine(content, lineIndex)
                                    onContentChange(updatedContent)
                                },
                                noteDir = noteDir
                            )
                        }
                    }
                }
            }

            // Inline Formatting Shortcut Bar (Visible during Editing mode)
            AnimatedVisibility(
                visible = viewMode == EditorViewMode.EDITING,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                MarkdownFormattingToolbar(
                    onInsertSymbol = { symbolPrefix, symbolSuffix ->
                        val updated = insertMarkdownSymbol(content, symbolPrefix, symbolSuffix)
                        onContentChange(updated)
                    }
                )
            }
        }
    }
}

@Composable
private fun MarkdownFormattingToolbar(
    onInsertSymbol: (String, String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = { onInsertSymbol("# ", "") },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Title,
                    contentDescription = "Header",
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = { onInsertSymbol("**", "**") },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.FormatBold,
                    contentDescription = "Bold",
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = { onInsertSymbol("*", "*") },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.FormatItalic,
                    contentDescription = "Italic",
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = { onInsertSymbol("~~", "~~") },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.StrikethroughS,
                    contentDescription = "Strikethrough",
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = { onInsertSymbol("[[", "]]") },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Link,
                    contentDescription = "WikiLink",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = { onInsertSymbol("- [ ] ", "") },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.CheckBoxOutlineBlank,
                    contentDescription = "Task Checkbox",
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = { onInsertSymbol("- ", "") },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.FormatListBulleted,
                    contentDescription = "Bullet List",
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = { onInsertSymbol("> ", "") },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.FormatQuote,
                    contentDescription = "Quote Block",
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = { onInsertSymbol("`", "`") },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Code,
                    contentDescription = "Inline Code",
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = {
                    onInsertSymbol(
                        "```mermaid\ngraph TD\n    A[Start] --> B[End]\n```",
                        ""
                    )
                },
                modifier = Modifier.size(36.dp)
            ) {
                Text(
                    text = "UML",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private data class FrontmatterParsedResult(
    val metadata: Map<String, String>,
    val body: String,
    /** 0-based line index in the original [text] where [body] starts. */
    val bodyStartLineIndex: Int = 0
)

private fun parseFrontmatterAndBody(text: String): FrontmatterParsedResult {
    val trimmed = text.trimStart()
    if (!trimmed.startsWith("---")) {
        return FrontmatterParsedResult(emptyMap(), text, 0)
    }
    val closingIndex = trimmed.indexOf("---", startIndex = 3)
    if (closingIndex == -1) {
        return FrontmatterParsedResult(emptyMap(), text, 0)
    }
    val yamlSection = trimmed.substring(3, closingIndex).trim()
    val afterClosing = trimmed.substring(closingIndex + 3)
    val bodyText = afterClosing.trimStart()

    // Offset = newlines before body start in the original text, so task
    // line indices stay correct even with leading blanks or \r\n endings.
    // Compute from char offsets (mirrors the substring/trim logic above)
    // instead of `text.lines().size - body.lines().size`, which drifts when
    // leading/trailing blank lines are trimmed.
    val trimmedStartChars = text.length - trimmed.length
    val leadingWsAfterClosing = afterClosing.length - bodyText.length
    val bodyStartChar = trimmedStartChars + closingIndex + 3 + leadingWsAfterClosing
    val bodyStartLineIndex =
        text.substring(0, bodyStartChar.coerceIn(0, text.length)).count { it == '\n' }

    val metaMap = mutableMapOf<String, String>()
    yamlSection.lines().forEach { line ->
        val parts = line.split(":", limit = 2)
        if (parts.size == 2) {
            val key = parts[0].trim()
            val value = parts[1].trim().removeSurrounding("\"", "\"").removeSurrounding("'", "'")
            if (key.isNotEmpty() && value.isNotEmpty()) {
                metaMap[key] = value
            }
        }
    }
    return FrontmatterParsedResult(metaMap, bodyText, bodyStartLineIndex)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FrontmatterMetadataHeader(
    metadata: Map<String, String>,
    modifier: Modifier = Modifier
) {
    if (metadata.isEmpty()) return

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Tag,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Note Metadata",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                metadata.forEach { (key, value) ->
                    val chipColor = when (key.lowercase()) {
                        "type" -> MaterialTheme.colorScheme.primaryContainer
                        "date", "created" -> MaterialTheme.colorScheme.secondaryContainer
                        "uid" -> MaterialTheme.colorScheme.tertiaryContainer
                        else -> MaterialTheme.colorScheme.surface
                    }
                    val textColor = when (key.lowercase()) {
                        "type" -> MaterialTheme.colorScheme.onPrimaryContainer
                        "date", "created" -> MaterialTheme.colorScheme.onSecondaryContainer
                        "uid" -> MaterialTheme.colorScheme.onTertiaryContainer
                        else -> MaterialTheme.colorScheme.onSurface
                    }

                    Surface(
                        color = chipColor,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${key.uppercase()}: ",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = textColor.copy(alpha = 0.7f)
                            )
                            Text(
                                text = value,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = textColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MarkdownContentRenderer(
    text: String,
    onWikiLinkClick: (String) -> Unit,
    onTaskToggle: (Int) -> Unit,
    noteDir: String = ""
) {
    if (text.isBlank()) {
        Text(
            text = "(Empty note - tap top-right 'Editing' to start writing)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    val context = LocalContext.current
    val parsed = remember(text) { parseFrontmatterAndBody(text) }

    // Render Frontmatter Metadata Card if tags / metadata exist
    if (parsed.metadata.isNotEmpty()) {
        FrontmatterMetadataHeader(metadata = parsed.metadata)
    }

    val bodyText = parsed.body

    // Interactive task checklist: mikepenz renders `- [ ]` statically, so
    // surface real checkboxes here. Tapping flips the source line via
    // onTaskToggle (same helper the editor uses).
    val taskEntries = remember(bodyText) { TaskToggleHelper.parseTasks(bodyText) }
    // Frontmatter strip shifts line numbers — use the parser's exact body
    // start line so toggles hit the full-document index.
    val bodyOffset = remember(parsed) { parsed.bodyStartLineIndex }
    if (taskEntries.isNotEmpty()) {
        TaskChecklistCard(
            entries = taskEntries,
            onToggle = { entry -> onTaskToggle(entry.lineIndex + bodyOffset) },
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }

    // Mermaid fences render as interactive diagrams; everything else (prose,
    // tables, lists, highlighted code) goes through the shared Markdown view.
    val (proseText, mermaidBlocks) = remember(bodyText) {
        MarkdownLinks.extractMermaidBlocks(bodyText)
    }
    if (mermaidBlocks.isNotEmpty()) {
        mermaidBlocks.forEach { mermaidCode ->
            MermaidDiagramView(
                mermaidCode = mermaidCode,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
    }

    val processedText = remember(proseText, noteDir) {
        MarkdownLinks.rewriteWikiLinks(proseText)
    }

    // Image resolution hits the filesystem — keep it off Main. Reset on note
    // change so the previous note's resolved text never flashes.
    var resolvedText by remember(processedText, noteDir) { mutableStateOf<String?>(null) }
    LaunchedEffect(processedText, noteDir) {
        resolvedText = try {
            withContext(Dispatchers.IO) {
                val vaultRoot = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                    ?: context.filesDir
                MarkdownLinks.resolveImageDestinations(processedText, noteDir, vaultRoot)
            }
        } catch (e: Exception) {
            android.util.Log.w("LivePreviewEditor", "image resolve failed", e)
            processedText
        }
    }

    MarkdownView(
        content = resolvedText ?: processedText,
        onWikiLinkClick = onWikiLinkClick
    )
}

@Composable
private fun TaskChecklistCard(
    entries: List<TaskListEntry>,
    onToggle: (TaskListEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Tasks (${entries.count { it.checked }}/${entries.size})",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            entries.forEach { entry ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggle(entry) }
                        .padding(vertical = 2.dp)
                ) {
                    androidx.compose.material3.Checkbox(
                        checked = entry.checked,
                        onCheckedChange = { onToggle(entry) }
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = entry.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (entry.checked) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

private fun toggleTaskAtLine(text: String, lineIndex: Int): String =
    TaskToggleHelper.toggleTaskAtLine(text, lineIndex)

private fun insertMarkdownSymbol(currentContent: String, prefix: String, suffix: String): String {
    return if (currentContent.isBlank()) {
        "$prefix$suffix"
    } else {
        "$currentContent\n$prefix$suffix"
    }
}

// ── FULL CASE-BY-CASE PREVIEWS ──

@Preview(showBackground = true, name = "Obsidian Single View Live Editor with Mermaid (Dark)")
@Composable
fun LivePreviewEditorPreview_Dark() {
    AppTheme(darkTheme = true) {
        LivePreviewEditor(
            content = """
                # Architecture & Flowcharts
                
                ```mermaid
                graph TD
                    A[Client] --> B[ViewModel]
                    B --> C[VaultRepository]
                ```
                
                Check [[Meeting Notes]] and #ideas.
            """.trimIndent(),
            onContentChange = {},
            onWikiLinkClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Obsidian Single View Live Editor with Mermaid (Light)")
@Composable
fun LivePreviewEditorPreview_Light() {
    AppTheme(darkTheme = false) {
        LivePreviewEditor(
            content = """
                # Architecture & Flowcharts
                
                ```mermaid
                graph TD
                    A[Client] --> B[ViewModel]
                    B --> C[VaultRepository]
                ```
                
                Check [[Meeting Notes]] and #ideas.
            """.trimIndent(),
            onContentChange = {},
            onWikiLinkClick = {}
        )
    }
}
