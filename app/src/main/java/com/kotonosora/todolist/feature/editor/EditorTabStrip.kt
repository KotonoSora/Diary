package com.kotonosora.todolist.feature.editor

import android.content.res.Configuration
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kotonosora.todolist.ui.theme.TodoListTheme

data class EditorTabItem(
    val id: String,
    val title: String
)

@Composable
fun EditorTabStrip(
    openTabs: List<EditorTabItem>,
    activeTabId: String,
    onTabSelect: (String) -> Unit,
    onTabClose: (String) -> Unit,
    onNewTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            openTabs.forEach { tab ->
                val isSelected = tab.id == activeTabId
                FilterChip(
                    selected = isSelected,
                    onClick = { onTabSelect(tab.id) },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = tab.title.ifBlank { "Untitled" },
                                style = MaterialTheme.typography.labelMedium
                            )
                            Spacer(Modifier.width(4.dp))
                            IconButton(
                                onClick = { onTabClose(tab.id) },
                                modifier = Modifier.size(18.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close tab",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.padding(end = 6.dp)
                )
            }

            IconButton(
                onClick = onNewTab,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Tab",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// ── FULL CASE-BY-CASE PREVIEWS ──

@Preview(
    showBackground = true,
    name = "1. Editor Tab Strip - Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun EditorTabStripPreview_Dark() {
    val tabs = listOf(
        EditorTabItem("1", "Project Roadmap"),
        EditorTabItem("2", "Zettel Concept"),
        EditorTabItem("3", "Meeting Notes")
    )

    TodoListTheme(darkTheme = true) {
        EditorTabStrip(
            openTabs = tabs,
            activeTabId = "1",
            onTabSelect = {},
            onTabClose = {},
            onNewTab = {}
        )
    }
}

@Preview(
    showBackground = true,
    name = "2. Editor Tab Strip - Light",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
fun EditorTabStripPreview_Light() {
    val tabs = listOf(
        EditorTabItem("1", "Project Roadmap"),
        EditorTabItem("2", "Zettel Concept")
    )

    TodoListTheme(darkTheme = false) {
        EditorTabStrip(
            openTabs = tabs,
            activeTabId = "1",
            onTabSelect = {},
            onTabClose = {},
            onNewTab = {}
        )
    }
}
