package com.kotonosora.todolist.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kotonosora.todolist.domain.model.NoteType

@Composable
fun EditorMetadataBar(
    relativePath: String,
    content: String,
    modifier: Modifier = Modifier,
    noteType: NoteType = NoteType.PERMANENT
) {
    val wordCount = if (content.isBlank()) 0 else content.trim().split("\\s+".toRegex()).size
    val charCount = content.length
    val readingTimeMinutes = (wordCount / 200).coerceAtLeast(1)

    val noteColor = when (noteType) {
        NoteType.FLEETING -> Color(0xFFFFC107)    // Yellow
        NoteType.LITERATURE -> Color(0xFF2196F3)  // Blue
        NoteType.PERMANENT -> Color(0xFF4CAF50)   // Green
        NoteType.MOC -> Color(0xFF9C27B0)         // Purple
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(noteColor)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (relativePath.isBlank()) "Vault Root" else relativePath,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "$wordCount words · $charCount chars · $readingTimeMinutes min read",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true, name = "Editor Metadata Bar Preview")
@Composable
fun EditorMetadataBarPreview() {
    MaterialTheme {
        EditorMetadataBar(
            relativePath = "Projects/Roadmap.md",
            content = "Sample content for Zettelkasten note with several words and characters.",
            noteType = NoteType.PERMANENT
        )
    }
}
