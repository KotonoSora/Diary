package com.kotonosora.todolist.feature.editor

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kotonosora.todolist.ui.theme.TodoListTheme

@Composable
fun BacklinkPanel(
    incomingLinks: List<String>,
    onNoteClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "Linked Notes (${incomingLinks.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            if (incomingLinks.isEmpty()) {
                Text(
                    text = "No linked notes referencing this entry.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn {
                    items(incomingLinks, key = { it }) { sourceNoteId ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNoteClick(sourceNoteId) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = sourceNoteId,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── FULL CASE-BY-CASE PREVIEWS ──

@Preview(showBackground = true, name = "1. Backlink Panel With Links - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun BacklinkPanelPreview_WithLinks_Dark() {
    TodoListTheme(darkTheme = true) {
        BacklinkPanel(
            incomingLinks = listOf("Projects/Roadmap.md", "Ideas/Concepts.md", "Work/Meeting.md"),
            onNoteClick = {}
        )
    }
}

@Preview(showBackground = true, name = "2. Backlink Panel Empty - Light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun BacklinkPanelPreview_Empty_Light() {
    TodoListTheme(darkTheme = false) {
        BacklinkPanel(
            incomingLinks = emptyList(),
            onNoteClick = {}
        )
    }
}
