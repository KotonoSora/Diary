package com.kotonosora.todolist.feature.calendar

import android.content.res.Configuration
import androidx.compose.animation.fadeIn
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.WeekCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.compose.weekcalendar.rememberWeekCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.WeekDay
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import com.kotonosora.todolist.domain.model.ActionStamp
import com.kotonosora.todolist.domain.model.EmotionStamp
import com.kotonosora.todolist.domain.model.NoteItem
import com.kotonosora.todolist.domain.model.TodoItem
import com.kotonosora.todolist.ui.theme.TodoListTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class CalendarViewMode {
    MONTH,
    WEEK
}

@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = viewModel(),
    onOpenDrawer: (() -> Unit)? = null
) {
    val year by viewModel.currentYear.collectAsState()
    val month by viewModel.currentMonth.collectAsState()
    val selectedDateMillis by viewModel.selectedDateMillis.collectAsState()
    val allTodos by viewModel.allTodos.collectAsState()
    val allNotes by viewModel.allNotes.collectAsState()
    val todosForDate by viewModel.todosForSelectedDate.collectAsState()
    val notesForDate by viewModel.notesForSelectedDate.collectAsState()

    CalendarScreenContent(
        year = year,
        month = month,
        selectedDateMillis = selectedDateMillis,
        allTodos = allTodos,
        allNotes = allNotes,
        todosForDate = todosForDate,
        notesForDate = notesForDate,
        onPreviousMonth = { viewModel.previousMonth() },
        onNextMonth = { viewModel.nextMonth() },
        onSelectToday = { viewModel.selectToday() },
        onDateSelected = { viewModel.selectDate(it) },
        onToggleTodo = { viewModel.toggleTodoStatus(it) },
        onOpenDrawer = onOpenDrawer
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreenContent(
    year: Int,
    month: Int,
    selectedDateMillis: Long,
    allTodos: List<TodoItem>,
    allNotes: List<NoteItem> = emptyList(),
    todosForDate: List<TodoItem>,
    notesForDate: List<NoteItem> = emptyList(),
    onPreviousMonth: () -> Unit = {},
    onNextMonth: () -> Unit = {},
    onSelectToday: () -> Unit = {},
    onDateSelected: (Long) -> Unit = {},
    onToggleTodo: (TodoItem) -> Unit = {},
    onOpenDrawer: (() -> Unit)? = null
) {
    val locale = LocalConfiguration.current.locales[0]
    var viewMode by remember { mutableStateOf(CalendarViewMode.MONTH) }

    val currentYearMonth = remember(year, month) {
        YearMonth.of(year, month + 1)
    }

    val startMonth = remember { currentYearMonth.minusMonths(24) }
    val endMonth = remember { currentYearMonth.plusMonths(24) }
    val firstDayOfWeek = remember(locale) { firstDayOfWeekFromLocale(locale) }

    val monthCalendarState = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = currentYearMonth,
        firstDayOfWeek = firstDayOfWeek
    )

    val selectedLocalDate = remember(selectedDateMillis) {
        Instant.ofEpochMilli(selectedDateMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    }

    val weekCalendarState = rememberWeekCalendarState(
        startDate = selectedLocalDate.minusWeeks(12),
        endDate = selectedLocalDate.plusWeeks(12),
        firstVisibleWeekDate = selectedLocalDate,
        firstDayOfWeek = firstDayOfWeek
    )

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(currentYearMonth) {
        monthCalendarState.animateScrollToMonth(currentYearMonth)
    }

    val monthTitle = remember(monthCalendarState.firstVisibleMonth.yearMonth, locale) {
        val ym = monthCalendarState.firstVisibleMonth.yearMonth
        val cal = Calendar.getInstance(locale).apply { set(ym.year, ym.monthValue - 1, 1) }
        SimpleDateFormat("MMMM yyyy", locale).format(cal.time)
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 12.dp)
        ) {
            // Month Navigation Header & Mode Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onOpenDrawer != null) {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Default.Menu, contentDescription = "Open Sidebar")
                        }
                    }
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                monthCalendarState.animateScrollToMonth(monthCalendarState.firstVisibleMonth.yearMonth.minusMonths(1))
                            }
                            onPreviousMonth()
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous month")
                    }
                }

                Text(
                    text = monthTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Today button
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.clickable {
                            onSelectToday()
                            val todayLocalDate = LocalDate.now()
                            val ym = YearMonth.of(todayLocalDate.year, todayLocalDate.monthValue)
                            coroutineScope.launch {
                                monthCalendarState.animateScrollToMonth(ym)
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Today,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Today",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(Modifier.width(4.dp))

                    // Month vs Week toggle icon
                    IconButton(
                        onClick = {
                            viewMode = if (viewMode == CalendarViewMode.MONTH) CalendarViewMode.WEEK else CalendarViewMode.MONTH
                        }
                    ) {
                        Icon(
                            imageVector = if (viewMode == CalendarViewMode.MONTH) Icons.Default.CalendarViewWeek else Icons.Default.CalendarMonth,
                            contentDescription = "Toggle Calendar View",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                monthCalendarState.animateScrollToMonth(monthCalendarState.firstVisibleMonth.yearMonth.plusMonths(1))
                            }
                            onNextMonth()
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next month")
                    }
                }
            }

            // Days of week header
            DaysOfWeekHeader(firstDayOfWeek = firstDayOfWeek, locale = locale)

            Spacer(Modifier.height(4.dp))

            when (viewMode) {
                CalendarViewMode.MONTH -> {
                    // Full Month Calendar View
                    HorizontalCalendar(
                        state = monthCalendarState,
                        dayContent = { day ->
                            CalendarDayCell(
                                day = day,
                                selectedLocalDate = selectedLocalDate,
                                allTodos = allTodos,
                                allNotes = allNotes,
                                onDateSelected = { selectedDate ->
                                    val millis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                                    onDateSelected(millis)
                                }
                            )
                        }
                    )
                }

                CalendarViewMode.WEEK -> {
                    // Compact Week Calendar View
                    WeekCalendar(
                        state = weekCalendarState,
                        dayContent = { weekDay ->
                            CalendarWeekDayCell(
                                weekDay = weekDay,
                                selectedLocalDate = selectedLocalDate,
                                allTodos = allTodos,
                                onDateSelected = { selectedDate ->
                                    val millis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                                    onDateSelected(millis)
                                }
                            )
                        }
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Selected date task summary header
            val formatter = remember(locale) { SimpleDateFormat("EEEE, MMM d, yyyy", locale) }
            val completedCount = todosForDate.count { it.isCompleted }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatter.format(Date(selectedDateMillis)),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                if (todosForDate.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "$completedCount/${todosForDate.size} done",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Stamped Notes Mood/Activity Summary Card for Selected Date
            val selectedDateEmotion = notesForDate.firstNotNullOfOrNull { it.emotion }
            val selectedDateActions = notesForDate.flatMap { it.actions }.distinct()

            if (selectedDateEmotion != null || selectedDateActions.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Stamps:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)

                        selectedDateEmotion?.let { emo ->
                            Surface(color = emo.color, shape = RoundedCornerShape(6.dp)) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(emo.icon, contentDescription = emo.label, tint = Color.White, modifier = Modifier.size(12.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(emo.label, style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        selectedDateActions.forEach { act ->
                            Surface(color = act.color, shape = RoundedCornerShape(6.dp)) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(act.icon, contentDescription = act.label, tint = Color.White, modifier = Modifier.size(12.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(act.label, style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            if (todosForDate.isEmpty()) {
                Text(
                    text = "No scheduled tasks for this day.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    items(todosForDate, key = { it.id }) { todo ->
                        CalendarTodoCard(
                            todo = todo,
                            onToggle = { onToggleTodo(todo) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DaysOfWeekHeader(firstDayOfWeek: DayOfWeek, locale: Locale) {
    val daysOfWeek = remember(firstDayOfWeek) {
        val days = DayOfWeek.entries.toTypedArray()
        val pivot = firstDayOfWeek.ordinal
        days.sliceArray(pivot until days.size) + days.sliceArray(0 until pivot)
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        daysOfWeek.forEach { dayOfWeek ->
            Text(
                text = dayOfWeek.getDisplayName(TextStyle.SHORT, locale),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: CalendarDay,
    selectedLocalDate: LocalDate,
    allTodos: List<TodoItem>,
    allNotes: List<NoteItem> = emptyList(),
    onDateSelected: (LocalDate) -> Unit
) {
    val date = day.date
    val isSelected = date == selectedLocalDate
    val isToday = date == LocalDate.now()
    val isCurrentMonth = day.position == DayPosition.MonthDate

    // Emotion Vector Badge for this date
    val dayEmotion = remember(allNotes, date) {
        allNotes.firstOrNull { note ->
            val noteDate = Instant.ofEpochMilli(note.updatedAt).atZone(ZoneId.systemDefault()).toLocalDate()
            noteDate == date && note.emotion != null
        }?.emotion
    }

    val (pendingCount, completedCount) = remember(allTodos, date) {
        var pending = 0
        var completed = 0
        allTodos.forEach { todo ->
            todo.dueDate?.let { dueDateMillis ->
                val todoDate = Instant.ofEpochMilli(dueDateMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                if (todoDate == date) {
                    if (todo.isCompleted) completed++ else pending++
                }
            }
        }
        Pair(pending, completed)
    }
    val totalEvents = pendingCount + completedCount

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(MaterialTheme.shapes.small)
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    isToday -> MaterialTheme.colorScheme.primaryContainer
                    isCurrentMonth -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                    else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.1f)
                }
            )
            .clickable { onDateSelected(date) },
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = when {
                        isSelected -> MaterialTheme.colorScheme.onPrimary
                        isCurrentMonth -> MaterialTheme.colorScheme.onSurface
                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                    }
                )

                if (totalEvents > 0 && isCurrentMonth) {
                    if (totalEvents >= 2) {
                        Box(
                            modifier = Modifier
                                .padding(top = 1.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f)
                                    else MaterialTheme.colorScheme.primaryContainer
                                )
                                .padding(horizontal = 4.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = totalEvents.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.onPrimary
                                        else if (pendingCount > 0) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.secondary
                                    )
                            )
                        }
                    }
                }
            }

            // Emotion Vector Icon Badge at Top Right of Day Cell
            if (dayEmotion != null && isCurrentMonth) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Color.White else dayEmotion.color),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = dayEmotion.icon,
                        contentDescription = dayEmotion.label,
                        tint = if (isSelected) dayEmotion.color else Color.White,
                        modifier = Modifier.size(9.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarWeekDayCell(
    weekDay: WeekDay,
    selectedLocalDate: LocalDate,
    allTodos: List<TodoItem>,
    onDateSelected: (LocalDate) -> Unit
) {
    val date = weekDay.date
    val isSelected = date == selectedLocalDate
    val isToday = date == LocalDate.now()

    val (pendingCount, completedCount) = remember(allTodos, date) {
        var pending = 0
        var completed = 0
        allTodos.forEach { todo ->
            todo.dueDate?.let { dueDateMillis ->
                val todoDate = Instant.ofEpochMilli(dueDateMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                if (todoDate == date) {
                    if (todo.isCompleted) completed++ else pending++
                }
            }
        }
        Pair(pending, completed)
    }
    val totalEvents = pendingCount + completedCount

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(MaterialTheme.shapes.small)
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    isToday -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                }
            )
            .clickable { onDateSelected(date) },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )

            if (totalEvents > 0) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else if (pendingCount > 0) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.secondary
                        )
                )
            }
        }
    }
}

@Composable
private fun CalendarTodoCard(
    todo: TodoItem,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        shape = RoundedCornerShape(12.dp),
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
            IconButton(
                onClick = onToggle,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = if (todo.isCompleted) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                    contentDescription = null,
                    tint = if (todo.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = todo.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else null,
                    color = if (todo.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                if (!todo.description.isNullOrBlank()) {
                    Text(
                        text = todo.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// ── FULL CASE-BY-CASE PREVIEWS ──

@Preview(showBackground = true, name = "1. Calendar Screen - Populated Tasks & Stamps (Dark)", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun CalendarScreenPreview_Populated_Dark() {
    val sampleDate = System.currentTimeMillis()
    val sampleTodos = listOf(
        TodoItem("1", "Architecture Sync Meeting", "Discuss Diary redesign", sampleDate, null, false),
        TodoItem("2", "Review Pull Request #42", "Check unit tests and UI state", sampleDate, null, true),
        TodoItem("3", "Grocery Shopping", "Milk, Bread, Coffee", sampleDate + 86400000L, null, false)
    )

    val sampleNotes = listOf(
        NoteItem("1.md", "Diary Entry", "", "Content", emotion = EmotionStamp.HAPPY, actions = listOf(ActionStamp.WORK, ActionStamp.EXERCISE))
    )

    TodoListTheme(darkTheme = true) {
        CalendarScreenContent(
            year = 2026,
            month = 2,
            selectedDateMillis = sampleDate,
            allTodos = sampleTodos,
            allNotes = sampleNotes,
            todosForDate = sampleTodos.take(2),
            notesForDate = sampleNotes
        )
    }
}

@Preview(showBackground = true, name = "2. Calendar Screen - Populated Tasks (Light)", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun CalendarScreenPreview_Populated_Light() {
    val sampleDate = System.currentTimeMillis()
    val sampleTodos = listOf(
        TodoItem("1", "Architecture Sync Meeting", "Discuss Diary redesign", sampleDate, null, false),
        TodoItem("2", "Review Pull Request #42", "Check unit tests and UI state", sampleDate, null, true)
    )

    TodoListTheme(darkTheme = false) {
        CalendarScreenContent(
            year = 2026,
            month = 2,
            selectedDateMillis = sampleDate,
            allTodos = sampleTodos,
            todosForDate = sampleTodos
        )
    }
}

@Preview(showBackground = true, name = "3. Calendar Screen - Empty Selected Date")
@Composable
fun CalendarScreenPreview_EmptyTasks() {
    TodoListTheme(darkTheme = true) {
        CalendarScreenContent(
            year = 2026,
            month = 2,
            selectedDateMillis = System.currentTimeMillis(),
            allTodos = emptyList(),
            todosForDate = emptyList()
        )
    }
}
