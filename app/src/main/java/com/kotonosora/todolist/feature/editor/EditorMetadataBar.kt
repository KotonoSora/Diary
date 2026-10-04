package com.kotonosora.todolist.feature.editor

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kotonosora.todolist.data.native.MdNativeHelper
import com.kotonosora.todolist.domain.model.ActionStamp
import com.kotonosora.todolist.domain.model.EmotionStamp
import com.kotonosora.todolist.domain.model.NoteType
import com.kotonosora.todolist.ui.theme.AppTheme
import com.kotonosora.todolist.ui.theme.NoteTypeColors

@Composable
fun EditorMetadataBar(
    relativePath: String,
    content: String,
    modifier: Modifier = Modifier,
    noteType: NoteType = NoteType.PERMANENT,
    flashcardCount: Int = 0,
    onLearnFlashcards: () -> Unit = {},
    emotion: EmotionStamp? = null,
    actions: List<ActionStamp> = emptyList()
) {
    val stats = remember(content) {
        try {
            MdNativeHelper.calculateTextStats(content)
        } catch (_: Exception) {
            val wc = if (content.isBlank()) 0 else content.trim().split("\\s+".toRegex()).size
            val cc = content.length
            val rt = (wc / 200).coerceAtLeast(1)
            intArrayOf(wc, cc, 0, rt)
        }
    }
    val wordCount = stats[0]

    val noteColor = NoteTypeColors.forNoteType(noteType)

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(noteColor)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (relativePath.isBlank()) "Vault Root" else relativePath,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "$wordCount words",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (flashcardCount > 0) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.clickable { onLearnFlashcards() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Style,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Flashcards ($flashcardCount)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }

            if (emotion != null || actions.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (emotion != null) {
                        StampChip(
                            label = emotion.label,
                            icon = {
                                Icon(
                                    imageVector = emotion.icon,
                                    contentDescription = null,
                                    tint = emotion.color,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        )
                    }
                    actions.forEach { action ->
                        StampChip(
                            label = action.label,
                            icon = {
                                Icon(
                                    imageVector = action.icon,
                                    contentDescription = null,
                                    tint = action.color,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun StampChip(
    label: String,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@Preview(
    showBackground = true,
    name = "1. Editor Metadata Bar - Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun EditorMetadataBarPreview_Dark() {
    AppTheme(darkTheme = true) {
        EditorMetadataBar(
            relativePath = "Projects/Roadmap.md",
            content = "Sample content for Zettelkasten note with several words and characters.",
            noteType = NoteType.DIARY,
            emotion = EmotionStamp.HAPPY,
            actions = listOf(ActionStamp.WORK, ActionStamp.EXERCISE)
        )
    }
}

@Preview(
    showBackground = true,
    name = "2. Editor Metadata Bar - Light",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
fun EditorMetadataBarPreview_Light() {
    AppTheme(darkTheme = false) {
        EditorMetadataBar(
            relativePath = "Projects/Roadmap.md",
            content = "Sample content for Zettelkasten note with several words and characters.",
            noteType = NoteType.DIARY,
            emotion = EmotionStamp.HAPPY,
            actions = listOf(ActionStamp.WORK, ActionStamp.EXERCISE)
        )
    }
}
