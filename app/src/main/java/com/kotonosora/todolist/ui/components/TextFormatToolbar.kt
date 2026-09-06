package com.kotonosora.todolist.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.model.RichTextState

@Composable
fun TextFormatToolbar(
    state: RichTextState,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Header 1
            FormatChip(
                label = "H1",
                selected = state.currentSpanStyle.fontSize == 24.sp,
                onClick = {
                    state.toggleSpanStyle(
                        SpanStyle(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            )

            // Header 2
            FormatChip(
                label = "H2",
                selected = state.currentSpanStyle.fontSize == 20.sp,
                onClick = {
                    state.toggleSpanStyle(
                        SpanStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            )

            // Bold
            FormatChip(
                label = "B",
                selected = state.currentSpanStyle.fontWeight == FontWeight.Bold,
                fontWeight = FontWeight.Bold,
                onClick = {
                    state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
                }
            )

            // Italic
            FormatChip(
                label = "I",
                selected = state.currentSpanStyle.fontStyle == FontStyle.Italic,
                fontWeight = FontWeight.Normal,
                onClick = {
                    state.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic))
                }
            )

            // Strikethrough
            FormatChip(
                label = "~~S~~",
                selected = state.currentSpanStyle.textDecoration == TextDecoration.LineThrough,
                onClick = {
                    state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                }
            )

            // Bullet List
            FormatChip(
                label = "• List",
                selected = state.isUnorderedList,
                onClick = { state.toggleUnorderedList() }
            )

            // Ordered List
            FormatChip(
                label = "1. List",
                selected = state.isOrderedList,
                onClick = { state.toggleOrderedList() }
            )

            // Code
            FormatChip(
                label = "</>",
                selected = state.currentSpanStyle.fontFamily == FontFamily.Monospace,
                fontFamily = FontFamily.Monospace,
                onClick = {
                    state.toggleSpanStyle(SpanStyle(fontFamily = FontFamily.Monospace))
                }
            )
        }
    }
}

@Composable
private fun FormatChip(
    label: String,
    selected: Boolean = false,
    onClick: () -> Unit,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                fontWeight = fontWeight,
                fontFamily = fontFamily,
                style = MaterialTheme.typography.labelMedium
            )
        },
        modifier = Modifier.padding(end = 4.dp)
    )
}
