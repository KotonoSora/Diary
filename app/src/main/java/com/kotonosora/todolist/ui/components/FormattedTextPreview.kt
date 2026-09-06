package com.kotonosora.todolist.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

@Composable
fun FormattedTextPreview(
    text: String,
    format: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (text.isBlank()) {
                Text(
                    text = "(Nothing to preview)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (format.equals("txt", ignoreCase = true)) {
                PlainTextRenderer(text = text)
            } else {
                MarkdownRenderer(text = text)
            }
        }
    }
}

@Composable
private fun PlainTextRenderer(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        fontFamily = FontFamily.Monospace,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun MarkdownRenderer(text: String) {
    val lines = text.lines()
    var inCodeBlock = false
    val codeBlockLines = mutableListOf<String>()

    for (line in lines) {
        val trimmed = line.trim()

        if (trimmed.startsWith("```")) {
            if (inCodeBlock) {
                // End code block
                CodeBlockView(code = codeBlockLines.joinToString("\n"))
                codeBlockLines.clear()
                inCodeBlock = false
            } else {
                inCodeBlock = true
            }
            continue
        }

        if (inCodeBlock) {
            codeBlockLines.add(line)
            continue
        }

        when {
            trimmed == "---" || trimmed == "***" -> {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            }

            trimmed.startsWith("# ") -> {
                Text(
                    text = parseInlineMarkdown(trimmed.removePrefix("# ")),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            trimmed.startsWith("## ") -> {
                Text(
                    text = parseInlineMarkdown(trimmed.removePrefix("## ")),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            trimmed.startsWith("### ") -> {
                Text(
                    text = parseInlineMarkdown(trimmed.removePrefix("### ")),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            trimmed.startsWith("> ") -> {
                QuoteBlockView(text = trimmed.removePrefix("> "))
            }

            trimmed.startsWith("- [ ] ") || trimmed.startsWith("* [ ] ") -> {
                TaskItemView(text = trimmed.substring(6), isChecked = false)
            }

            trimmed.startsWith("- [x] ") || trimmed.startsWith("- [X] ") ||
            trimmed.startsWith("* [x] ") || trimmed.startsWith("* [X] ") -> {
                TaskItemView(text = trimmed.substring(6), isChecked = true)
            }

            trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                BulletItemView(text = trimmed.substring(2))
            }

            else -> {
                if (trimmed.isNotEmpty()) {
                    Text(
                        text = parseInlineMarkdown(line),
                        style = MaterialTheme.typography.bodyMedium
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
private fun BulletItemView(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = "• ",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = parseInlineMarkdown(text),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun TaskItemView(text: String, isChecked: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (isChecked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
            contentDescription = null,
            tint = if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = parseInlineMarkdown(text),
            style = MaterialTheme.typography.bodyMedium,
            textDecoration = if (isChecked) TextDecoration.LineThrough else null,
            color = if (isChecked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun QuoteBlockView(text: String) {
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
        Text(
            text = parseInlineMarkdown(text),
            style = MaterialTheme.typography.bodyMedium,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun CodeBlockView(code: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.small
            )
            .padding(12.dp)
    ) {
        Text(
            text = code,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Parses bold (**), italic (*), strikethrough (~~), and inline code (`) into AnnotatedString.
 */
private fun parseInlineMarkdown(input: String): AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        val length = input.length

        while (cursor < length) {
            when {
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
