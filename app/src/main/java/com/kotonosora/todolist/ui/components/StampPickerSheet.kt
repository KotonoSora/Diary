package com.kotonosora.todolist.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kotonosora.todolist.domain.model.ActionStamp
import com.kotonosora.todolist.domain.model.EmotionStamp
import com.kotonosora.todolist.ui.theme.TodoListTheme

/**
 * Stamp Picker Sheet component for selecting 1 Emotion Vector Stamp and multiple Action Vector Stamps.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StampPickerSheet(
    selectedEmotion: EmotionStamp?,
    selectedActions: List<ActionStamp>,
    onEmotionSelected: (EmotionStamp?) -> Unit,
    onActionToggle: (ActionStamp) -> Unit,
    modifier: Modifier = Modifier
) {
    val chipShape = RoundedCornerShape(12.dp)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Emotion Stamps Section
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Mood,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Emotion Stamp (Select 1)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                EmotionStamp.entries.forEach { emotion ->
                    val isSelected = selectedEmotion == emotion
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (isSelected) onEmotionSelected(null) else onEmotionSelected(emotion)
                        },
                        label = {
                            Text(
                                text = emotion.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = emotion.icon,
                                contentDescription = emotion.label,
                                tint = if (isSelected) Color.White else emotion.color,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        shape = chipShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = emotion.color,
                            selectedLabelColor = Color.White,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Action Stamps Section
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Activity Stamps (Select Multiple)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ActionStamp.entries.forEach { action ->
                    val isSelected = selectedActions.contains(action)
                    FilterChip(
                        selected = isSelected,
                        onClick = { onActionToggle(action) },
                        label = {
                            Text(
                                text = action.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = action.icon,
                                contentDescription = action.label,
                                tint = if (isSelected) Color.White else action.color,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        shape = chipShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = action.color,
                            selectedLabelColor = Color.White,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }
    }
}

// ── FULL CASE-BY-CASE PREVIEWS ──

@Preview(
    showBackground = true,
    name = "1. Stamp Picker Sheet - Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun StampPickerSheetPreview_Dark() {
    var selectedEmotion by remember { mutableStateOf<EmotionStamp?>(EmotionStamp.HAPPY) }
    var selectedActions by remember {
        mutableStateOf(
            listOf(
                ActionStamp.WORK,
                ActionStamp.EXERCISE
            )
        )
    }

    TodoListTheme(darkTheme = true) {
        Box(modifier = Modifier.padding(16.dp)) {
            StampPickerSheet(
                selectedEmotion = selectedEmotion,
                selectedActions = selectedActions,
                onEmotionSelected = { selectedEmotion = it },
                onActionToggle = { action ->
                    selectedActions = if (selectedActions.contains(action)) {
                        selectedActions - action
                    } else {
                        selectedActions + action
                    }
                }
            )
        }
    }
}

@Preview(
    showBackground = true,
    name = "2. Stamp Picker Sheet - Light",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
fun StampPickerSheetPreview_Light() {
    var selectedEmotion by remember { mutableStateOf<EmotionStamp?>(EmotionStamp.CALM) }
    var selectedActions by remember { mutableStateOf(listOf(ActionStamp.READING)) }

    TodoListTheme(darkTheme = false) {
        Box(modifier = Modifier.padding(16.dp)) {
            StampPickerSheet(
                selectedEmotion = selectedEmotion,
                selectedActions = selectedActions,
                onEmotionSelected = { selectedEmotion = it },
                onActionToggle = { action ->
                    selectedActions = if (selectedActions.contains(action)) {
                        selectedActions - action
                    } else {
                        selectedActions + action
                    }
                }
            )
        }
    }
}
