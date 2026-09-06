package com.kotonosora.todolist.feature.vault

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kotonosora.todolist.domain.model.VaultNode

@Composable
fun FolderTreeExplorer(
    rootNode: VaultNode.FolderNode,
    onNoteSelect: (String) -> Unit,
    onCreateNote: (String) -> Unit,
    onCreateFolder: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Vault: ${rootNode.name}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { onCreateNote(rootNode.relativePath) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.NoteAdd,
                        contentDescription = "New Note",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = { onCreateFolder(rootNode.relativePath) }) {
                    Icon(
                        imageVector = Icons.Default.CreateNewFolder,
                        contentDescription = "New Folder",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            RenderFolderNode(
                folder = rootNode,
                level = 0,
                onNoteSelect = onNoteSelect,
                onCreateNote = onCreateNote,
                onCreateFolder = onCreateFolder
            )
        }
    }
}

@Composable
private fun RenderFolderNode(
    folder: VaultNode.FolderNode,
    level: Int,
    onNoteSelect: (String) -> Unit,
    onCreateNote: (String) -> Unit,
    onCreateFolder: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(level == 0) }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (level > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(start = (level * 12).dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        if (isExpanded || level == 0) {
            folder.children.forEach { child ->
                when (child) {
                    is VaultNode.FolderNode -> {
                        RenderFolderNode(
                            folder = child,
                            level = level + 1,
                            onNoteSelect = onNoteSelect,
                            onCreateNote = onCreateNote,
                            onCreateFolder = onCreateFolder
                        )
                    }

                    is VaultNode.FileNode -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNoteSelect(child.relativePath) }
                                .padding(start = ((level + 1) * 12).dp, top = 6.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = child.name,
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

@Preview(showBackground = true, name = "Folder Tree Explorer")
@Composable
fun FolderTreeExplorerPreview() {
    val sampleTree = VaultNode.FolderNode(
        name = "My Vault",
        relativePath = "",
        children = listOf(
            VaultNode.FileNode("Index.md", "Index.md", "md", 1024, System.currentTimeMillis()),
            VaultNode.FolderNode(
                name = "Projects",
                relativePath = "Projects",
                children = listOf(
                    VaultNode.FileNode("Roadmap.md", "Projects/Roadmap.md", "md", 2048, System.currentTimeMillis()),
                    VaultNode.FileNode("Architecture.md", "Projects/Architecture.md", "md", 1536, System.currentTimeMillis())
                )
            ),
            VaultNode.FolderNode(
                name = "Journal",
                relativePath = "Journal",
                children = listOf(
                    VaultNode.FileNode("2026-03-01.md", "Journal/2026-03-01.md", "md", 512, System.currentTimeMillis())
                )
            )
        )
    )

    MaterialTheme {
        FolderTreeExplorer(
            rootNode = sampleTree,
            onNoteSelect = {},
            onCreateNote = {},
            onCreateFolder = {}
        )
    }
}
