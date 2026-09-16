package com.kotonosora.todolist.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kotonosora.todolist.data.native.MdNativeHelper
import com.kotonosora.todolist.domain.model.ActionStamp
import com.kotonosora.todolist.domain.model.EmotionStamp
import com.kotonosora.todolist.domain.model.NoteType

@Composable
fun EditorMetadataBar(
    relativePath: String,
    content: String,
    modifier: Modifier = Modifier,
    noteType: NoteType = NoteType.PERMANENT,
    emotion: EmotionStamp? = null,
    actions: List<ActionStamp> = emptyList()
) {
    val stats = remember(content) {
        try {
            MdNativeHelper.calculateTextStatsNative(content)
        } catch (_: Throwable) {
            val wc = if (content.isBlank()) 0 else content.trim().split("\\s+".toRegex()).size
            val cc = content.length
            val rt = (wc / 200).coerceAtLeast(1)
            intArrayOf(wc, cc, 0, rt)
        }
    }
    val wordCount = stats[0]
    val charCount = stats[1]
    val readingTimeMinutes = stats[3]

    val noteColor = when (noteType) {
        NoteType.FLEETING -> Color(0xFFFFC107)    // Yellow
        NoteType.LITERATURE -> Color(0xFF2196F3)  // Blue
        NoteType.PERMANENT -> Color(0xFF4CAF50)   // Green
        NoteType.MOC -> Color(0xFF9C27B0)         // Purple
        NoteType.DIARY -> Color(0xFFE91E63)       // Pink
        NoteType.DAILY -> Color(0xFF00BCD4)       // Cyan
        NoteType.REPORT -> Color(0xFFFF5722)      // Deep Orange
        NoteType.TODO -> Color(0xFF8BC34A)        // Light Green
        NoteType.FLASHCARD -> Color(0xFF673AB7)   // Deep Purple
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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

            if (emotion != null || actions.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    emotion?.let { emo ->
                        Surface(
                            color = emo.color,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = emo.icon,
                                    contentDescription = emo.label,
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = emo.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    actions.forEach { act ->
                        Surface(
                            color = act.color,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = act.icon,
                                    contentDescription = act.label,
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = act.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Editor Metadata Bar with Stamps Preview")
@Composable
fun EditorMetadataBarPreview() {
    MaterialTheme {
        EditorMetadataBar(
            relativePath = "Projects/Roadmap.md",
            content = "Sample content for Zettelkasten note with several words and characters.",
            noteType = NoteType.DIARY,
            emotion = EmotionStamp.HAPPY,
            actions = listOf(ActionStamp.WORK, ActionStamp.EXERCISE)
        )
    }
}
