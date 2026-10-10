package com.kotonosora.todolist.feature.mood

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kotonosora.todolist.common.AppConstants
import com.kotonosora.todolist.domain.model.EmotionStamp
import com.kotonosora.todolist.domain.model.MoodEntry
import com.kotonosora.todolist.navigation.appViewModel
import com.kotonosora.todolist.ui.theme.AppTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.TimeUnit

private data class MoodSummary(
    val weekLogCount: Int,
    val streakDays: Int,
    val topEmotion: EmotionStamp?
)

/** Derived presentation stats for the last 7 days (inclusive). */
private fun summarize(
    days: List<MoodDay>,
    nowMillis: Long = System.currentTimeMillis()
): MoodSummary {
    val todayStart = MoodTimelineViewModel.startOfDayMillis(nowMillis)
    val weekStart = todayStart - TimeUnit.DAYS.toMillis(6)
    val weekEntries = days
        .filter { it.dayStartMillis >= weekStart }
        .flatMap { it.entries }
    val topEmotion = weekEntries
        .groupingBy { it.emotion }
        .eachCount()
        .maxByOrNull { it.value }
        ?.key
    val loggedDays = days.map { it.dayStartMillis }.toSet()
    var streak = 0
    var cursor = todayStart
    if (cursor !in loggedDays) cursor -= TimeUnit.DAYS.toMillis(1)
    while (cursor in loggedDays) {
        streak++
        cursor -= TimeUnit.DAYS.toMillis(1)
    }
    return MoodSummary(
        weekLogCount = weekEntries.size,
        streakDays = streak,
        topEmotion = topEmotion
    )
}

@Composable
fun MoodTimelineScreen(
    viewModel: MoodTimelineViewModel = appViewModel { container ->
        MoodTimelineViewModel(container.moodUseCases)
    },
    onNoteClick: (String) -> Unit = {},
    onOpenDrawer: (() -> Unit)? = null
) {
    val timeline by viewModel.timeline.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var logSheetEntry by remember { mutableStateOf<MoodTimelineEntry?>(null) }
    var showLogSheet by remember { mutableStateOf(false) }
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.error) {
        val error = uiState.error ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(error)
        viewModel.clearError()
    }

    MoodTimelineContent(
        days = timeline,
        snackbarHostState = snackbarHostState,
        onNoteClick = onNoteClick,
        onOpenDrawer = onOpenDrawer,
        onAddClick = {
            logSheetEntry = null
            showLogSheet = true
        },
        onEditClick = { entry ->
            logSheetEntry = entry
            showLogSheet = true
        },
        onDeleteClick = { pendingDeleteId = it }
    )

    if (showLogSheet) {
        LogMoodSheet(
            editingEntry = logSheetEntry,
            onDismiss = { showLogSheet = false },
            onSave = { emotion, note ->
                val editing = logSheetEntry
                if (editing == null) {
                    viewModel.logMood(emotion, note)
                } else {
                    // createdAt is preserved so edits never move the day group.
                    viewModel.updateMood(
                        MoodEntry(
                            id = editing.id,
                            emotion = emotion,
                            note = note.trim(),
                            linkedNoteId = editing.linkedNoteId,
                            createdAt = editing.createdAt
                        )
                    )
                }
                showLogSheet = false
            }
        )
    }

    val deleteId = pendingDeleteId
    if (deleteId != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text("Delete mood?") },
            text = { Text("This entry will be permanently removed from your timeline.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteMoodById(deleteId)
                        pendingDeleteId = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun MoodTimelineContent(
    days: List<MoodDay>,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onNoteClick: (String) -> Unit = {},
    onOpenDrawer: (() -> Unit)? = null,
    onAddClick: () -> Unit = {},
    onEditClick: (MoodTimelineEntry) -> Unit = {},
    onDeleteClick: (String) -> Unit = {}
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = "Log mood")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        text = "Mood Timeline",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (days.isEmpty()) {
                item {
                    MoodEmptyState(onAddClick = onAddClick)
                }
            } else {
                item {
                    MoodSummaryCard(summary = summarize(days))
                }
            }

            items(days, key = { it.dayStartMillis }) { day ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = formatDay(day.dayStartMillis),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${day.entries.size} logs",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            day.entries.forEach { entry ->
                                MoodEntryRow(
                                    entry = entry,
                                    onNoteClick = onNoteClick,
                                    onEditClick = { onEditClick(entry) },
                                    onDeleteClick = { onDeleteClick(entry.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MoodSummaryCard(summary: MoodSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MoodSummaryStat(
                icon = {
                    Icon(
                        Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                value = "${summary.streakDays}-day",
                label = "streak"
            )
            MoodSummaryStat(
                icon = {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                value = summary.weekLogCount.toString(),
                label = "logs this week"
            )
            val topEmotion = summary.topEmotion
            if (topEmotion != null) {
                MoodSummaryStat(
                    icon = {
                        Icon(
                            topEmotion.icon,
                            contentDescription = topEmotion.label,
                            tint = topEmotion.color
                        )
                    },
                    value = topEmotion.label,
                    label = "most felt"
                )
            }
        }
    }
}

@Composable
private fun MoodSummaryStat(
    icon: @Composable () -> Unit,
    value: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        icon()
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun MoodEntryRow(
    entry: MoodTimelineEntry,
    onNoteClick: (String) -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val linkedNoteId = entry.linkedNoteId
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (linkedNoteId != null) {
                    Modifier.clickable { onNoteClick(linkedNoteId) }
                } else {
                    Modifier
                }
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = entry.emotion.color.copy(alpha = 0.18f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = entry.emotion.icon,
                        contentDescription = entry.emotion.label,
                        tint = entry.emotion.color,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.emotion.label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = formatTime(entry.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (entry.note.isNotBlank()) {
                    Text(
                        text = entry.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Entry options")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = {
                            showMenu = false
                            onEditClick()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Delete",
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            showMenu = false
                            onDeleteClick()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogMoodSheet(
    editingEntry: MoodTimelineEntry? = null,
    onDismiss: () -> Unit = {},
    onSave: (EmotionStamp, String) -> Unit = { _, _ -> }
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        LogMoodSheetContent(
            editingEntry = editingEntry,
            onSave = {
                onSave(it.first, it.second)
                onDismiss()
            }
        )
    }
}

/**
 * Sheet body split out so @Preview can render it: [ModalBottomSheet] is a
 * dialog and shows nothing in previews. Scrollable + IME-aware so focusing
 * the note field lifts the content above the keyboard instead of hiding
 * the save button behind it.
 */
@Composable
fun LogMoodSheetContent(
    editingEntry: MoodTimelineEntry? = null,
    onSave: (Pair<EmotionStamp, String>) -> Unit = {}
) {
    var selectedEmotion by remember(editingEntry?.id) {
        mutableStateOf(editingEntry?.emotion ?: EmotionStamp.HAPPY)
    }
    var note by remember(editingEntry?.id) {
        mutableStateOf(editingEntry?.note ?: "")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Hero: the currently selected emotion, large and tinted.
        Surface(
            shape = CircleShape,
            color = selectedEmotion.color.copy(alpha = 0.16f),
            modifier = Modifier.size(76.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = selectedEmotion.icon,
                    contentDescription = selectedEmotion.label,
                    tint = selectedEmotion.color,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = if (editingEntry == null) {
                "I'm feeling ${selectedEmotion.label.lowercase()}"
            } else {
                "Edit mood"
            },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Tap an emotion, add an optional note.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        // Static rows (not lazy): the sheet body already scrolls for the
        // keyboard, and a scrollable grid inside a scrollable column
        // breaks gesture handling.
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            EmotionStamp.entries.chunked(3).forEach { rowEmotions ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    rowEmotions.forEach { emotion ->
                        val selected = emotion == selectedEmotion
                        Card(
                            onClick = { selectedEmotion = emotion },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selected) {
                                    emotion.color.copy(alpha = 0.16f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerLow
                                }
                            ),
                            border = if (selected) {
                                BorderStroke(2.dp, emotion.color)
                            } else {
                                null
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Box {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = emotion.icon,
                                        contentDescription = emotion.label,
                                        tint = emotion.color,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = emotion.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (selected) {
                                            FontWeight.Bold
                                        } else {
                                            FontWeight.Normal
                                        },
                                        textAlign = TextAlign.Center
                                    )
                                }
                                if (selected) {
                                    Surface(
                                        shape = CircleShape,
                                        color = emotion.color,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .size(20.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("Note (optional)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4,
            shape = RoundedCornerShape(16.dp)
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { onSave(selectedEmotion to note) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                selectedEmotion.icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                if (editingEntry == null) {
                    "Save ${selectedEmotion.label} mood"
                } else {
                    "Save changes"
                }
            )
        }
    }
}

@Composable
private fun MoodEmptyState(onAddClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Emotion collage: three tinted stamps instead of one flat icon.
            Row(horizontalArrangement = Arrangement.spacedBy((-12).dp)) {
                listOf(EmotionStamp.HAPPY, EmotionStamp.CALM, EmotionStamp.REFLECTIVE)
                    .forEach { emotion ->
                        Surface(
                            shape = CircleShape,
                            color = emotion.color.copy(alpha = 0.18f),
                            border = BorderStroke(
                                2.dp,
                                MaterialTheme.colorScheme.surfaceContainerLow
                            ),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = emotion.icon,
                                    contentDescription = emotion.label,
                                    tint = emotion.color,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "No moods yet",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Log how you feel each day and watch your week take shape — streaks, patterns, and your most-felt emotion.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onAddClick) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Log your first mood")
            }
        }
    }
}

private fun formatDay(dayStartMillis: Long): String {
    return SimpleDateFormat("EEE, MMM d, yyyy", AppConstants.APP_LOCALE)
        .format(Date(dayStartMillis))
}

private fun formatTime(epochMillis: Long): String {
    return SimpleDateFormat("HH:mm", AppConstants.APP_LOCALE)
        .format(Date(epochMillis))
}

// ── FULL CASE-BY-CASE PREVIEWS ──

private fun previewDays(): List<MoodDay> {
    val now = System.currentTimeMillis()
    val day = TimeUnit.DAYS.toMillis(1)
    return listOf(
        MoodDay(
            dayStartMillis = MoodTimelineViewModel.startOfDayMillis(now),
            entries = listOf(
                MoodTimelineEntry("1", EmotionStamp.HAPPY, "Morning pages with coffee", null, now),
                MoodTimelineEntry("2", EmotionStamp.CALM, "", null, now - 3_600_000L)
            )
        ),
        MoodDay(
            dayStartMillis = MoodTimelineViewModel.startOfDayMillis(now - day),
            entries = listOf(
                MoodTimelineEntry("3", EmotionStamp.STRESSED, "Tough retro", "retro.md", now - day),
                MoodTimelineEntry("4", EmotionStamp.HAPPY, "Shipped the release", null, now - day)
            )
        )
    )
}

@Preview(
    showBackground = true,
    name = "1. Mood Timeline - Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun MoodTimelinePreview_Dark() {
    AppTheme(darkTheme = true) {
        MoodTimelineContent(days = previewDays())
    }
}

@Preview(
    showBackground = true,
    name = "2. Mood Timeline - Light",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
fun MoodTimelinePreview_Light() {
    AppTheme(darkTheme = false) {
        MoodTimelineContent(days = previewDays())
    }
}

@Preview(
    showBackground = true,
    name = "3. Mood Timeline - Empty - Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun MoodTimelinePreview_Empty_Dark() {
    AppTheme(darkTheme = true) {
        MoodTimelineContent(days = emptyList())
    }
}

@Preview(
    showBackground = true,
    name = "4. Mood Timeline - Single Entry - Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun MoodTimelinePreview_SingleEntry_Dark() {
    AppTheme(darkTheme = true) {
        MoodTimelineContent(
            days = listOf(
                MoodDay(
                    dayStartMillis = System.currentTimeMillis(),
                    entries = listOf(
                        MoodTimelineEntry("1", EmotionStamp.REFLECTIVE, "Evening notes", null, 0L)
                    )
                )
            )
        )
    }
}

@Preview(
    showBackground = true,
    name = "5. Mood Timeline - Single Entry - Light",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
fun MoodTimelinePreview_SingleEntry_Light() {
    AppTheme(darkTheme = false) {
        MoodTimelineContent(
            days = listOf(
                MoodDay(
                    dayStartMillis = System.currentTimeMillis(),
                    entries = listOf(
                        MoodTimelineEntry("1", EmotionStamp.REFLECTIVE, "", null, 0L)
                    )
                )
            )
        )
    }
}

@Preview(
    showBackground = true,
    name = "6. Log Mood Sheet - Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun LogMoodSheetPreview_Dark() {
    AppTheme(darkTheme = true) {
        // ModalBottomSheet is a dialog and renders nothing in preview —
        // render the sheet body directly instead.
        Surface(color = MaterialTheme.colorScheme.surface) {
            LogMoodSheetContent()
        }
    }
}

@Preview(
    showBackground = true,
    name = "7. Log Mood Sheet - Light",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
fun LogMoodSheetPreview_Light() {
    AppTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            LogMoodSheetContent(
                editingEntry = MoodTimelineEntry("1", EmotionStamp.TIRED, "Long day", null, 0L)
            )
        }
    }
}
