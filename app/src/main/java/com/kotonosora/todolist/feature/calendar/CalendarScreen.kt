package com.kotonosora.todolist.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kotonosora.todolist.domain.model.TodoItem
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: CalendarViewModel = viewModel()) {
    val year by viewModel.currentYear.collectAsState()
    val month by viewModel.currentMonth.collectAsState()
    val selectedDateMillis by viewModel.selectedDateMillis.collectAsState()
    val allTodos by viewModel.allTodos.collectAsState()
    val todosForDate by viewModel.todosForSelectedDate.collectAsState()

    val monthNames = remember {
        listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Calendar") }) }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 8.dp)
        ) {
            // Month navigation header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { viewModel.previousMonth() }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous month")
                }
                Text(
                    "${monthNames[month]} $year",
                    style = MaterialTheme.typography.titleLarge
                )
                IconButton(onClick = { viewModel.nextMonth() }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next month")
                }
            }

            // Day of week headers
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("Su","Mo","Tu","We","Th","Fr","Sa").forEach { label ->
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

            // Calendar grid
            CalendarGrid(
                year = year,
                month = month,
                selectedDateMillis = selectedDateMillis,
                allTodos = allTodos,
                onDateSelected = { viewModel.selectDate(it) }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Todos for selected date
            val formatter = remember { SimpleDateFormat("EEEE, MMM d", Locale.getDefault()) }
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
    onDateSelected: (Long) -> Unit
) {
    val cal = Calendar.getInstance().apply { set(year, month, 1) }
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0 = Sunday

    val selectedCal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
    val selectedDay = if (selectedCal.get(Calendar.YEAR) == year && selectedCal.get(Calendar.MONTH) == month)
        selectedCal.get(Calendar.DAY_OF_MONTH) else -1

    val todoDays = remember(allTodos, year, month) {
        allTodos.mapNotNull { todo ->
            todo.dueDate?.let {
                val c = Calendar.getInstance().apply { timeInMillis = it }
                if (c.get(Calendar.YEAR) == year && c.get(Calendar.MONTH) == month)
                    c.get(Calendar.DAY_OF_MONTH) else null
            }
        }.toSet()
    }

    val cells = firstDayOfWeek + daysInMonth
    val rows = (cells + 6) / 7

    Column {
        repeat(rows) { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) { col ->
                    val cellIndex = row * 7 + col
                    val day = cellIndex - firstDayOfWeek + 1
                    if (day < 1 || day > daysInMonth) {
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val isSelected = day == selectedDay
                        val hasTodo = day in todoDays
                        val today = Calendar.getInstance()
                        val isToday = today.get(Calendar.YEAR) == year &&
                                today.get(Calendar.MONTH) == month &&
                                today.get(Calendar.DAY_OF_MONTH) == day

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        isToday -> MaterialTheme.colorScheme.primaryContainer
                                        else -> MaterialTheme.colorScheme.surface
                                    }
                                )
                                .clickable {
                                    val c = Calendar.getInstance().apply {
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
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurface
                                )
                                if (hasTodo) {
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                else MaterialTheme.colorScheme.tertiary
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
    }
}

