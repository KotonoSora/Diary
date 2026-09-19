package com.kotonosora.todolist.feature.vault

import android.content.Intent
import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kotonosora.todolist.domain.model.VaultNode
import com.kotonosora.todolist.ui.theme.TodoListTheme

fun formatCleanDisplayName(rawName: String): String {
    return rawName.replace(Regex("""^\d{8,14}-?"""), "").ifBlank { rawName }
}

@Composable
fun FolderTreeExplorer(
    rootNode: VaultNode.FolderNode,
    onNoteSelect: (String) -> Unit,
    onCreateNote: (folderPath: String) -> Unit,
    onCreateFolder: (parentFolderPath: String, folderName: String) -> Unit,
    modifier: Modifier = Modifier,
    onMoveNote: (srcNotePath: String, destFolderPath: String) -> Unit = { _, _ -> },
    onMoveFolder: (srcFolderPath: String, destFolderPath: String) -> Unit = { _, _ -> },
    onDeleteNote: (String) -> Unit = {},
    onDeleteFolder: (folderPath: String) -> Unit = {}
) {
    var createFolderParentPath by remember { mutableStateOf<String?>(null) }
    var moveSrcItem by remember { mutableStateOf<Pair<VaultNode, Boolean>?>(null) }
    var folderActionTarget by remember { mutableStateOf<VaultNode.FolderNode?>(null) }
    var deleteFolderTarget by remember { mutableStateOf<VaultNode.FolderNode?>(null) }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        RenderFolderNode(
            folder = rootNode,
            level = 0,
            onNoteSelect = onNoteSelect,
            onCreateNote = onCreateNote,
            onRequestCreateFolder = { parentPath -> createFolderParentPath = parentPath },
            onRequestMoveItem = { node, isFolder -> moveSrcItem = Pair(node, isFolder) },
            onOpenFolderActions = { folder -> folderActionTarget = folder },
            onDeleteNote = onDeleteNote
        )
    }

    if (createFolderParentPath != null) {
        CreateFolderDialog(
            parentFolderPath = createFolderParentPath!!,
            onDismiss = { createFolderParentPath = null },
            onConfirm = { folderName ->
                onCreateFolder(createFolderParentPath!!, folderName)
                createFolderParentPath = null
            }
        )
    }

    if (moveSrcItem != null) {
        val (node, isFolder) = moveSrcItem!!
        MoveDestinationDialog(
            rootNode = rootNode,
            srcItemName = node.name,
            srcRelativePath = node.relativePath,
            isFolder = isFolder,
            onDismiss = { moveSrcItem = null },
            onConfirm = { destFolder ->
                if (isFolder) {
                    onMoveFolder(node.relativePath, destFolder)
                } else {
                    onMoveNote(node.relativePath, destFolder)
                }
                moveSrcItem = null
            }
        )
    }

    if (folderActionTarget != null) {
        val folder = folderActionTarget!!
        FolderActionsBottomSheet(
            folderName = folder.name,
            folderPath = folder.relativePath,
            isRoot = folder.relativePath.isBlank(),
            onDismiss = { folderActionTarget = null },
            onCreateNote = { onCreateNote(folder.relativePath) },
            onCreateFolder = { createFolderParentPath = folder.relativePath },
            onMoveFolder = if (folder.relativePath.isNotBlank()) {
                { moveSrcItem = Pair(folder, true) }
            } else null,
            onDeleteFolder = if (folder.relativePath.isNotBlank()) {
                { deleteFolderTarget = folder }
            } else null
        )
    }

    if (deleteFolderTarget != null) {
        val folder = deleteFolderTarget!!
        DeleteFolderConfirmationDialog(
            folderName = folder.name,
            onDismiss = { deleteFolderTarget = null },
            onConfirm = {
                onDeleteFolder(folder.relativePath)
                deleteFolderTarget = null
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RenderFolderNode(
    folder: VaultNode.FolderNode,
    level: Int,
    onNoteSelect: (String) -> Unit,
    onCreateNote: (String) -> Unit,
    onRequestCreateFolder: (String) -> Unit,
    onRequestMoveItem: (VaultNode, Boolean) -> Unit,
    onOpenFolderActions: (VaultNode.FolderNode) -> Unit,
    onDeleteNote: (String) -> Unit
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(level == 0) }
    var activeActionFile by remember { mutableStateOf<VaultNode.FileNode?>(null) }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (level == 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = folder.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row {
                    IconButton(
                        onClick = { onCreateNote(folder.relativePath) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.NoteAdd,
                            contentDescription = "New Note in Root",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = { onRequestCreateFolder(folder.relativePath) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreateNewFolder,
                            contentDescription = "New Subfolder in Root",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .combinedClickable(
                        onClick = { isExpanded = !isExpanded },
                        onLongClick = { onOpenFolderActions(folder) }
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
                IconButton(
                    onClick = { onOpenFolderActions(folder) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Folder Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
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
                            onRequestCreateFolder = onRequestCreateFolder,
                            onRequestMoveItem = onRequestMoveItem,
                            onOpenFolderActions = onOpenFolderActions,
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
                                text = formatCleanDisplayName(child.name),
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
            onDeleteNote = { onDeleteNote(file.relativePath) },
            onMoveNote = { onRequestMoveItem(file, false) }
        )
    }
}

@Composable
fun CreateFolderDialog(
    parentFolderPath: String,
    onDismiss: () -> Unit,
    onConfirm: (folderName: String) -> Unit
) {
    var folderName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (parentFolderPath.isBlank()) "New Folder" else "New folder in '$parentFolderPath'"
            )
        },
        text = {
            OutlinedTextField(
                value = folderName,
                onValueChange = { folderName = it },
                label = { Text("Folder Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (folderName.isNotBlank()) {
                        onConfirm(folderName.trim())
                    }
                },
                enabled = folderName.isNotBlank()
            ) {
                Text("Create Folder")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DeleteFolderConfirmationDialog(
    folderName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete folder '$folderName'") },
        text = { Text("Are you sure you want to delete this folder and all its contents? This action cannot be undone.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete Folder", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun MoveDestinationDialog(
    rootNode: VaultNode.FolderNode,
    srcItemName: String,
    srcRelativePath: String,
    isFolder: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (destFolderPath: String) -> Unit
) {
    val allFolders = remember(rootNode) { getAllFolderPaths(rootNode) }
    var selectedFolder by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Move ${if (isFolder) "folder" else "file"} '$srcItemName'")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
            ) {
                Text(
                    text = "Select target folder:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(allFolders) { (label, folderPath) ->
                        val isSelfOrChild =
                            isFolder && (folderPath == srcRelativePath || folderPath.startsWith("$srcRelativePath/"))
                        val isSelected = selectedFolder == folderPath

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isSelfOrChild) {
                                    selectedFolder = folderPath
                                },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(
                                alpha = 0.6f
                            )
                            else if (isSelfOrChild) MaterialTheme.colorScheme.surfaceVariant.copy(
                                alpha = 0.2f
                            )
                            else MaterialTheme.colorScheme.surface,
                            tonalElevation = if (isSelected) 2.dp else 0.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (folderPath.isBlank()) Icons.Default.FolderOpen else Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary
                                    else if (isSelfOrChild) MaterialTheme.colorScheme.onSurface.copy(
                                        alpha = 0.3f
                                    )
                                    else MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                    else if (isSelfOrChild) MaterialTheme.colorScheme.onSurface.copy(
                                        alpha = 0.3f
                                    )
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedFolder) }
            ) {
                Text("Move Here")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderActionsBottomSheet(
    folderName: String,
    folderPath: String,
    isRoot: Boolean,
    onDismiss: () -> Unit,
    onCreateNote: () -> Unit,
    onCreateFolder: () -> Unit,
    onMoveFolder: (() -> Unit)? = null,
    onDeleteFolder: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        val compactShape = RoundedCornerShape(6.dp)

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .padding(bottom = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isRoot) Icons.Default.FolderOpen else Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isRoot) "Root Vault" else folderName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (!isRoot) {
                            Text(
                                text = folderPath,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                // Create Note
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = compactShape,
                    elevation = CardDefaults.cardElevation(0.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    onClick = { onDismiss(); onCreateNote() }
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.NoteAdd,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "New Note",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Create Folder
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    shape = compactShape,
                    elevation = CardDefaults.cardElevation(0.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    onClick = { onDismiss(); onCreateFolder() }
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CreateNewFolder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "New Folder",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Move Folder
                if (onMoveFolder != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        shape = compactShape,
                        elevation = CardDefaults.cardElevation(0.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ),
                        onClick = { onDismiss(); onMoveFolder() }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.DriveFileMove,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Move Folder",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Delete Folder
                if (onDeleteFolder != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        shape = compactShape,
                        elevation = CardDefaults.cardElevation(0.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                        ),
                        onClick = { onDismiss(); onDeleteFolder() }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Delete Folder",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getAllFolderPaths(root: VaultNode.FolderNode): List<Pair<String, String>> {
    val result = mutableListOf<Pair<String, String>>()
    result.add(Pair("📁 Root Vault", ""))

    fun traverse(folder: VaultNode.FolderNode, depth: Int) {
        for (child in folder.children) {
            if (child is VaultNode.FolderNode) {
                val indent = "  ".repeat(depth) + "📁 "
                result.add(Pair("$indent${child.name}", child.relativePath))
                traverse(child, depth + 1)
            }
        }
    }
    traverse(root, 1)
    return result
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
            onCreateFolder = { _, _ -> },
            onDeleteNote = {},
            onDeleteFolder = {}
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
            onCreateFolder = { _, _ -> },
            onDeleteNote = {},
            onDeleteFolder = {}
        )
    }
}
