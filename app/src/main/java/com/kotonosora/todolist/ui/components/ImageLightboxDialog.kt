package com.kotonosora.todolist.ui.components

import android.content.res.Configuration
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.kotonosora.todolist.ui.theme.AppTheme
import java.io.File

/**
 * Full-screen Lightbox Dialog with Pinch-to-Zoom & Pan Gestures for Photos.
 */
@Composable
fun ImageLightboxDialog(
    filePath: String,
    onDismiss: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val imageModel = remember(filePath) {
        if (filePath.startsWith("content://")) Uri.parse(filePath)
        else File(filePath)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black.copy(alpha = 0.95f)
        ) {
            ImageLightboxContent(filePath = filePath, onDismiss = onDismiss)
        }
    }
}

/**
 * Dialog content extracted for @Preview (Dialog windows don't render in
 * static previews, so previews render this directly on a black surface).
 */
@Composable
fun ImageLightboxContent(
    filePath: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val imageModel = remember(filePath) {
        if (filePath.startsWith("content://")) Uri.parse(filePath)
        else File(filePath)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f))
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.8f, 5f)
                    if (scale > 1f) {
                        offsetX += pan.x
                        offsetY += pan.y
                    } else {
                        offsetX = 0f
                        offsetY = 0f
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = imageModel,
            contentDescription = "Fullscreen Photo",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                )
        )

        // Close Button Top End (inset-aware + scrimmed for contrast on any image)
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close Lightbox",
                tint = Color.White
            )
        }
    }
}

@Preview(
    showBackground = true,
    name = "1. Image Lightbox - Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun ImageLightboxDialogPreview_Dark() {
    AppTheme(darkTheme = true) {
        ImageLightboxContent(filePath = "_assets/IMG_20260301_120000.jpg", onDismiss = {})
    }
}

@Preview(
    showBackground = true,
    name = "2. Image Lightbox - Light",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
fun ImageLightboxDialogPreview_Light() {
    AppTheme(darkTheme = false) {
        ImageLightboxContent(filePath = "_assets/IMG_20260301_120000.jpg", onDismiss = {})
    }
}
