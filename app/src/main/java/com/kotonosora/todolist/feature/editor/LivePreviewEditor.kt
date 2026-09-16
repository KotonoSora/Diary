package com.kotonosora.todolist.feature.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.StrikethroughS
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kotonosora.todolist.ui.components.MermaidDiagramView
import com.kotonosora.todolist.ui.theme.TodoListTheme
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography

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
    modifier: Modifier = Modifier
) {
    var viewMode by remember { mutableStateOf(EditorViewMode.EDITING) }

    Box(modifier = modifier.fillMaxSize()) {
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
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(end = 4.dp),
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
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            MarkdownContentRenderer(
                                text = content,
                                onWikiLinkClick = onWikiLinkClick,
                                onTaskToggle = { lineIndex ->
                                    val updatedContent = toggleTaskAtLine(content, lineIndex)
                                    onContentChange(updatedContent)
                                }
                            )
                        }
                    }
                }

                // Obsidian-style Floating Mode Toggle Chip (Top Right)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(20.dp),
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp)
                        .clickable {
                            viewMode =
                                if (viewMode == EditorViewMode.EDITING) EditorViewMode.READING else EditorViewMode.EDITING
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (viewMode == EditorViewMode.EDITING) Icons.AutoMirrored.Filled.MenuBook else Icons.Default.Edit,
                            contentDescription = if (viewMode == EditorViewMode.EDITING) "Switch to Reading View" else "Switch to Live Edit",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (viewMode == EditorViewMode.EDITING) "Reading" else "Editing",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
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

@Composable
private fun MarkdownContentRenderer(
    text: String,
    onWikiLinkClick: (String) -> Unit,
    onTaskToggle: (Int) -> Unit
) {
    if (text.isBlank()) {
        Text(
            text = "(Empty note - tap top-right 'Editing' to start writing)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    // Extract Mermaid code blocks (```mermaid ... ```) for diagram rendering
    val mermaidBlocks = remember(text) {
        val regex = Regex("""```mermaid\s*\n([\s\S]*?)\n```""", RegexOption.IGNORE_CASE)
        regex.findAll(text).map { it.groupValues[1].trim() }.toList()
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

    // Convert WikiLinks [[Target]] into standard Markdown links [Target](wikilink://Target) for AST rendering
    val processedText = remember(text) {
        text.replace(Regex("""\[\[([^|\]]+)(?:\|([^\]]+))?\]\]""")) { matchResult ->
            val target = matchResult.groupValues[1].trim()
            val alias = matchResult.groupValues.getOrNull(2)?.trim()?.ifBlank { null } ?: target
            "[$alias](wikilink://$target)"
        }
    }

    Markdown(
        content = processedText,
        colors = markdownColor(
            text = MaterialTheme.colorScheme.onSurface
        ),
        typography = markdownTypography(
            h1 = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            h2 = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            h3 = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            paragraph = MaterialTheme.typography.bodyMedium
        )
    )
}

private fun toggleTaskAtLine(text: String, lineIndex: Int): String {
    val lines = text.lines().toMutableList()
    if (lineIndex in lines.indices) {
        val line = lines[lineIndex]
        lines[lineIndex] = when {
            line.contains("- [ ] ") -> line.replace("- [ ] ", "- [x] ")
            line.contains("* [ ] ") -> line.replace("* [ ] ", "* [x] ")
            line.contains("- [x] ") -> line.replace("- [x] ", "- [ ] ")
            line.contains("- [X] ") -> line.replace("- [X] ", "- [ ] ")
            line.contains("* [x] ") -> line.replace("* [x] ", "* [ ] ")
            line.contains("* [X] ") -> line.replace("* [X] ", "* [ ] ")
            else -> line
        }
    }
    return lines.joinToString("\n")
}

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
    TodoListTheme(darkTheme = true) {
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
    TodoListTheme(darkTheme = false) {
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
