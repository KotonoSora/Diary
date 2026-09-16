package com.kotonosora.todolist.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.StrikethroughS
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Title / H1
            FormatIconButton(
                icon = Icons.Default.Title,
                contentDescription = "Header 1",
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
            FormatIconButton(
                icon = Icons.Default.FormatSize,
                contentDescription = "Header 2",
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
            FormatIconButton(
                icon = Icons.Default.FormatBold,
                contentDescription = "Bold",
                selected = state.currentSpanStyle.fontWeight == FontWeight.Bold,
                onClick = {
                    state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
                }
            )

            // Italic
            FormatIconButton(
                icon = Icons.Default.FormatItalic,
                contentDescription = "Italic",
                selected = state.currentSpanStyle.fontStyle == FontStyle.Italic,
                onClick = {
                    state.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic))
                }
            )

            // Strikethrough
            FormatIconButton(
                icon = Icons.Default.StrikethroughS,
                contentDescription = "Strikethrough",
                selected = state.currentSpanStyle.textDecoration == TextDecoration.LineThrough,
                onClick = {
                    state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                }
            )

            // Bullet List
            FormatIconButton(
                icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                contentDescription = "Bullet List",
                selected = state.isUnorderedList,
                onClick = { state.toggleUnorderedList() }
            )

            // Ordered List
            FormatIconButton(
                icon = Icons.Default.FormatListNumbered,
                contentDescription = "Ordered List",
                selected = state.isOrderedList,
                onClick = { state.toggleOrderedList() }
            )

            // Code
            FormatIconButton(
                icon = Icons.Default.Code,
                contentDescription = "Code",
                selected = state.currentSpanStyle.fontFamily == FontFamily.Monospace,
                onClick = {
                    state.toggleSpanStyle(SpanStyle(fontFamily = FontFamily.Monospace))
                }
            )
        }
    }
}

@Composable
private fun FormatIconButton(
    icon: ImageVector,
    contentDescription: String,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(36.dp),
        colors = if (selected) {
            IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        } else {
            IconButtonDefaults.iconButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(20.dp)
        )
    }
}
