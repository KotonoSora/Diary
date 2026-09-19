package com.kotonosora.todolist.feature.vault

import android.content.Intent
import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import com.kotonosora.todolist.domain.model.ActionStamp
import com.kotonosora.todolist.domain.model.EmotionStamp
import com.kotonosora.todolist.domain.model.NoteItem
import com.kotonosora.todolist.domain.model.NoteType
import com.kotonosora.todolist.domain.model.VaultNode
import com.kotonosora.todolist.feature.search.SearchScreen
import com.kotonosora.todolist.feature.tags.TagExplorerScreen
import com.kotonosora.todolist.navigation.appViewModel
import com.kotonosora.todolist.ui.theme.TodoListTheme

@Composable
fun VaultWorkspaceScreen(
    viewModel: VaultViewModel = appViewModel { container ->
        VaultViewModel(
            container.vaultRepository,
            container.userPreferencesRepository
        )
    },
    onNoteSelect: (String) -> Unit,
    onOpenGraph: () -> Unit = {},
    onOpenDrawer: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            val flags =
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
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
        onOpenDrawer = onOpenDrawer,
        onSelectCustomVaultFolder = { folderPickerLauncher.launch(null) },
        onCreateZettelNote = { folderPath, title, noteType, author, url, emotion, actions, onCreated ->
            viewModel.createZettelNoteInFolder(
                folderPath,
                title,
                noteType,
                author,
                url,
                emotion,
                actions,
                onCreated
            )
        },
        onCreateFolder = { parentFolderPath, folderName ->
            viewModel.createFolder(parentFolderPath, folderName)
        },
        onMoveNote = { srcNotePath, destFolderPath ->
            viewModel.moveNote(srcNotePath, destFolderPath)
        },
        onMoveFolder = { srcFolderPath, destFolderPath ->
            viewModel.moveFolder(srcFolderPath, destFolderPath)
        },
        onDeleteNote = { relativePath ->
            viewModel.deleteNote(relativePath)
        },
        onDeleteFolder = { folderPath ->
            viewModel.deleteFolder(folderPath)
        },
        onRefresh = {
            viewModel.loadVault()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultWorkspaceContent(
    uiState: VaultUiState,
    onNoteSelect: (String) -> Unit,
    onOpenGraph: () -> Unit = {},
    onOpenDrawer: (() -> Unit)? = null,
    onSelectCustomVaultFolder: () -> Unit = {},
    onCreateZettelNote: (String, String, NoteType, String?, String?, EmotionStamp?, List<ActionStamp>, (String) -> Unit) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onCreateFolder: (parentFolderPath: String, folderName: String) -> Unit = { _, _ -> },
    onMoveNote: (srcNotePath: String, destFolderPath: String) -> Unit = { _, _ -> },
    onMoveFolder: (srcFolderPath: String, destFolderPath: String) -> Unit = { _, _ -> },
    onDeleteNote: (String) -> Unit = {},
    onDeleteFolder: (folderPath: String) -> Unit = {},
    onRefresh: () -> Unit = {}
) {
    var selectedFilterTab by remember { mutableIntStateOf(0) } // 0: Files, 1: Search, 2: Tags
    var showQuickCapture by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var targetFolderPath by remember { mutableStateOf("") }

    Scaffold(
        floatingActionButton = {
            if (selectedFilterTab == 0) {
                FloatingActionButton(
                    onClick = {
                        targetFolderPath = ""
                        showQuickCapture = true
                    },
                    elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation(0.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Quick Capture")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Flat Frameless Action Header Row (No TopAppBar, No Header Title)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (onOpenDrawer != null) {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Default.Menu, contentDescription = "Open Sidebar")
                        }
                    } else {
                        Spacer(Modifier.width(48.dp))
                    }

                    // Flat Filter Chips
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = selectedFilterTab == 0,
                            onClick = { selectedFilterTab = 0 },
                            label = { Icon(Icons.Default.Folder, contentDescription = "Files") }
                        )
                        FilterChip(
                            selected = selectedFilterTab == 1,
                            onClick = { selectedFilterTab = 1 },
                            label = { Icon(Icons.Default.Search, contentDescription = "Search") }
                        )
                        FilterChip(
                            selected = selectedFilterTab == 2,
                            onClick = { selectedFilterTab = 2 },
                            label = {
                                Icon(
                                    Icons.AutoMirrored.Filled.Label,
                                    contentDescription = "Tags"
                                )
                            }
                        )
                    }

                    Row {
                        IconButton(onClick = onOpenGraph) {
                            Icon(
                                Icons.Default.Hub,
                                contentDescription = "Graph View",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Options")
                        }

                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Open Vault Folder") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.FolderOpen,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    onSelectCustomVaultFolder()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("2D Knowledge Graph") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Hub,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    onOpenGraph()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("New Capture Note") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    targetFolderPath = ""
                                    showQuickCapture = true
                                }
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))

                when (selectedFilterTab) {
                    0 -> {
                        val pullToRefreshState = rememberPullToRefreshState()
                        PullToRefreshBox(
                            isRefreshing = uiState.isLoading,
                            onRefresh = onRefresh,
                            state = pullToRefreshState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                contentPadding = PaddingValues(bottom = 80.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                item {
                                    FolderTreeExplorer(
                                        rootNode = uiState.rootNode,
                                        onNoteSelect = onNoteSelect,
                                        onCreateNote = { folderPath ->
                                            targetFolderPath = folderPath
                                            showQuickCapture = true
                                        },
                                        onCreateFolder = onCreateFolder,
                                        onMoveNote = onMoveNote,
                                        onMoveFolder = onMoveFolder,
                                        onDeleteNote = onDeleteNote,
                                        onDeleteFolder = onDeleteFolder
                                    )
                                }
                            }
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
        }

        if (showQuickCapture) {
            QuickCaptureDialog(
                onDismiss = { showQuickCapture = false },
                onConfirm = { title, noteType, author, url, emotion, actions ->
                    onCreateZettelNote(
                        targetFolderPath,
                        title,
                        noteType,
                        author,
                        url,
                        emotion,
                        actions
                    ) { createdNoteId ->
                        showQuickCapture = false
                        onNoteSelect(createdNoteId)
                    }
                }
            )
        }
    }
}

// ── FULL CASE-BY-CASE PREVIEWS ──

@Preview(
    showBackground = true,
    name = "1. Vault Workspace - Flat Dark Design",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun VaultWorkspaceScreenPreview_Populated_Dark() {
    val sampleTree = VaultNode.FolderNode(
        name = "My Personal Vault",
        relativePath = "",
        children = listOf(
            VaultNode.FileNode("Welcome.md", "Welcome.md", "md", 1024, System.currentTimeMillis()),
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

    val sampleNotes = listOf(
        NoteItem("Welcome.md", "Welcome", "", "Welcome to your personal Markdown Knowledge Base!"),
        NoteItem(
            "Projects/Roadmap.md",
            "Project Roadmap",
            "Projects",
            "Milestones for Q1 architecture and local vault sync."
        )
    )

    TodoListTheme(darkTheme = true) {
        VaultWorkspaceContent(
            uiState = VaultUiState(rootNode = sampleTree, notes = sampleNotes),
            onNoteSelect = {}
        )
    }
}

@Preview(
    showBackground = true,
    name = "2. Vault Workspace - Flat Light Design",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
fun VaultWorkspaceScreenPreview_Populated_Light() {
    val sampleTree = VaultNode.FolderNode(
        name = "My Personal Vault",
        relativePath = "",
        children = listOf(
            VaultNode.FileNode("Welcome.md", "Welcome.md", "md", 1024, System.currentTimeMillis())
        )
    )

    val sampleNotes = listOf(
        NoteItem("Welcome.md", "Welcome", "", "Welcome to your personal Markdown Knowledge Base!")
    )

    TodoListTheme(darkTheme = false) {
        VaultWorkspaceContent(
            uiState = VaultUiState(rootNode = sampleTree, notes = sampleNotes),
            onNoteSelect = {}
        )
    }
}
