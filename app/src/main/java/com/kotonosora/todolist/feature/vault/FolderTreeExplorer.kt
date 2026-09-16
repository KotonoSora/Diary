package com.kotonosora.todolist.feature.vault

import android.content.Intent
import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kotonosora.todolist.domain.model.VaultNode
import com.kotonosora.todolist.ui.theme.TodoListTheme

@Composable
fun FolderTreeExplorer(
    rootNode: VaultNode.FolderNode,
    onNoteSelect: (String) -> Unit,
    onCreateNote: (String) -> Unit,
    onCreateFolder: (String) -> Unit,
    modifier: Modifier = Modifier,
    onDeleteNote: (String) -> Unit = {}
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = rootNode.name,
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
                onCreateFolder = onCreateFolder,
                onDeleteNote = onDeleteNote
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RenderFolderNode(
    folder: VaultNode.FolderNode,
    level: Int,
    onNoteSelect: (String) -> Unit,
    onCreateNote: (String) -> Unit,
    onCreateFolder: (String) -> Unit,
    onDeleteNote: (String) -> Unit
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(level == 0) }
    var activeActionFile by remember { mutableStateOf<VaultNode.FileNode?>(null) }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (level > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .combinedClickable(
                        onClick = { isExpanded = !isExpanded }
                    )
                    .padding(start = (level * 16).dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
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
                            onCreateFolder = onCreateFolder,
                            onDeleteNote = onDeleteNote
                        )
                    }

                    is VaultNode.FileNode -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .combinedClickable(
                                    onClick = { onNoteSelect(child.relativePath) },
                                    onLongClick = { activeActionFile = child }
                                )
                                .padding(start = ((level + 1) * 16).dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = child.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { activeActionFile = child },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "File Options",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (activeActionFile != null) {
        val file = activeActionFile!!
        FileActionsBottomSheet(
            fileName = file.name,
            filePath = file.relativePath,
            onDismiss = { activeActionFile = null },
            onOpenNote = { onNoteSelect(file.relativePath) },
            onRenameNote = { onNoteSelect(file.relativePath) },
            onShareNote = {
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    putExtra(Intent.EXTRA_TITLE, file.name)
                    putExtra(Intent.EXTRA_TEXT, "Markdown file: ${file.relativePath}")
                    type = "text/plain"
                }
                context.startActivity(Intent.createChooser(sendIntent, "Share Markdown File"))
            },
            onDeleteNote = { onDeleteNote(file.relativePath) }
        )
    }
}

// ── FULL CASE-BY-CASE PREVIEWS ──

@Preview(
    showBackground = true,
    name = "1. Folder Tree Explorer - Flat Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun FolderTreeExplorerPreview_Dark() {
    val sampleTree = VaultNode.FolderNode(
        name = "My Personal Vault",
        relativePath = "",
        children = listOf(
            VaultNode.FileNode("Index.md", "Index.md", "md", 1024, System.currentTimeMillis()),
            VaultNode.FolderNode(
                name = "Projects",
                relativePath = "Projects",
                children = listOf(
                    VaultNode.FileNode(
                        "Roadmap.md",
                        "Projects/Roadmap.md",
                        "md",
                        2048,
                        System.currentTimeMillis()
                    )
                )
            )
        )
    )

    TodoListTheme(darkTheme = true) {
        FolderTreeExplorer(
            rootNode = sampleTree,
            onNoteSelect = {},
            onCreateNote = {},
            onCreateFolder = {},
            onDeleteNote = {}
        )
    }
}

@Preview(
    showBackground = true,
    name = "2. Folder Tree Explorer - Flat Light",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
fun FolderTreeExplorerPreview_Light() {
    val sampleTree = VaultNode.FolderNode(
        name = "My Personal Vault",
        relativePath = "",
        children = listOf(
            VaultNode.FileNode("Index.md", "Index.md", "md", 1024, System.currentTimeMillis())
        )
    )

    TodoListTheme(darkTheme = false) {
        FolderTreeExplorer(
            rootNode = sampleTree,
            onNoteSelect = {},
            onCreateNote = {},
            onCreateFolder = {},
            onDeleteNote = {}
        )
    }
}
