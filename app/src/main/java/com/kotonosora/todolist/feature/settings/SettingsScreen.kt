package com.kotonosora.todolist.feature.settings

import android.content.Intent
import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kotonosora.todolist.ui.theme.TodoListTheme

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateToGuide: () -> Unit = {},
    onOpenDrawer: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val themeMode by viewModel.themeMode.collectAsState()
    val customStorageUri by viewModel.customStorageUri.collectAsState()
    val defaultNoteFormat by viewModel.defaultNoteFormat.collectAsState()

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            try {
                context.contentResolver.takePersistableUriPermission(it, flags)
                viewModel.setCustomStorageUri(it.toString())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    SettingsScreenContent(
        themeMode = themeMode,
        customStorageUri = customStorageUri,
        defaultNoteFormat = defaultNoteFormat,
        onThemeModeChange = { viewModel.setThemeMode(it) },
        onPickFolderClick = { folderPickerLauncher.launch(null) },
        onResetFolderClick = { viewModel.setCustomStorageUri(null) },
        onDefaultNoteFormatChange = { viewModel.setDefaultNoteFormat(it) },
        onNavigateToGuide = onNavigateToGuide,
        onShareVaultClick = {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_TEXT, "Exported Markdown Vault Notes & Diary Workspace")
                type = "text/plain"
            }
            context.startActivity(Intent.createChooser(sendIntent, "Share App Workspace"))
        },
        onOpenDrawer = onOpenDrawer
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenContent(
    themeMode: String,
    customStorageUri: String?,
    defaultNoteFormat: String,
    onThemeModeChange: (String) -> Unit = {},
    onPickFolderClick: () -> Unit = {},
    onResetFolderClick: () -> Unit = {},
    onDefaultNoteFormatChange: (String) -> Unit = {},
    onNavigateToGuide: () -> Unit = {},
    onShareVaultClick: () -> Unit = {},
    onOpenDrawer: (() -> Unit)? = null
) {
    Scaffold { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onOpenDrawer != null) {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Default.Menu, contentDescription = "Open Sidebar")
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // ── 1. App Theme ──
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Text("Appearance & Theme", style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Choose whether the app should follow system defaults or stay in Dark/Light mode.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = themeMode == "system",
                                onClick = { onThemeModeChange("system") },
                                label = { Text("System") }
                            )
                            FilterChip(
                                selected = themeMode == "dark",
                                onClick = { onThemeModeChange("dark") },
                                label = { Text("Dark") },
                                leadingIcon = { Icon(Icons.Default.DarkMode, contentDescription = null) }
                            )
                            FilterChip(
                                selected = themeMode == "light",
                                onClick = { onThemeModeChange("light") },
                                label = { Text("Light") }
                            )
                        }
                    }
                }
            }

            // ── 2. Storage & Vault Folder ──
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.width(12.dp))
                            Text("Vault Storage Location", style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = customStorageUri ?: "Using app internal storage.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = onPickFolderClick) {
                                Text("Choose Folder")
                            }
                            if (customStorageUri != null) {
                                OutlinedButton(onClick = onResetFolderClick) {
                                    Text("Reset")
                                }
                            }
                        }
                    }
                }
            }

            // ── 3. Default Note Format ──
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Text("Default File Format", style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = defaultNoteFormat == "md",
                                onClick = { onDefaultNoteFormatChange("md") },
                                label = { Text(".md (Markdown)") }
                            )
                            FilterChip(
                                selected = defaultNoteFormat == "txt",
                                onClick = { onDefaultNoteFormatChange("txt") },
                                label = { Text(".txt (Plain Text)") }
                            )
                        }
                    }
                }
            }

            // ── 4. Feature Guide & Permission Assistant ──
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onNavigateToGuide
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Feature Guide & Permissions", style = MaterialTheme.typography.titleMedium)
                                Text("App overview and system permissions assistant", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    }
                }
            }

            // ── 5. Share & Export Vault ──
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onShareVaultClick
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Export / Share Vault", style = MaterialTheme.typography.titleMedium)
                            Text("Share notes and diary entries via system share sheet", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

// ── FULL CASE-BY-CASE PREVIEWS ──

@Preview(showBackground = true, name = "1. Settings Screen - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SettingsScreenPreview_Dark() {
    TodoListTheme(darkTheme = true) {
        SettingsScreenContent(
            themeMode = "dark",
            customStorageUri = "content://com.android.externalstorage.documents/tree/primary%3ADiaryVault",
            defaultNoteFormat = "md"
        )
    }
}

@Preview(showBackground = true, name = "2. Settings Screen - Light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun SettingsScreenPreview_Light() {
    TodoListTheme(darkTheme = false) {
        SettingsScreenContent(
            themeMode = "light",
            customStorageUri = null,
            defaultNoteFormat = "txt"
        )
    }
}
