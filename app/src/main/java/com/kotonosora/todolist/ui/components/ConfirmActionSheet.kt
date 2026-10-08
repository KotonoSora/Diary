package com.kotonosora.todolist.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kotonosora.todolist.ui.theme.AppTheme

/**
 * Shared small confirmation sheet (replaces centered AlertDialogs).
 * Destructive confirms render the primary action in the error color.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmActionSheet(
    title: String,
    message: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    isDestructive: Boolean = true
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        ConfirmActionSheetContent(
            title = title,
            message = message,
            confirmLabel = confirmLabel,
            onDismiss = onDismiss,
            onConfirm = onConfirm,
            isDestructive = isDestructive
        )
    }
}

@Composable
fun ConfirmActionSheetContent(
    title: String,
    message: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    isDestructive: Boolean = true
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = if (isDestructive) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                },
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = if (isDestructive) Icons.Default.Delete else Icons.Default.Edit,
                    contentDescription = null,
                    tint = if (isDestructive) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    },
                    modifier = Modifier.padding(12.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close"
                )
            }
        }
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            ) { Text("Cancel") }
            Button(
                onClick = onConfirm,
                modifier = Modifier.weight(1f),
                colors = if (isDestructive) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                } else {
                    ButtonDefaults.buttonColors()
                }
            ) { Text(confirmLabel) }
        }
    }
}

@Preview(showBackground = true, name = "1. Confirm Sheet - Dark")
@Composable
fun ConfirmActionSheetPreview_Dark() {
    AppTheme(darkTheme = true) {
        Surface {
            ConfirmActionSheetContent(
                title = "Delete word?",
                message = "\"Serendipity\" will be removed from this deck.",
                confirmLabel = "Delete",
                onDismiss = {},
                onConfirm = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "2. Confirm Sheet - Light")
@Composable
fun ConfirmActionSheetPreview_Light() {
    AppTheme(darkTheme = false) {
        Surface {
            ConfirmActionSheetContent(
                title = "Delete word?",
                message = "\"Serendipity\" will be removed from this deck.",
                confirmLabel = "Delete",
                onDismiss = {},
                onConfirm = {}
            )
        }
    }
}
