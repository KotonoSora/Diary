package com.kotonosora.todolist.feature.task

import android.Manifest
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.kotonosora.todolist.domain.model.TaskItem
import com.kotonosora.todolist.feature.media.formatMediaDisplayName
import com.kotonosora.todolist.ui.components.CameraCaptureView
import com.kotonosora.todolist.ui.components.FormattedTextPreview
import com.kotonosora.todolist.ui.components.TextFormatToolbar
import com.kotonosora.todolist.ui.theme.TodoListTheme
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.OutlinedRichTextEditor
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
            if (existingTask == null) viewModel.addTask(task) else viewModel.updateTask(task)
            navController.popBackStack()
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

    var fileFormat by remember(existingTask) { mutableStateOf(existingTask?.fileFormat ?: "md") }
    var isPreviewMode by remember { mutableStateOf(false) }

    var dueDate by remember(existingTask) { mutableStateOf(existingTask?.dueDate) }
    var remindMe by remember(existingTask) { mutableStateOf(existingTask?.reminderTime != null) }
    var mediaPath by remember(existingTask) { mutableStateOf(existingTask?.filePath) }
    var showDatePickerSheet by remember { mutableStateOf(false) }
    var showCameraSheet by remember { mutableStateOf(false) }
    var titleError by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    val text = stream.bufferedReader().readText()
                    richTextState.setMarkdown(text)
                    if (title.isBlank()) {
                        val fileName = File(it.path ?: "").name
                        title = fileName.substringBeforeLast(".")
                    }
                    if (it.path?.endsWith(".txt", ignoreCase = true) == true) {
                        fileFormat = "txt"
                    } else if (it.path?.endsWith(".md", ignoreCase = true) == true) {
                        fileFormat = "md"
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
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

    val dateFormatter = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // ── Top Frameless Action Header Row ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Mode Toggle (Edit / Preview)
                    IconButton(onClick = { isPreviewMode = !isPreviewMode }) {
                        Icon(
                            imageVector = if (isPreviewMode) Icons.Default.Edit else Icons.Default.Preview,
                            contentDescription = if (isPreviewMode) "Edit Mode" else "Preview Mode",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Import Text File
                    IconButton(onClick = { documentPickerLauncher.launch(arrayOf("text/*", "*/*")) }) {
                        Icon(Icons.Default.FileOpen, contentDescription = "Import file", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    // Done / Save Checkmark
                    IconButton(
                        onClick = {
                            if (title.isBlank()) {
                                titleError = true
                                return@IconButton
                            }
                            val reminderTime = if (remindMe && dueDate != null) dueDate!! - 3_600_000L else null
                            val task = TaskItem(
                                id = existingTask?.id ?: UUID.randomUUID().toString(),
                                title = title.trim(),
                                description = richTextState.toMarkdown().trim().ifBlank { null },
                                dueDate = dueDate,
                                filePath = mediaPath,
                                isCompleted = existingTask?.isCompleted ?: false,
                                reminderTime = reminderTime,
                                fileFormat = fileFormat
                            )
                            onSaveTask(task)
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save Task")
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))

            // ── Inline Frameless Task Title Input ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                if (title.isEmpty()) {
                    Text(
                        text = "Task title…",
                        style = MaterialTheme.typography.headlineSmall,
                        color = if (titleError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
                BasicTextField(
                    value = title,
                    onValueChange = { title = it; titleError = false },
                    textStyle = MaterialTheme.typography.headlineSmall.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── Expanded Content / WYSIWYG Editor ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (!isPreviewMode) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (fileFormat == "md") {
                            TextFormatToolbar(
                                state = richTextState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            )
                        }

                        OutlinedRichTextEditor(
                            state = richTextState,
                            label = { Text("Task details & notes…") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                    }
                } else {
                    FormattedTextPreview(
                        text = richTextState.toMarkdown(),
                        format = fileFormat,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Docked Flat Accessory Bar ──
            HorizontalDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Due Date & Reminders
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Due Date Chip / Action
                    FilterChip(
                        selected = dueDate != null,
                        onClick = { showDatePickerSheet = true },
                        label = {
                            Text(dueDate?.let { dateFormatter.format(Date(it)) } ?: "Due Date")
                        },
                        leadingIcon = {
                            Icon(Icons.Default.DateRange, contentDescription = "Pick Due Date", modifier = Modifier.size(16.dp))
                        }
                    )

                    // Reminder Toggle Action
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

                    // Camera Photo Attachment Action
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

                // File Format Selector Chip
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(
                        selected = fileFormat == "md",
                        onClick = { fileFormat = "md" },
                        label = { Text(".md") }
                    )
                    FilterChip(
                        selected = fileFormat == "txt",
                        onClick = { fileFormat = "txt" },
                        label = { Text(".txt") }
                    )
                }
            }

            // Thumbnail Preview if photo attached
            if (mediaPath != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Card(
                        shape = MaterialTheme.shapes.small,
                        elevation = CardDefaults.cardElevation(0.dp),
                        modifier = Modifier
                            .size(72.dp, 48.dp)
                            .clickable { mediaPath = null }
                    ) {
                        val imageModel = remember(mediaPath) {
                            if (mediaPath == null) null
                            else if (mediaPath!!.startsWith("content://")) Uri.parse(mediaPath!!)
                            else File(mediaPath!!)
                        }
                        AsyncImage(
                            model = imageModel,
                            contentDescription = "Photo Attachment",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = formatMediaDisplayName(mediaPath!!, isAudio = false),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
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
            Box(modifier = Modifier.fillMaxWidth().height(480.dp)) {
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

@Preview(showBackground = true, name = "1. Add/Edit Task - New (Dark)", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AddEditTaskScreenPreview_New_Dark() {
    TodoListTheme(darkTheme = true) {
        AddEditTaskContent(
            existingTask = null
        )
    }
}

@Preview(showBackground = true, name = "2. Add/Edit Task - Existing Task (Dark)", uiMode = Configuration.UI_MODE_NIGHT_YES)
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

@Preview(showBackground = true, name = "3. Add/Edit Task - Existing Task (Light)", uiMode = Configuration.UI_MODE_NIGHT_NO)
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
