package com.kotonosora.todolist.feature.todo

import android.Manifest
import android.net.Uri
import android.os.Build
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.kotonosora.todolist.domain.model.TodoItem
import com.kotonosora.todolist.feature.media.formatMediaDisplayName
import com.kotonosora.todolist.ui.components.CameraCaptureView
import com.kotonosora.todolist.ui.components.FormattedTextPreview
import com.kotonosora.todolist.ui.components.TextFormatToolbar
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.OutlinedRichTextEditor
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTodoScreen(
    navController: NavController,
    todoId: String?,
    viewModel: TodoViewModel = viewModel()
) {
    // Use unfiltered list so an active search query never hides the item being edited
    val allTodos by viewModel.allTodos.collectAsState()
    val customFolderUri by viewModel.customFolderUri.collectAsState()
    val existingTodo = remember(todoId, allTodos) {
        if (todoId == null || todoId == "new") null else allTodos.find { it.id == todoId }
    }

    var title by remember(existingTodo) { mutableStateOf(existingTodo?.title ?: "") }
    val richTextState = rememberRichTextState()

    LaunchedEffect(existingTodo) {
        richTextState.setMarkdown(existingTodo?.description ?: "")
    }

    var fileFormat by remember(existingTodo) { mutableStateOf(existingTodo?.fileFormat ?: "md") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Edit, 1: Preview

    var dueDate by remember(existingTodo) { mutableStateOf(existingTodo?.dueDate) }
    var remindMe by remember(existingTodo) { mutableStateOf(existingTodo?.reminderTime != null) }
    var mediaPath by remember(existingTodo) { mutableStateOf(existingTodo?.filePath) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showCameraView by remember { mutableStateOf(false) }
    var titleError by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // File Picker for importing local text files
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
            showCameraView = true
        }
    }

    // Request POST_NOTIFICATIONS before scheduling a reminder (Android 13+)
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* reminder toggle already flipped */ }

    val dateFormatter = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existingTodo == null) "Add Todo" else "Edit Todo") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            documentPickerLauncher.launch(arrayOf("text/*", "*/*"))
                        }
                    ) {
                        Icon(Icons.Default.FileOpen, contentDescription = "Import local text file")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // Title
            OutlinedTextField(
                value = title,
                onValueChange = { title = it; titleError = false },
                label = { Text("Title *") },
                isError = titleError,
                supportingText = if (titleError) ({ Text("Title is required") }) else null,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // File format selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "File Format:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = fileFormat == "md",
                        onClick = { fileFormat = "md" },
                        label = { Text(".md (Markdown)") }
                    )
                    FilterChip(
                        selected = fileFormat == "txt",
                        onClick = { fileFormat = "txt" },
                        label = { Text(".txt (Plain Text)") }
                    )
                }
            }

            // Edit vs Preview Tabs
            SecondaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Edit") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Preview") }
                )
            }

            if (selectedTab == 0) {
                // Edit Mode (WYSIWYG Live Rich Text Editor)
                if (fileFormat == "md") {
                    TextFormatToolbar(
                        state = richTextState,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedRichTextEditor(
                    state = richTextState,
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                // Preview Mode
                FormattedTextPreview(
                    text = richTextState.toMarkdown(),
                    format = fileFormat,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Due Date
            OutlinedTextField(
                value = dueDate?.let { dateFormatter.format(Date(it)) } ?: "Not set",
                onValueChange = {},
                readOnly = true,
                label = { Text("Due Date") },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Pick date")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            if (dueDate != null) {
                TextButton(onClick = { dueDate = null; remindMe = false }) {
                    Text("Clear date")
                }
            }

            // Reminder toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Set Reminder", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "1 hour before due date",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = remindMe,
                    onCheckedChange = { newValue ->
                        remindMe = newValue
                        if (newValue && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    enabled = dueDate != null
                )
            }

            // Media attachment with Coil AsyncImage Thumbnail Preview & Human-readable name
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Photo Attachment", style = MaterialTheme.typography.bodyLarge)
                    if (mediaPath != null) {
                        Text(
                            text = formatMediaDisplayName(mediaPath!!, isAudio = false),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(4.dp))
                        Card(
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.size(120.dp, 80.dp)
                        ) {
                            val imageModel = remember(mediaPath) {
                                if (mediaPath == null) null
                                else if (mediaPath!!.startsWith("content://")) Uri.parse(mediaPath!!)
                                else File(mediaPath!!)
                            }
                            AsyncImage(
                                model = imageModel,
                                contentDescription = "Photo Preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
                OutlinedButton(
                    onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }
                ) {
                    Text("Take Photo")
                }
            }

            if (mediaPath != null) {
                TextButton(onClick = { mediaPath = null }) {
                    Text("Remove photo")
                }
            }

            Spacer(Modifier.height(8.dp))

            // Save button
            Button(
                onClick = {
                    if (title.isBlank()) {
                        titleError = true
                        return@Button
                    }
                    val reminderTime =
                        if (remindMe && dueDate != null) dueDate!! - 3_600_000L else null
                    val todo = TodoItem(
                        id = existingTodo?.id ?: UUID.randomUUID().toString(),
                        title = title.trim(),
                        description = richTextState.toMarkdown().trim().ifBlank { null },
                        dueDate = dueDate,
                        filePath = mediaPath,
                        isCompleted = existingTodo?.isCompleted ?: false,
                        reminderTime = reminderTime,
                        fileFormat = fileFormat
                    )
                    if (existingTodo == null) viewModel.addTodo(todo) else viewModel.updateTodo(todo)
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (existingTodo == null) "Add Todo" else "Save Changes")
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    // Camera Capture Dialog
    if (showCameraView) {
        Dialog(
            onDismissRequest = { showCameraView = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            CameraCaptureView(
                onPhotoCaptured = { pathStr ->
                    mediaPath = pathStr
                    showCameraView = false
                },
                onDismiss = { showCameraView = false },
                customFolderUriStr = customFolderUri
            )
        }
    }

    // DatePickerDialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dueDate ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dueDate = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
