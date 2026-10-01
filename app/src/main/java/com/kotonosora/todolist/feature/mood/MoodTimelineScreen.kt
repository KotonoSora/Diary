package com.kotonosora.todolist.feature.mood

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kotonosora.todolist.common.AppConstants
import com.kotonosora.todolist.domain.model.EmotionStamp
import com.kotonosora.todolist.navigation.appViewModel
import com.kotonosora.todolist.ui.theme.AppTheme
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun MoodTimelineScreen(
    viewModel: MoodTimelineViewModel = appViewModel { container ->
        MoodTimelineViewModel(container.vaultRepository)
    },
    onNoteClick: (String) -> Unit = {},
    onOpenDrawer: (() -> Unit)? = null
) {
    val timeline by viewModel.timeline.collectAsState()
    MoodTimelineContent(
        days = timeline,
        onNoteClick = onNoteClick,
        onOpenDrawer = onOpenDrawer
    )
}

@Composable
fun MoodTimelineContent(
    days: List<MoodDay>,
    onNoteClick: (String) -> Unit = {},
    onOpenDrawer: (() -> Unit)? = null
) {
    Scaffold { paddingValues ->
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
                    Text(
                        text = "No mood stamps yet — capture a note with an emotion stamp to start your timeline.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(days, key = { it.dayStartMillis }) { day ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = formatDay(day.dayStartMillis),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            day.entries.forEach { entry ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onNoteClick(entry.noteId) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(
                                            horizontal = 10.dp,
                                            vertical = 6.dp
                                        ),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = entry.emotion.icon,
                                            contentDescription = entry.emotion.label,
                                            tint = entry.emotion.color,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = entry.emotion.label,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = entry.title,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
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
    }
}

private fun formatDay(dayStartMillis: Long): String {
    return SimpleDateFormat("EEE, MMM d, yyyy", AppConstants.APP_LOCALE)
        .format(Date(dayStartMillis))
}

// ── FULL CASE-BY-CASE PREVIEWS ──

@Preview(
    showBackground = true,
    name = "1. Mood Timeline - Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun MoodTimelinePreview_Dark() {
    AppTheme(darkTheme = true) {
        MoodTimelineContent(
            days = listOf(
                MoodDay(
                    dayStartMillis = System.currentTimeMillis(),
                    entries = listOf(
                        MoodEntry("a.md", "Morning pages", EmotionStamp.HAPPY, 0L),
                        MoodEntry("b.md", "Deep work", EmotionStamp.CALM, 0L)
                    )
                ),
                MoodDay(
                    dayStartMillis = System.currentTimeMillis() - 86_400_000L,
                    entries = listOf(
                        MoodEntry("c.md", "Tough retro", EmotionStamp.STRESSED, 0L)
                    )
                )
            )
        )
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
        MoodTimelineContent(days = emptyList())
    }
}
