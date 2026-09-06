package com.kotonosora.todolist.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

enum class EditorMode {
    LivePreview,
    RawEdit,
    Split
}

@Composable
fun LivePreviewEditor(
    content: String,
    onContentChange: (String) -> Unit,
    onWikiLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val mode = when (selectedTab) {
        0 -> EditorMode.LivePreview
        1 -> EditorMode.RawEdit
        else -> EditorMode.Split
    }

    Column(modifier = modifier) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Live Preview") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Raw Markdown") }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Split View") }
            )
        }

        Spacer(Modifier.height(8.dp))

        when (mode) {
            EditorMode.RawEdit -> {
                OutlinedTextField(
                    value = content,
                    onValueChange = onContentChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    placeholder = { Text("Write your Markdown note here...") }
                )
            }

            EditorMode.LivePreview -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(8.dp)
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

            EditorMode.Split -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    OutlinedTextField(
                        value = content,
                        onValueChange = onContentChange,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 4.dp),
                        placeholder = { Text("Markdown...") }
                    )
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .padding(12.dp)
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
            text = "(Empty note)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    val lines = text.lines()
    var inCodeBlock = false
    val codeBlockLines = mutableListOf<String>()

    lines.forEachIndexed { index, line ->
        val trimmed = line.trim()

        if (trimmed.startsWith("```")) {
            if (inCodeBlock) {
                CodeBlockView(code = codeBlockLines.joinToString("\n"))
                codeBlockLines.clear()
                inCodeBlock = false
            } else {
                inCodeBlock = true
            }
            return@forEachIndexed
        }

        if (inCodeBlock) {
            codeBlockLines.add(line)
            return@forEachIndexed
        }

        when {
            trimmed == "---" || trimmed == "***" -> {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            }

            trimmed.startsWith("# ") -> {
                Text(
                    text = parseInlineMarkdownWithLinks(trimmed.removePrefix("# ")),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            trimmed.startsWith("## ") -> {
                Text(
                    text = parseInlineMarkdownWithLinks(trimmed.removePrefix("## ")),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            trimmed.startsWith("### ") -> {
                Text(
                    text = parseInlineMarkdownWithLinks(trimmed.removePrefix("### ")),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            trimmed.startsWith("> ") -> {
                QuoteBlockView(text = trimmed.removePrefix("> "), onWikiLinkClick = onWikiLinkClick)
            }

            trimmed.startsWith("- [ ] ") || trimmed.startsWith("* [ ] ") -> {
                TaskItemView(
                    text = trimmed.substring(6),
                    isChecked = false,
                    onToggle = { onTaskToggle(index) },
                    onWikiLinkClick = onWikiLinkClick
                )
            }

            trimmed.startsWith("- [x] ") || trimmed.startsWith("- [X] ") ||
            trimmed.startsWith("* [x] ") || trimmed.startsWith("* [X] ") -> {
                TaskItemView(
                    text = trimmed.substring(6),
                    isChecked = true,
                    onToggle = { onTaskToggle(index) },
                    onWikiLinkClick = onWikiLinkClick
                )
            }

            trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                BulletItemView(text = trimmed.substring(2), onWikiLinkClick = onWikiLinkClick)
            }

            else -> {
                if (trimmed.isNotEmpty()) {
                    val annotated = parseInlineMarkdownWithLinks(line)
                    ClickableText(
                        text = annotated,
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                        onClick = { offset ->
                            annotated.getStringAnnotations("WIKILINK", offset, offset)
                                .firstOrNull()?.let { annotation ->
                                    onWikiLinkClick(annotation.item)
                                }
                        }
                    )
                } else {
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }

    if (inCodeBlock && codeBlockLines.isNotEmpty()) {
        CodeBlockView(code = codeBlockLines.joinToString("\n"))
    }
}

@Composable
private fun BulletItemView(text: String, onWikiLinkClick: (String) -> Unit) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = "• ",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        val annotated = parseInlineMarkdownWithLinks(text)
        ClickableText(
            text = annotated,
            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
            onClick = { offset ->
                annotated.getStringAnnotations("WIKILINK", offset, offset)
                    .firstOrNull()?.let { annotation ->
                        onWikiLinkClick(annotation.item)
                    }
            }
        )
    }
}

@Composable
private fun TaskItemView(
    text: String,
    isChecked: Boolean,
    onToggle: () -> Unit,
    onWikiLinkClick: (String) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable { onToggle() }
    ) {
        Icon(
            imageVector = if (isChecked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
            contentDescription = null,
            tint = if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(6.dp))
        val annotated = parseInlineMarkdownWithLinks(text)
        ClickableText(
            text = annotated,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = if (isChecked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (isChecked) TextDecoration.LineThrough else null
            ),
            onClick = { offset ->
                annotated.getStringAnnotations("WIKILINK", offset, offset)
                    .firstOrNull()?.let { annotation ->
                        onWikiLinkClick(annotation.item)
                    } ?: onToggle()
            }
        )
    }
}

@Composable
private fun QuoteBlockView(text: String, onWikiLinkClick: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                shape = MaterialTheme.shapes.extraSmall
            )
            .padding(10.dp)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(20.dp)
                .background(MaterialTheme.colorScheme.primary)
        )
        Spacer(Modifier.width(8.dp))
        val annotated = parseInlineMarkdownWithLinks(text)
        ClickableText(
            text = annotated,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            onClick = { offset ->
                annotated.getStringAnnotations("WIKILINK", offset, offset)
                    .firstOrNull()?.let { annotation ->
                        onWikiLinkClick(annotation.item)
                    }
            }
        )
    }
}

@Composable
private fun CodeBlockView(code: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small
            )
            .padding(12.dp)
    ) {
        Text(
            text = code,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun parseInlineMarkdownWithLinks(input: String): AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        val length = input.length

        while (cursor < length) {
            when {
                // WikiLink [[Target]] or [[Target|Alias]]
                input.startsWith("[[", cursor) -> {
                    val next = input.indexOf("]]", cursor + 2)
                    if (next != -1) {
                        val inner = input.substring(cursor + 2, next)
                        val target = inner.substringBefore("|").trim()
                        val display = if (inner.contains("|")) inner.substringAfter("|").trim() else target

                        pushStringAnnotation(tag = "WIKILINK", annotation = target)
                        withStyle(
                            SpanStyle(
                                color = Color(0xFF2196F3),
                                fontWeight = FontWeight.Bold,
                                textDecoration = TextDecoration.Underline
                            )
                        ) {
                            append(display)
                        }
                        pop()
                        cursor = next + 2
                    } else {
                        append(input[cursor])
                        cursor++
                    }
                }

                // Bold **
                input.startsWith("**", cursor) -> {
                    val next = input.indexOf("**", cursor + 2)
                    if (next != -1) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(input.substring(cursor + 2, next))
                        }
                        cursor = next + 2
                    } else {
                        append(input[cursor])
                        cursor++
                    }
                }

                // Strikethrough ~~
                input.startsWith("~~", cursor) -> {
                    val next = input.indexOf("~~", cursor + 2)
                    if (next != -1) {
                        withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                            append(input.substring(cursor + 2, next))
                        }
                        cursor = next + 2
                    } else {
                        append(input[cursor])
                        cursor++
                    }
                }

                // Italic *
                input.startsWith("*", cursor) -> {
                    val next = input.indexOf("*", cursor + 1)
                    if (next != -1) {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(input.substring(cursor + 1, next))
                        }
                        cursor = next + 1
                    } else {
                        append(input[cursor])
                        cursor++
                    }
                }

                // Inline Code `
                input.startsWith("`", cursor) -> {
                    val next = input.indexOf("`", cursor + 1)
                    if (next != -1) {
                        withStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium
                            )
                        ) {
                            append(input.substring(cursor + 1, next))
                        }
                        cursor = next + 1
                    } else {
                        append(input[cursor])
                        cursor++
                    }
                }

                else -> {
                    append(input[cursor])
                    cursor++
                }
            }
        }
    }
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

@Preview(showBackground = true, name = "Live Preview Mode")
@Composable
fun LivePreviewEditorPreview_LivePreview() {
    MaterialTheme {
        LivePreviewEditor(
            content = "# Project Roadmap\n\n- [ ] Draft Architecture\n- [x] SAF Vault Manager\n\nCheck [[Meeting Notes]] and #ideas.",
            onContentChange = {},
            onWikiLinkClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Split Mode")
@Composable
fun LivePreviewEditorPreview_Split() {
    MaterialTheme {
        LivePreviewEditor(
            content = "# Knowledge Base\n\nConnected notes with [[WikiLinks]].",
            onContentChange = {},
            onWikiLinkClick = {}
        )
    }
}
