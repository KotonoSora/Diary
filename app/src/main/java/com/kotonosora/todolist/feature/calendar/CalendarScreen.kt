package com.kotonosora.todolist.feature.calendar

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kotonosora.todolist.domain.model.TodoItem
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: CalendarViewModel = viewModel()) {
    val year by viewModel.currentYear.collectAsState()
    val month by viewModel.currentMonth.collectAsState()
    val selectedDateMillis by viewModel.selectedDateMillis.collectAsState()
    val allTodos by viewModel.allTodos.collectAsState()
    val todosForDate by viewModel.todosForSelectedDate.collectAsState()

    val locale = LocalConfiguration.current.locales[0]
    val monthTitle = remember(year, month, locale) {
        val cal = Calendar.getInstance(locale).apply { set(year, month, 1) }
        SimpleDateFormat("MMMM yyyy", locale).format(cal.time)
    }

    val weekDays = remember(locale) {
        val symbols = DateFormatSymbols(locale).shortWeekdays
        val firstDayOfWeek = Calendar.getInstance(locale).firstDayOfWeek
        val days = mutableListOf<String>()
        for (i in 0..6) {
            val dayIndex = ((firstDayOfWeek - 1 + i) % 7) + 1
            days.add(symbols[dayIndex])
        }
        days
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Calendar") }) }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 8.dp)
        ) {
            // Month navigation header (locale formatted)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { viewModel.previousMonth() }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous month")
                }
                Text(
                    text = monthTitle,
                    style = MaterialTheme.typography.titleLarge
                )
                IconButton(onClick = { viewModel.nextMonth() }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next month")
                }
            }

            // Locale-aware Day of week headers
            Row(modifier = Modifier.fillMaxWidth()) {
                weekDays.forEach { label ->
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // Calendar grid with event indicators
            CalendarGrid(
                year = year,
                month = month,
                selectedDateMillis = selectedDateMillis,
                allTodos = allTodos,
                locale = locale,
                onDateSelected = { viewModel.selectDate(it) }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Todos for selected date formatted in locale language
            val formatter = remember(locale) { SimpleDateFormat("EEEE, MMM d, yyyy", locale) }
            Text(
                text = formatter.format(Date(selectedDateMillis)),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            if (todosForDate.isEmpty()) {
                Text(
                    "No tasks for this day.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(8.dp)
                )
            } else {
                LazyColumn {
                    items(todosForDate) { todo -> CalendarTodoItem(todo) }
                }
            }
        }
    }
}

@Composable
private fun CalendarGrid(
    year: Int,
    month: Int,
    selectedDateMillis: Long,
    allTodos: List<TodoItem>,
    locale: Locale,
    onDateSelected: (Long) -> Unit
) {
    val firstDayOfWeek = Calendar.getInstance(locale).firstDayOfWeek
    val cal = Calendar.getInstance(locale).apply { set(year, month, 1) }
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOffset = ((cal.get(Calendar.DAY_OF_WEEK) - firstDayOfWeek + 7) % 7)

    val selectedCal = Calendar.getInstance(locale).apply { timeInMillis = selectedDateMillis }
    val selectedDay =
        if (selectedCal.get(Calendar.YEAR) == year && selectedCal.get(Calendar.MONTH) == month)
            selectedCal.get(Calendar.DAY_OF_MONTH) else -1

    // Map dayOfMonth -> (pendingCount, completedCount)
    val dayEventsMap = remember(allTodos, year, month) {
        val map = mutableMapOf<Int, Pair<Int, Int>>()
        allTodos.forEach { todo ->
            todo.dueDate?.let {
                val c = Calendar.getInstance().apply { timeInMillis = it }
                if (c.get(Calendar.YEAR) == year && c.get(Calendar.MONTH) == month) {
                    val day = c.get(Calendar.DAY_OF_MONTH)
                    val current = map.getOrDefault(day, Pair(0, 0))
                    if (todo.isCompleted) {
                        map[day] = Pair(current.first, current.second + 1)
                    } else {
                        map[day] = Pair(current.first + 1, current.second)
                    }
                }
            }
        }
        map
    }

    val cells = firstDayOffset + daysInMonth
    val rows = (cells + 6) / 7

    Column {
        repeat(rows) { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) { col ->
                    val cellIndex = row * 7 + col
                    val day = cellIndex - firstDayOffset + 1
                    if (day < 1 || day > daysInMonth) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                        )
                    } else {
                        val isSelected = day == selectedDay
                        val (pendingCount, completedCount) = dayEventsMap.getOrDefault(day, Pair(0, 0))
                        val totalEvents = pendingCount + completedCount
                        val today = Calendar.getInstance(locale)
                        val isToday = today.get(Calendar.YEAR) == year &&
                                today.get(Calendar.MONTH) == month &&
                                today.get(Calendar.DAY_OF_MONTH) == day

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(
                                    when {
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        isToday -> MaterialTheme.colorScheme.primaryContainer
                                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                    }
                                )
                                .clickable {
                                    val c = Calendar.getInstance(locale).apply {
                                        set(year, month, day, 0, 0, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                    onDateSelected(c.timeInMillis)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = day.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurface
                                )

                                if (totalEvents > 0) {
                                    if (totalEvents >= 2) {
                                        // Small event chip tag
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
                                                text = "$totalEvents",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                else MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    } else {
                                        // Single event status dot
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
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarTodoItem(todo: TodoItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(
                    if (todo.isCompleted) MaterialTheme.colorScheme.outline
                    else MaterialTheme.colorScheme.primary
                )
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = todo.title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (todo.isCompleted) MaterialTheme.colorScheme.outline
            else MaterialTheme.colorScheme.onSurface
        )
        if (todo.isCompleted) {
            Spacer(Modifier.width(8.dp))
            SuggestionChip(
                onClick = {},
                label = { Text("Completed", style = MaterialTheme.typography.labelSmall) },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }
}
