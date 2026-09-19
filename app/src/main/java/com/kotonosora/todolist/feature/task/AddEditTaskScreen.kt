package com.kotonosora.todolist.feature.task

import android.Manifest
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.kotonosora.todolist.common.AppConstants
import com.kotonosora.todolist.domain.model.TaskItem
import com.kotonosora.todolist.feature.media.formatMediaDisplayName
import com.kotonosora.todolist.ui.components.CameraCaptureView
import com.kotonosora.todolist.ui.components.TextFormatToolbar
import com.kotonosora.todolist.ui.theme.TodoListTheme
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditorDefaults
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID

@Composable
fun AddEditTaskScreen(
    navController: NavController,
    taskId: String?,
    viewModel: TaskViewModel = viewModel()
) {
    val allTasks by viewModel.allTasks.collectAsState()
    val customFolderUri by viewModel.customFolderUri.collectAsState()
    val existingTask = remember(taskId, allTasks) {
        if (taskId == null || taskId == "new") null else allTasks.find { it.id == taskId }
    }

    AddEditTaskContent(
        existingTask = existingTask,
        customFolderUri = customFolderUri,
        onSaveTask = { task ->
            if (existingTask == null && allTasks.none { it.id == task.id }) {
                viewModel.addTask(task)
            } else {
                viewModel.updateTask(task)
            }
        },
        onBack = { navController.popBackStack() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTaskContent(
    existingTask: TaskItem?,
    customFolderUri: String? = null,
    onSaveTask: (TaskItem) -> Unit = {},
    onBack: () -> Unit = {}
) {
    var title by remember(existingTask) { mutableStateOf(existingTask?.title ?: "") }
    val richTextState = rememberRichTextState()

    LaunchedEffect(existingTask) {
        richTextState.setMarkdown(existingTask?.description ?: "")
    }

    var dueDate by remember(existingTask) { mutableStateOf(existingTask?.dueDate) }
    var remindMe by remember(existingTask) { mutableStateOf(existingTask?.reminderTime != null) }
    var mediaPath by remember(existingTask) { mutableStateOf(existingTask?.filePath) }
    var showDatePickerSheet by remember { mutableStateOf(false) }
    var showCameraSheet by remember { mutableStateOf(false) }

    val currentTaskId = remember(existingTask) { existingTask?.id ?: UUID.randomUUID().toString() }

    // Auto-save task attributes whenever updated
    LaunchedEffect(title, richTextState.toMarkdown(), dueDate, remindMe, mediaPath) {
        val markdownDesc = richTextState.toMarkdown().trim().ifBlank { null }
        val reminderTime = if (remindMe && dueDate != null) dueDate!! - 3_600_000L else null
        if (title.isNotBlank() || !markdownDesc.isNullOrBlank() || dueDate != null || mediaPath != null) {
            val task = TaskItem(
                id = currentTaskId,
                title = title.trim().ifBlank { "Untitled Task" },
                description = markdownDesc,
                dueDate = dueDate,
                filePath = mediaPath,
                isCompleted = existingTask?.isCompleted ?: false,
                reminderTime = reminderTime
            )
            onSaveTask(task)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showCameraSheet = true
        }
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    val dateFormatter = remember { SimpleDateFormat("EEE, MMM d", AppConstants.APP_LOCALE) }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }

                    // ── Inline Task Title in Header Bar (like Notes) ──
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    ) {
                        if (title.isEmpty()) {
                            Text(
                                text = "Task title…",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                        BasicTextField(
                            value = title,
                            onValueChange = { title = it },
                            textStyle = MaterialTheme.typography.titleMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── Compact Metadata Action Toolbar ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Due Date Chip
                FilterChip(
                    selected = dueDate != null,
                    onClick = { showDatePickerSheet = true },
                    label = {
                        Text(dueDate?.let { dateFormatter.format(Date(it)) } ?: "Due Date")
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = "Due Date",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    trailingIcon = if (dueDate != null) {
                        {
                            IconButton(
                                onClick = { dueDate = null; remindMe = false },
                                modifier = Modifier.size(16.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear Date",
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    } else null
                )

                // Reminder Toggle Button
                if (dueDate != null) {
                    IconButton(
                        onClick = {
                            remindMe = !remindMe
                            if (remindMe && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (remindMe) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                            contentDescription = "Reminder",
                            tint = if (remindMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Camera Photo Attachment Button
                IconButton(
                    onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = "Attach Photo",
                        tint = if (mediaPath != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Photo Attachment Preview Strip
            if (mediaPath != null) {
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    elevation = CardDefaults.cardElevation(0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Card(
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.size(48.dp, 36.dp)
                            ) {
                                val imageModel = remember(mediaPath) {
                                    if (mediaPath == null) null
                                    else if (mediaPath!!.startsWith("content://")) Uri.parse(
                                        mediaPath!!
                                    )
                                    else File(mediaPath!!)
                                }
                                AsyncImage(
                                    model = imageModel,
                                    contentDescription = "Photo Attachment",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = formatMediaDisplayName(mediaPath!!, isAudio = false),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = { mediaPath = null },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove photo",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))

            // ── 2. Frameless Rich Text Description Editor ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                TextFormatToolbar(
                    state = richTextState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                )

                RichTextEditor(
                    state = richTextState,
                    placeholder = { Text("Task details & notes…") },
                    colors = RichTextEditorDefaults.richTextEditorColors(
                        containerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
        }
    }

    // ── Camera Capture Bottom Sheet ──
    if (showCameraSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showCameraSheet = false },
            sheetState = sheetState
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(480.dp)
            ) {
                CameraCaptureView(
                    onPhotoCaptured = { pathStr ->
                        mediaPath = pathStr
                        showCameraSheet = false
                    },
                    onDismiss = { showCameraSheet = false },
                    customFolderUriStr = customFolderUri
                )
            }
        }
    }

    // ── Date Picker Bottom Sheet ──
    if (showDatePickerSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dueDate ?: System.currentTimeMillis()
        )
        ModalBottomSheet(
            onDismissRequest = { showDatePickerSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                DatePicker(state = datePickerState)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = {
                        dueDate = null
                        remindMe = false
                        showDatePickerSheet = false
                    }) { Text("Clear Date") }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { showDatePickerSheet = false }) { Text("Cancel") }
                    TextButton(onClick = {
                        dueDate = datePickerState.selectedDateMillis
                        showDatePickerSheet = false
                    }) { Text("OK") }
                }
            }
        }
    }
}

// ── FULL CASE-BY-CASE PREVIEWS ──

@Preview(
    showBackground = true,
    name = "1. Add/Edit Task - New (Dark)",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun AddEditTaskScreenPreview_New_Dark() {
    TodoListTheme(darkTheme = true) {
        AddEditTaskContent(
            existingTask = null
        )
    }
}

@Preview(
    showBackground = true,
    name = "2. Add/Edit Task - Existing Task (Dark)",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun AddEditTaskScreenPreview_Existing_Dark() {
    val sampleTask = TaskItem(
        id = "1",
        title = "Refactor Task Editor",
        description = "# Tasks\n- [x] Inline Title\n- [x] Flat Docked Accessory Toolbar\n- [x] Case-by-case Previews",
        dueDate = System.currentTimeMillis() + 86400000L,
        filePath = "_assets/IMG_20260301_120000.jpg",
        isCompleted = false
    )

    TodoListTheme(darkTheme = true) {
        AddEditTaskContent(
            existingTask = sampleTask
        )
    }
}

@Preview(
    showBackground = true,
    name = "3. Add/Edit Task - Existing Task (Light)",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
fun AddEditTaskScreenPreview_Existing_Light() {
    val sampleTask = TaskItem(
        id = "1",
        title = "Refactor Task Editor",
        description = "Task details in light theme",
        dueDate = System.currentTimeMillis() + 86400000L,
        filePath = null,
        isCompleted = false
    )

    TodoListTheme(darkTheme = false) {
        AddEditTaskContent(
            existingTask = sampleTask
        )
    }
}
