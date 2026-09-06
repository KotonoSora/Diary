package com.kotonosora.todolist.feature.vault

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kotonosora.todolist.domain.model.NoteItem
import com.kotonosora.todolist.domain.model.NoteType
import com.kotonosora.todolist.domain.model.VaultNode
import com.kotonosora.todolist.feature.search.SearchScreen
import com.kotonosora.todolist.feature.tags.TagExplorerScreen

@Composable
fun VaultWorkspaceScreen(
    viewModel: VaultViewModel = hiltViewModel(),
    onNoteSelect: (String) -> Unit,
    onOpenGraph: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            try {
                context.contentResolver.takePersistableUriPermission(it, flags)
                viewModel.loadVault(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    VaultWorkspaceContent(
        uiState = uiState,
        onNoteSelect = onNoteSelect,
        onOpenGraph = onOpenGraph,
        onSelectCustomVaultFolder = { folderPickerLauncher.launch(null) },
        onCreateZettelNote = { folderPath, title, noteType, author, url, onCreated ->
            viewModel.createZettelNoteInFolder(folderPath, title, noteType, author, url, onCreated)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultWorkspaceContent(
    uiState: VaultUiState,
    onNoteSelect: (String) -> Unit,
    onOpenGraph: () -> Unit = {},
    onSelectCustomVaultFolder: () -> Unit = {},
    onCreateZettelNote: (String, String, NoteType, String?, String?, (String) -> Unit) -> Unit = { _, _, _, _, _, _ -> }
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showQuickCapture by remember { mutableStateOf(false) }
    var targetFolderPath by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Markdown Knowledge Base") },
                    actions = {
                        IconButton(onClick = onSelectCustomVaultFolder) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = "Open Local Vault Folder",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = onOpenGraph) {
                            Icon(
                                imageVector = Icons.Default.Hub,
                                contentDescription = "2D Knowledge Graph",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )

                PrimaryTabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Folder, contentDescription = null) },
                        text = { Text("Vault") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.Search, contentDescription = null) },
                        text = { Text("Search") }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null) },
                        text = { Text("Tags") }
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(onClick = {
                    targetFolderPath = ""
                    showQuickCapture = true
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Quick Capture Note")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                0 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Home & Recent Notes",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            SuggestionChip(
                                onClick = {},
                                label = { Text("Local Vault") }
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        if (uiState.notes.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                uiState.notes.take(5).forEach { note ->
                                    Card(
                                        modifier = Modifier
                                            .width(160.dp)
                                            .clickable { onNoteSelect(note.id) },
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Description,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(end = 4.dp)
                                                )
                                                Text(
                                                    text = note.title,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    maxLines = 1
                                                )
                                            }
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                text = note.content.take(60).replace("\n", " "),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2
                                            )
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                        }

                        FolderTreeExplorer(
                            rootNode = uiState.rootNode,
                            onNoteSelect = onNoteSelect,
                            onCreateNote = { folderPath ->
                                targetFolderPath = folderPath
                                showQuickCapture = true
                            },
                            onCreateFolder = { folderPath ->
                                targetFolderPath = folderPath
                                showQuickCapture = true
                            }
                        )
                    }
                }

                1 -> {
                    SearchScreen(onNoteClick = onNoteSelect)
                }

                2 -> {
                    TagExplorerScreen(onNoteClick = onNoteSelect)
                }
            }
        }

        if (showQuickCapture) {
            QuickCaptureDialog(
                onDismiss = { showQuickCapture = false },
                onConfirm = { title, noteType, author, url ->
                    onCreateZettelNote(targetFolderPath, title, noteType, author, url) { createdNoteId ->
                        showQuickCapture = false
                        onNoteSelect(createdNoteId)
                    }
                }
            )
        }
    }
}

@Preview(showBackground = true, name = "Home Workspace Screen Single Bottom Bar")
@Composable
fun VaultWorkspaceScreenPreview() {
    val sampleTree = VaultNode.FolderNode(
        name = "My Knowledge Base",
        relativePath = "",
        children = listOf(
            VaultNode.FileNode("Welcome.md", "Welcome.md", "md", 1024, System.currentTimeMillis()),
            VaultNode.FolderNode(
                name = "Projects",
                relativePath = "Projects",
                children = listOf(
                    VaultNode.FileNode("Roadmap.md", "Projects/Roadmap.md", "md", 2048, System.currentTimeMillis())
                )
            )
        )
    )

    val sampleNotes = listOf(
        NoteItem("Welcome.md", "Welcome", "", "Welcome to your personal Markdown Knowledge Base!"),
        NoteItem("Projects/Roadmap.md", "Project Roadmap", "Projects", "Milestones for Q1 architecture and local vault sync.")
    )

    MaterialTheme {
        VaultWorkspaceContent(
            uiState = VaultUiState(rootNode = sampleTree, notes = sampleNotes),
            onNoteSelect = {}
        )
    }
}
