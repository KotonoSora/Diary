package com.kotonosora.todolist.feature.editor

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun BacklinkPanel(
    incomingLinks: List<String>,
    onNoteClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "Backlinks & Mentions (${incomingLinks.size})",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            if (incomingLinks.isEmpty()) {
                Text(
                    text = "No incoming links to this note.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn {
                    items(incomingLinks) { sourceNoteId ->
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

@Preview(showBackground = true, name = "Backlink Panel with Links")
@Composable
fun BacklinkPanelPreview_WithLinks() {
    MaterialTheme {
        BacklinkPanel(
            incomingLinks = listOf("Projects/Roadmap.md", "Ideas/Zettelkasten.md", "Work/Meeting.md"),
            onNoteClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Backlink Panel Empty")
@Composable
fun BacklinkPanelPreview_Empty() {
    MaterialTheme {
        BacklinkPanel(
            incomingLinks = emptyList(),
            onNoteClick = {}
        )
    }
}
