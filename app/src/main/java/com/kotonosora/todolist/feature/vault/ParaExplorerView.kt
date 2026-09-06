package com.kotonosora.todolist.feature.vault

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Topic
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kotonosora.todolist.domain.model.NoteItem
import com.kotonosora.todolist.domain.model.ParaCategory

@Composable
fun ParaExplorerView(
    notes: List<NoteItem>,
    onNoteClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val projects = notes.filter { it.paraCategory == ParaCategory.PROJECT || it.relativePath.contains("1. Projects", ignoreCase = true) }
    val areas = notes.filter { it.paraCategory == ParaCategory.AREA || it.relativePath.contains("2. Areas", ignoreCase = true) }
    val resources = notes.filter { it.paraCategory == ParaCategory.RESOURCE || it.relativePath.contains("3. Resources", ignoreCase = true) }
    val archives = notes.filter { it.paraCategory == ParaCategory.ARCHIVE || it.relativePath.contains("4. Archives", ignoreCase = true) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "PARA Second Brain Framework",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        ParaSectionCard(
            title = "1. Projects (${projects.size})",
            icon = Icons.Default.FolderSpecial,
            notes = projects,
            onNoteClick = onNoteClick
        )

        Spacer(Modifier.height(8.dp))

        ParaSectionCard(
            title = "2. Areas (${areas.size})",
            icon = Icons.Default.Folder,
            notes = areas,
            onNoteClick = onNoteClick
        )

        Spacer(Modifier.height(8.dp))

        ParaSectionCard(
            title = "3. Resources (${resources.size})",
            icon = Icons.Default.Topic,
            notes = resources,
            onNoteClick = onNoteClick
        )

        Spacer(Modifier.height(8.dp))

        ParaSectionCard(
            title = "4. Archives (${archives.size})",
            icon = Icons.Default.Archive,
            notes = archives,
            onNoteClick = onNoteClick
        )
    }
}

@Composable
private fun ParaSectionCard(
    title: String,
    icon: ImageVector,
    notes: List<NoteItem>,
    onNoteClick: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (notes.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                notes.take(3).forEach { note ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNoteClick(note.id) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text(
                            text = note.title,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "PARA Explorer Preview")
@Composable
fun ParaExplorerViewPreview() {
    val sampleNotes = listOf(
        NoteItem("1. Projects/AppLaunch.md", "App Launch Goal", "1. Projects", "Launch details", paraCategory = ParaCategory.PROJECT),
        NoteItem("2. Areas/Health.md", "Health & Fitness", "2. Areas", "Daily habits", paraCategory = ParaCategory.AREA)
    )

    MaterialTheme {
        ParaExplorerView(
            notes = sampleNotes,
            onNoteClick = {}
        )
    }
}
