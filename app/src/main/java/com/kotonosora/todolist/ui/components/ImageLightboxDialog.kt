package com.kotonosora.todolist.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil3.compose.AsyncImage
import com.kotonosora.todolist.ui.theme.AppTheme
import java.io.File

/**
 * Full-screen Lightbox Dialog with Pinch-to-Zoom & Pan Gestures for Photos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageLightboxDialog(
    filePath: String,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = null,
        containerColor = Color.Black.copy(alpha = 0.95f)
    ) {
        ImageLightboxContent(filePath = filePath, onDismiss = onDismiss)
    }
}

/**
 * Optional immersive fullscreen for media viewers: hides status + navigation
 * bars while enabled and restores them on disable/dispose. No-op when there
 * is no Activity window (e.g. static previews).
 */
@Composable
fun ImmersiveSystemBarsEffect(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(enabled) {
        val controller = view.context.findActivity()?.window?.let { window ->
            try {
                WindowCompat.getInsetsController(window, view)
            } catch (_: Exception) {
                null
            }
        }
        if (controller != null) {
            if (enabled) {
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            try {
                view.context.findActivity()?.window?.let { window ->
                    WindowCompat.getInsetsController(window, view)
                        .show(WindowInsetsCompat.Type.systemBars())
                }
            } catch (_: Exception) {
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
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
    var isFullscreen by rememberSaveable { mutableStateOf(false) }

    ImmersiveSystemBarsEffect(enabled = isFullscreen)

    val imageModel = remember(filePath) {
        if (filePath.startsWith("content://")) Uri.parse(filePath)
        else File(filePath)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f))
            .zoomPanGestures(
                isZoomed = { scale > 1f },
                onGesture = { zoom, pan ->
                    scale = (scale * zoom).coerceIn(0.8f, 5f)
                    if (scale > 1f) {
                        offsetX += pan.x
                        offsetY += pan.y
                    } else {
                        offsetX = 0f
                        offsetY = 0f
                    }
                }
            ),
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

        // Close + optional immersive fullscreen (inset-aware + scrimmed).
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { isFullscreen = !isFullscreen },
                modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = if (isFullscreen) Icons.Default.FullscreenExit
                    else Icons.Default.Fullscreen,
                    contentDescription = if (isFullscreen) "Exit fullscreen" else "Fullscreen",
                    tint = Color.White
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .padding(start = 8.dp)
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
