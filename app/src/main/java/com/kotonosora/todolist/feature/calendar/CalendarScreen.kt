package com.kotonosora.todolist.feature.calendar

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.WeekCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.compose.weekcalendar.rememberWeekCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.WeekDay
import com.kotonosora.todolist.common.AppConstants
import com.kotonosora.todolist.domain.model.DayMarkerType
import com.kotonosora.todolist.domain.model.DayMarkers
import com.kotonosora.todolist.domain.model.MoodEntry
import com.kotonosora.todolist.domain.model.TaskItem
import com.kotonosora.todolist.ui.theme.AppTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Date

enum class CalendarViewMode {
    MONTH,
    WEEK
}

@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = viewModel(),
    onOpenDrawer: (() -> Unit)? = null,
    onNoteClick: ((String) -> Unit)? = null,
    onMoodClick: (() -> Unit)? = null
) {
    val year by viewModel.currentYear.collectAsState()
    val month by viewModel.currentMonth.collectAsState()
    val selectedDateMillis by viewModel.selectedDateMillis.collectAsState()
    val todosForDate by viewModel.todosForSelectedDate.collectAsState()
    val notesForDate by viewModel.notesForSelectedDate.collectAsState()
    val moodsForDate by viewModel.moodsForSelectedDate.collectAsState()
    val markers by viewModel.dayMarkers.collectAsState()

    CalendarScreenContent(
        year = year,
        month = month,
        selectedDateMillis = selectedDateMillis,
        todosForDate = todosForDate,
        notesForDate = notesForDate,
        moodsForDate = moodsForDate,
        markers = markers,
        onSelectDate = { viewModel.selectDate(it) },
        onSelectToday = { viewModel.selectToday() },
        onPreviousMonth = { viewModel.previousMonth() },
        onNextMonth = { viewModel.nextMonth() },
        onToggleTodo = { viewModel.toggleTodoStatus(it) },
        onNoteClick = onNoteClick,
        onMoodClick = onMoodClick,
        onOpenDrawer = onOpenDrawer
    )
}

@Composable
fun CalendarScreenContent(
    year: Int,
    month: Int,
    selectedDateMillis: Long,
    todosForDate: List<TaskItem>,
    notesForDate: List<com.kotonosora.todolist.domain.model.NoteItem> = emptyList(),
    moodsForDate: List<MoodEntry> = emptyList(),
    markers: Map<LocalDate, DayMarkers> = emptyMap(),
    onSelectDate: (Long) -> Unit = {},
    onSelectToday: () -> Unit = {},
    onPreviousMonth: () -> Unit = {},
    onNextMonth: () -> Unit = {},
    onToggleTodo: (TaskItem) -> Unit = {},
    onNoteClick: ((String) -> Unit)? = null,
    onMoodClick: (() -> Unit)? = null,
    onOpenDrawer: (() -> Unit)? = null
) {
    var viewMode by remember { mutableStateOf(CalendarViewMode.MONTH) }
    val scope = rememberCoroutineScope()

    // English (US) week convention: Sunday first. firstDayOfWeekFromLocale()
    // follows the device locale and would shift the grid on non-English phones.
    val firstDayOfWeek = remember { DayOfWeek.SUNDAY }
    val selectedLocalDate = remember(selectedDateMillis) {
        Instant.ofEpochMilli(selectedDateMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    }

    val currentMonth = remember(year, month) {
        YearMonth.of(year, month + 1)
    }

    val calendarState = rememberCalendarState(
        startMonth = currentMonth.minusMonths(12),
        endMonth = currentMonth.plusMonths(12),
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = firstDayOfWeek
    )

    val weekCalendarState = rememberWeekCalendarState(
        startDate = selectedLocalDate.minusWeeks(4),
        endDate = selectedLocalDate.plusWeeks(4),
        firstVisibleWeekDate = selectedLocalDate,
        firstDayOfWeek = firstDayOfWeek
    )

    LaunchedEffect(year, month) {
        if (calendarState.firstVisibleMonth.yearMonth != currentMonth) {
            calendarState.animateScrollToMonth(currentMonth)
        }
    }

    val selectedDateText = remember(selectedDateMillis) {
        val sdf = SimpleDateFormat("EEEE, MMMM d, yyyy", AppConstants.APP_LOCALE)
        sdf.format(Date(selectedDateMillis))
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ── Frameless Action Header ──
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

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onPreviousMonth) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous Month"
                        )
                    }
                    Text(
                        text = "${
                            currentMonth.month.getDisplayName(
                                TextStyle.FULL,
                                AppConstants.APP_LOCALE
                            )
                        } $year",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onNextMonth) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next Month"
                        )
                    }
                }

                Row {
                    IconButton(onClick = {
                        viewMode =
                            if (viewMode == CalendarViewMode.MONTH) CalendarViewMode.WEEK else CalendarViewMode.MONTH
                    }) {
                        Icon(
                            imageVector = if (viewMode == CalendarViewMode.MONTH) Icons.Default.CalendarViewWeek else Icons.Default.CalendarMonth,
                            contentDescription = "Toggle Calendar View"
                        )
                    }

                    IconButton(onClick = {
                        onSelectToday()
                        scope.launch {
                            val today = LocalDate.now()
                            calendarState.animateScrollToMonth(YearMonth.from(today))
                        }
                    }) {
                        Icon(
                            Icons.Default.Today,
                            contentDescription = "Today",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))

            // ── Calendar Body (Month vs Week) ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                if (viewMode == CalendarViewMode.MONTH) {
                    HorizontalCalendar(
                        state = calendarState,
                        dayContent = { day ->
                            DayContent(
                                day = day,
                                isSelected = day.date == selectedLocalDate,
                                markers = markers[day.date] ?: DayMarkers(),
                                onClick = {
                                    val millis =
                                        day.date.atStartOfDay(ZoneId.systemDefault()).toInstant()
                                            .toEpochMilli()
                                    onSelectDate(millis)
                                }
                            )
                        },
                        monthHeader = {
                            DaysOfWeekHeader(firstDayOfWeek = firstDayOfWeek)
                        }
                    )
                } else {
                    WeekCalendar(
                        state = weekCalendarState,
                        dayContent = { day ->
                            WeekDayContent(
                                day = day,
                                isSelected = day.date == selectedLocalDate,
                                markers = markers[day.date] ?: DayMarkers(),
                                onClick = {
                                    val millis =
                                        day.date.atStartOfDay(ZoneId.systemDefault()).toInstant()
                                            .toEpochMilli()
                                    onSelectDate(millis)
                                }
                            )
                        },
                        weekHeader = {
                            DaysOfWeekHeader(firstDayOfWeek = firstDayOfWeek)
                        }
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(top = 8.dp, bottom = 12.dp))

            // ── Selected Date Agenda Header ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = selectedDateText,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                SuggestionChip(
                    onClick = {},
                    label = {
                        val parts = listOfNotNull(
                            "${todosForDate.size} Tasks".takeIf { todosForDate.isNotEmpty() },
                            "${notesForDate.size} Notes".takeIf { notesForDate.isNotEmpty() },
                            "${moodsForDate.size} Moods".takeIf { moodsForDate.isNotEmpty() }
                        )
                        Text(parts.joinToString(" • ").ifBlank { "Nothing scheduled" })
                    },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    )
                )
            }

            // ── Agenda List ──
            if (todosForDate.isEmpty() && notesForDate.isEmpty() && moodsForDate.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No scheduled tasks, notes, or moods for this date.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(todosForDate, key = { "task-${it.id}" }) { todo ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { onToggleTodo(todo) }) {
                                    Icon(
                                        imageVector = if (todo.isCompleted) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                        contentDescription = "Toggle Task",
                                        tint = if (todo.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = todo.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else null
                                    )
                                    if (!todo.description.isNullOrBlank()) {
                                        Text(
                                            text = todo.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                    items(notesForDate, key = { "note-${it.id}" }) { note ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clickable(enabled = onNoteClick != null) {
                                    onNoteClick?.invoke(note.id)
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(
                                    alpha = 0.4f
                                )
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = note.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = note.content.take(120).replace("\n", " "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                    items(moodsForDate, key = { "mood-${it.id}" }) { mood ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clickable(enabled = onMoodClick != null) {
                                    onMoodClick?.invoke()
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = mood.emotion.color.copy(alpha = 0.14f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = mood.emotion.color.copy(alpha = 0.2f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = mood.emotion.icon,
                                            contentDescription = mood.emotion.label,
                                            tint = mood.emotion.color,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = mood.emotion.label,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (mood.note.isNotBlank()) {
                                        Text(
                                            text = mood.note,
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

@Composable
private fun DaysOfWeekHeader(firstDayOfWeek: DayOfWeek) {
    val daysOfWeek = remember(firstDayOfWeek) {
        val days = DayOfWeek.entries.toTypedArray()
        val pivot = days.indexOf(firstDayOfWeek)
        days.copyOfRange(pivot, days.size) + days.copyOfRange(0, pivot)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        for (dayOfWeek in daysOfWeek) {
            Text(
                modifier = Modifier.weight(1f),
                text = dayOfWeek.getDisplayName(TextStyle.SHORT, AppConstants.APP_LOCALE),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun DayContent(
    day: CalendarDay,
    isSelected: Boolean,
    markers: DayMarkers,
    onClick: () -> Unit
) {
    val isToday = day.date == LocalDate.now()
    val isCurrentMonth = day.position == DayPosition.MonthDate

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    isToday -> MaterialTheme.colorScheme.primaryContainer
                    else -> Color.Transparent
                }
            )
            .clickable(enabled = isCurrentMonth, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = day.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    !isCurrentMonth -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onSurface
                },
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
            )
            if (markers.types.isNotEmpty() && isCurrentMonth) {
                Spacer(Modifier.height(2.dp))
                EventDots(markers = markers, isSelected = isSelected)
            }
        }
    }
}

@Composable
private fun WeekDayContent(
    day: WeekDay,
    isSelected: Boolean,
    markers: DayMarkers,
    onClick: () -> Unit
) {
    val isToday = day.date == LocalDate.now()

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    isToday -> MaterialTheme.colorScheme.primaryContainer
                    else -> Color.Transparent
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = day.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onSurface
                },
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
            )
            if (markers.types.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                EventDots(markers = markers, isSelected = isSelected)
            }
        }
    }
}

@Composable
private fun EventDots(
    markers: DayMarkers,
    isSelected: Boolean
) {
    // Fixed priority order: task → note → mood (matches DayMarkerType order).
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (DayMarkerType.TASK in markers.types) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
            )
        }
        if (DayMarkerType.NOTE in markers.types) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.tertiary)
            )
        }
        markers.moodEmotion?.let { emotion ->
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.onPrimary else emotion.color
                    )
            )
        }
    }
}

// ── FULL CASE-BY-CASE PREVIEWS ──

@Preview(
    showBackground = true,
    name = "1. Calendar Screen - Month Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun CalendarScreenPreview_Dark() {
    val sampleDate = System.currentTimeMillis()
    val sampleTodos = listOf(
        TaskItem(
            "1",
            "Architecture Sync Meeting",
            "Discuss Diary redesign",
            sampleDate,
            null,
            false
        ),
        TaskItem("2", "Review PR #42", "Check unit test coverage", sampleDate, null, true)
    )

    AppTheme(darkTheme = true) {
        CalendarScreenContent(
            year = 2026,
            month = 2,
            selectedDateMillis = sampleDate,
            todosForDate = sampleTodos
        )
    }
}

@Preview(
    showBackground = true,
    name = "2. Calendar Screen - Month Light",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
fun CalendarScreenPreview_Light() {
    val sampleDate = System.currentTimeMillis()
    val sampleTodos = listOf(
        TaskItem(
            "1",
            "Architecture Sync Meeting",
            "Discuss Diary redesign",
            sampleDate,
            null,
            false
        )
    )

    AppTheme(darkTheme = false) {
        CalendarScreenContent(
            year = 2026,
            month = 2,
            selectedDateMillis = sampleDate,
            todosForDate = sampleTodos
        )
    }
}

@Preview(
    showBackground = true,
    name = "3. Calendar Screen - Empty Day Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun CalendarScreenPreview_EmptyDay_Dark() {
    AppTheme(darkTheme = true) {
        CalendarScreenContent(
            year = 2026,
            month = 2,
            selectedDateMillis = System.currentTimeMillis(),
            todosForDate = emptyList(),
            notesForDate = emptyList()
        )
    }
}

@Preview(
    showBackground = true,
    name = "4. Calendar Screen - Empty Day Light",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
fun CalendarScreenPreview_EmptyDay_Light() {
    AppTheme(darkTheme = false) {
        CalendarScreenContent(
            year = 2026,
            month = 2,
            selectedDateMillis = System.currentTimeMillis(),
            todosForDate = emptyList(),
            notesForDate = emptyList()
        )
    }
}

@Preview(
    showBackground = true,
    name = "5. Calendar Screen - Tasks and Note Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun CalendarScreenPreview_TasksAndNote_Dark() {
    val sampleDate = System.currentTimeMillis()
    val sampleTodos = listOf(
        TaskItem(
            "1",
            "Architecture Sync Meeting",
            "Discuss Diary redesign",
            sampleDate,
            null,
            false
        ),
        TaskItem("2", "Review PR #42", "Check unit test coverage", sampleDate, null, true)
    )
    val sampleNotes = listOf(
        com.kotonosora.todolist.domain.model.NoteItem(
            "Daily/2026-03-15.md",
            "Daily Note",
            "Daily",
            "Shipped the calendar agenda and reviewed open tasks."
        )
    )

    AppTheme(darkTheme = true) {
        CalendarScreenContent(
            year = 2026,
            month = 2,
            selectedDateMillis = sampleDate,
            todosForDate = sampleTodos,
            notesForDate = sampleNotes
        )
    }
}

@Preview(
    showBackground = true,
    name = "6. Calendar Screen - Tasks and Note Light",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
fun CalendarScreenPreview_TasksAndNote_Light() {
    val sampleDate = System.currentTimeMillis()
    val sampleTodos = listOf(
        TaskItem(
            "1",
            "Architecture Sync Meeting",
            "Discuss Diary redesign",
            sampleDate,
            null,
            false
        ),
        TaskItem("2", "Review PR #42", "Check unit test coverage", sampleDate, null, true)
    )
    val sampleNotes = listOf(
        com.kotonosora.todolist.domain.model.NoteItem(
            "Daily/2026-03-15.md",
            "Daily Note",
            "Daily",
            "Shipped the calendar agenda and reviewed open tasks."
        )
    )

    AppTheme(darkTheme = false) {
        CalendarScreenContent(
            year = 2026,
            month = 2,
            selectedDateMillis = sampleDate,
            todosForDate = sampleTodos,
            notesForDate = sampleNotes
        )
    }
}

@Preview(
    showBackground = true,
    name = "7. Calendar Screen - With Moods Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun CalendarScreenPreview_WithMoods_Dark() {
    val sampleDate = System.currentTimeMillis()
    val zone = ZoneId.systemDefault()
    val today = Instant.ofEpochMilli(sampleDate).atZone(zone).toLocalDate()
    val sampleTodos = listOf(
        TaskItem(
            "1",
            "Architecture Sync Meeting",
            "Discuss Diary redesign",
            sampleDate,
            null,
            false
        )
    )
    val sampleMoods = listOf(
        MoodEntry(
            id = "m1",
            emotion = com.kotonosora.todolist.domain.model.EmotionStamp.HAPPY,
            note = "Shipped the release",
            createdAt = sampleDate
        ),
        MoodEntry(
            id = "m2",
            emotion = com.kotonosora.todolist.domain.model.EmotionStamp.CALM,
            createdAt = sampleDate
        )
    )
    AppTheme(darkTheme = true) {
        CalendarScreenContent(
            year = today.year,
            month = today.monthValue - 1,
            selectedDateMillis = sampleDate,
            todosForDate = sampleTodos,
            moodsForDate = sampleMoods,
            markers = mapOf(
                today to DayMarkers(
                    types = setOf(DayMarkerType.TASK, DayMarkerType.MOOD),
                    moodEmotion = com.kotonosora.todolist.domain.model.EmotionStamp.HAPPY
                )
            )
        )
    }
}

@Preview(
    showBackground = true,
    name = "8. Calendar Screen - With Moods Light",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
fun CalendarScreenPreview_WithMoods_Light() {
    val sampleDate = System.currentTimeMillis()
    val zone = ZoneId.systemDefault()
    val today = Instant.ofEpochMilli(sampleDate).atZone(zone).toLocalDate()
    val sampleMoods = listOf(
        MoodEntry(
            id = "m1",
            emotion = com.kotonosora.todolist.domain.model.EmotionStamp.STRESSED,
            note = "Tough retro",
            createdAt = sampleDate
        )
    )
    AppTheme(darkTheme = false) {
        CalendarScreenContent(
            year = today.year,
            month = today.monthValue - 1,
            selectedDateMillis = sampleDate,
            todosForDate = emptyList(),
            moodsForDate = sampleMoods,
            markers = mapOf(
                today to DayMarkers(
                    types = setOf(DayMarkerType.MOOD),
                    moodEmotion = com.kotonosora.todolist.domain.model.EmotionStamp.STRESSED
                )
            )
        )
    }
}
