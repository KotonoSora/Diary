package com.kotonosora.todolist.feature.todo

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.kotonosora.todolist.domain.model.TodoItem
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTodoScreen(
    navController: NavController,
    todoId: String?,
    viewModel: TodoViewModel = viewModel()
) {
    // Use unfiltered list so an active search query never hides the item being edited
    val allTodos by viewModel.allTodos.collectAsState()
    val existingTodo = remember(todoId, allTodos) {
        if (todoId == null || todoId == "new") null else allTodos.find { it.id == todoId }
    }

    var title by remember(existingTodo) { mutableStateOf(existingTodo?.title ?: "") }
    var description by remember(existingTodo) { mutableStateOf(existingTodo?.description ?: "") }
    var dueDate by remember(existingTodo) { mutableStateOf(existingTodo?.dueDate) }
    var remindMe by remember(existingTodo) { mutableStateOf(existingTodo?.reminderTime != null) }
    var mediaPath by remember(existingTodo) { mutableStateOf(existingTodo?.filePath) }
    var showDatePicker by remember { mutableStateOf(false) }
    var titleError by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) mediaPath = photoUri?.path
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val imageFile = File(
                context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES),
                "IMG_${System.currentTimeMillis()}.jpg"
            )
            val uri = FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", imageFile
            )
            photoUri = uri
            cameraLauncher.launch(uri)
        }
    }

    // Request POST_NOTIFICATIONS before scheduling a reminder (Android 13+)
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* reminder toggle already flipped; result only affects whether notification fires */ }

    val dateFormatter = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existingTodo == null) "Add Todo" else "Edit Todo") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
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

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

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
                        // On Android 13+ request POST_NOTIFICATIONS if needed
                        if (newValue && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    enabled = dueDate != null
                )
            }

            // Media attachment
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Photo Attachment", style = MaterialTheme.typography.bodyLarge)
                    if (mediaPath != null) {
                        Text(
                            text = File(mediaPath!!).name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
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
                    val reminderTime = if (remindMe && dueDate != null) dueDate!! - 3_600_000L else null
                    val todo = TodoItem(
                        id = existingTodo?.id ?: UUID.randomUUID().toString(),
                        title = title.trim(),
                        description = description.trim().ifBlank { null },
                        dueDate = dueDate,
                        filePath = mediaPath,
                        isCompleted = existingTodo?.isCompleted ?: false,
                        reminderTime = reminderTime
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

