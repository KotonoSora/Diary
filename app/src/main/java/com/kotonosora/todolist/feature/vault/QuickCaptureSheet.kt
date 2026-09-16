package com.kotonosora.todolist.feature.vault

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.kotonosora.todolist.domain.model.NoteType
import com.kotonosora.todolist.domain.model.defaultTitlePrefix
import com.kotonosora.todolist.domain.model.description
import com.kotonosora.todolist.domain.model.displayName
import com.kotonosora.todolist.ui.components.StampPickerSheet
import com.kotonosora.todolist.ui.theme.TodoListTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickCaptureDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, noteType: NoteType, author: String?, url: String?, emotion: EmotionStamp?, actions: List<ActionStamp>) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        QuickCaptureSheetContent(
            onDismiss = onDismiss,
            onConfirm = onConfirm
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickCaptureSheetContent(
    onDismiss: () -> Unit = {},
    onConfirm: (title: String, noteType: NoteType, author: String?, url: String?, emotion: EmotionStamp?, actions: List<ActionStamp>) -> Unit = { _, _, _, _, _, _ -> }
) {
    var selectedType by remember { mutableStateOf(NoteType.FLEETING) }
    var title by remember { mutableStateOf(selectedType.defaultTitlePrefix) }
    var author by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    var selectedEmotion by remember { mutableStateOf<EmotionStamp?>(null) }
    var selectedActions by remember { mutableStateOf<List<ActionStamp>>(emptyList()) }

    val compactShape = RoundedCornerShape(8.dp)

    // Update title when switching type if title matches default prefix pattern
    LaunchedEffect(selectedType) {
        if (title.isBlank() || NoteType.entries.any { title == it.defaultTitlePrefix }) {
            title = selectedType.defaultTitlePrefix
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "New Note from Template",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = selectedType.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Template selection scrollable row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(NoteType.entries.toTypedArray()) { type ->
                    val isSelected = selectedType == type
                    val icon = when (type) {
                        NoteType.FLEETING -> Icons.Default.EditNote
                        NoteType.LITERATURE -> Icons.Default.Book
                        NoteType.PERMANENT -> Icons.Default.AutoAwesome
                        NoteType.MOC -> Icons.Default.Hub
                        NoteType.DIARY -> Icons.Default.Book
                        NoteType.DAILY -> Icons.Default.Event
                        NoteType.REPORT -> Icons.AutoMirrored.Filled.Assignment
                        NoteType.TODO -> Icons.Default.Checklist
                        NoteType.FLASHCARD -> Icons.Default.Style
                    }

                    Card(
                        modifier = Modifier
                            .width(130.dp)
                            .clickable { selectedType = type },
                        shape = compactShape,
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(
                                alpha = 0.4f
                            )
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = type.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = type.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Title Input
            TextField(
                value = title,
                onValueChange = { title = it.take(100) },
                label = { Text("Note Title", style = MaterialTheme.typography.labelSmall) },
                placeholder = {
                    Text(
                        "Enter title...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                singleLine = true,
                shape = compactShape,
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            if (selectedType == NoteType.LITERATURE || selectedType == NoteType.REPORT) {
                Spacer(Modifier.height(6.dp))
                TextField(
                    value = author,
                    onValueChange = { author = it.take(100) },
                    placeholder = {
                        Text(
                            "Author / Reporter",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    singleLine = true,
                    shape = compactShape,
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                            alpha = 0.2f
                        )
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (selectedType == NoteType.LITERATURE) {
                Spacer(Modifier.height(6.dp))
                TextField(
                    value = url,
                    onValueChange = { url = it.take(200) },
                    placeholder = {
                        Text(
                            "Reference URL",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    singleLine = true,
                    shape = compactShape,
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                            alpha = 0.2f
                        )
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(12.dp))

            // Stamp Picker Section (Emotions & Activities)
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

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = compactShape,
                    border = null
                ) {
                    Text("Cancel", style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            onConfirm(
                                title,
                                selectedType,
                                author.ifBlank { null },
                                url.ifBlank { null },
                                selectedEmotion,
                                selectedActions
                            )
                        }
                    },
                    shape = compactShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Text(
                        "Create from Template",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

// ── FULL CASE-BY-CASE PREVIEWS ──

@Preview(
    showBackground = true,
    name = "1. Quick Capture Sheet - Templates Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun QuickCaptureDialogPreview_Dark() {
    TodoListTheme(darkTheme = true) {
        QuickCaptureSheetContent()
    }
}

@Preview(
    showBackground = true,
    name = "2. Quick Capture Sheet - Templates Light",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
fun QuickCaptureDialogPreview_Light() {
    TodoListTheme(darkTheme = false) {
        QuickCaptureSheetContent()
    }
}
