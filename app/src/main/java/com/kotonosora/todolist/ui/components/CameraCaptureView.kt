package com.kotonosora.todolist.ui.components

import android.content.res.Configuration
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kotonosora.todolist.data.file.MediaFileManager
import com.kotonosora.todolist.data.file.MediaOutputLocation
import com.kotonosora.todolist.ui.theme.AppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.TimeUnit
import androidx.compose.ui.tooling.preview.Preview as ComposePreview
import com.kotonosora.todolist.common.AppConstants

enum class CaptureMode { PHOTO, VIDEO }

/**
 * CameraX Photo + Video Capture View featuring:
 * 1. Tap-to-Focus & Auto-Exposure Metering
 * 2. Flash Mode Controls (Off / Auto / On, photo mode)
 * 3. Front / Back Lens Switching
 * 4. Shutter Pulse Flash Animation (photo)
 * 5. HD Video Recording with audio (video mode)
 *
 * Captures are written into the chosen vault folder (SAF tree or local vault),
 * inside per-type subfolders (`photos/` / `videos/`) via [MediaFileManager].
 * CameraX cannot write directly to a SAF `DocumentFile` uri, so SAF captures
 * go to a `cacheDir` temp file first, then copy to the vault (see photo +
 * video finalize paths below).
 */
@Composable
fun CameraCaptureView(
    onPhotoCaptured: (String) -> Unit,
    onDismiss: () -> Unit,
    customFolderUriStr: String? = null,
    modifier: Modifier = Modifier,
    onVideoCaptured: (String) -> Unit = {},
    onCaptureError: (String) -> Unit = {}
) {
    if (LocalInspectionMode.current) {
        // Preview placeholder: CameraX needs a real lifecycle + camera hardware.
        CameraCaptureContent(onDismiss = onDismiss)
        return
    }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_OFF) }
    var captureMode by remember { mutableStateOf(CaptureMode.PHOTO) }
    var activeCamera by remember { mutableStateOf<Camera?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var videoElapsedSeconds by remember { mutableIntStateOf(0) }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }

    // Pin the latest recording for dispose: DisposableEffect(Unit) would
    // capture the initial null, so observe the state holder instead.

    // Tap-to-focus indicator position & animation
    var focusOffset by remember { mutableStateOf<Offset?>(null) }
    val focusRingAlpha = remember { Animatable(0f) }

    // Shutter flash feedback animation
    val shutterFlashAlpha = remember { Animatable(0f) }

    val previewView = remember { PreviewView(context) }
    val imageCapture = remember(flashMode) {
        ImageCapture.Builder()
            .setFlashMode(flashMode)
            .build()
    }
    val videoCapture = remember {
        VideoCapture.withOutput(
            Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HD))
                .build()
        )
    }

    LaunchedEffect(lensFacing, flashMode, captureMode) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            try {
                cameraProvider.unbindAll()
                activeCamera = if (captureMode == CaptureMode.PHOTO) {
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                } else {
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        videoCapture
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    DisposableEffect(activeRecording) {
        onDispose {
            try {
                activeRecording?.stop()
            } catch (_: Exception) {
            }
        }
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            videoElapsedSeconds = 0
            while (isRecording) {
                delay(1000)
                videoElapsedSeconds += 1
            }
        }
    }

    fun takePhoto() {
        coroutineScope.launch {
            shutterFlashAlpha.snapTo(0.8f)
            shutterFlashAlpha.animateTo(0f, animationSpec = tween(200))
        }

        val mediaFileManager = MediaFileManager(context)
        val location = mediaFileManager.createPhotoOutputLocation(customFolderUriStr)

        val targetFile = when (location) {
            is MediaOutputLocation.DocumentFileUri -> File(
                context.cacheDir,
                "TEMP_${System.currentTimeMillis()}.jpg"
            )

            is MediaOutputLocation.LocalFile -> location.file
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(targetFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val pathStr = when (location) {
                        is MediaOutputLocation.DocumentFileUri -> {
                            try {
                                context.contentResolver.openOutputStream(
                                    location.uri,
                                    "w"
                                )?.use { outStream ->
                                    targetFile.inputStream().use { inStream ->
                                        inStream.copyTo(outStream)
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            } finally {
                                targetFile.delete()
                            }
                            location.pathString
                        }

                        is MediaOutputLocation.LocalFile -> location.file.absolutePath
                    }
                    onPhotoCaptured(pathStr)
                }

                override fun onError(exception: ImageCaptureException) {
                    exception.printStackTrace()
                    onCaptureError("Couldn't take this photo (${exception.message}).")
                }
            }
        )
    }

    fun startVideoRecording() {
        val mediaFileManager = MediaFileManager(context)
        val location = mediaFileManager.createVideoOutputLocation(customFolderUriStr)
        val targetFile = when (location) {
            is MediaOutputLocation.DocumentFileUri -> File(
                context.cacheDir,
                "TEMP_${System.currentTimeMillis()}.mp4"
            )

            is MediaOutputLocation.LocalFile -> location.file.also {
                it.parentFile?.mkdirs()
            }
        }
        val outputOptions = FileOutputOptions.Builder(targetFile).build()
        try {
            val recording = videoCapture.output
                .prepareRecording(context, outputOptions)
                .withAudioEnabled()
                .start(
                    ContextCompat.getMainExecutor(context)
                ) { event ->
                    when (event) {
                        is VideoRecordEvent.Start -> {
                            isRecording = true
                        }

                        is VideoRecordEvent.Finalize -> {
                            isRecording = false
                            activeRecording = null
                            if (!event.hasError()) {
                                val pathStr = when (location) {
                                    is MediaOutputLocation.DocumentFileUri -> {
                                        try {
                                            context.contentResolver.openOutputStream(
                                                location.uri,
                                                "w"
                                            )?.use { outStream ->
                                                targetFile.inputStream().use { inStream ->
                                                    inStream.copyTo(outStream)
                                                }
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        } finally {
                                            targetFile.delete()
                                        }
                                        location.pathString
                                    }

                                    is MediaOutputLocation.LocalFile -> location.file.absolutePath
                                }
                                onVideoCaptured(pathStr)
                            } else {
                                try {
                                    targetFile.delete()
                                } catch (_: Exception) {
                                }
                                onCaptureError(
                                    "Couldn't save this video (${event.error})."
                                )
                            }
                        }
                    }
                }
            activeRecording = recording
        } catch (e: SecurityException) {
            e.printStackTrace()
            onCaptureError("Microphone permission is needed for video with audio.")
        } catch (e: Exception) {
            e.printStackTrace()
            onCaptureError("Couldn't start recording (${e.message}).")
        }
    }

    fun stopVideoRecording() {
        try {
            activeRecording?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
            isRecording = false
            activeRecording = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures { tapOffset ->
                    focusOffset = tapOffset
                    val meteringPointFactory = previewView.meteringPointFactory
                    val point = meteringPointFactory.createPoint(tapOffset.x, tapOffset.y)
                    val action = FocusMeteringAction.Builder(
                        point,
                        FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
                    )
                        .setAutoCancelDuration(3, TimeUnit.SECONDS)
                        .build()

                    activeCamera?.cameraControl?.startFocusAndMetering(action)

                    coroutineScope.launch {
                        focusRingAlpha.snapTo(1f)
                        focusRingAlpha.animateTo(0f, animationSpec = tween(1200))
                    }
                }
            }
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Shutter Flash Feedback
        if (shutterFlashAlpha.value > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(shutterFlashAlpha.value)
                    .background(Color.White)
            )
        }

        // Tap-to-Focus Ring Indicator
        focusOffset?.let { offset ->
            if (focusRingAlpha.value > 0f) {
                Box(
                    modifier = Modifier
                        .offset { IntOffset(offset.x.toInt() - 32, offset.y.toInt() - 32) }
                        .size(64.dp)
                        .alpha(focusRingAlpha.value)
                        .border(2.dp, Color.Yellow, CircleShape)
                )
            }
        }

        // Top Action Header (Close & Flash Toggle)
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close Camera", tint = Color.White)
            }

            if (isRecording) {
                Text(
                    text = formatElapsed(videoElapsedSeconds),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .background(Color.Red.copy(alpha = 0.7f), CircleShape)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )
            } else if (captureMode == CaptureMode.PHOTO) {
                // Flash Mode Button (Off -> Auto -> On)
                IconButton(
                    onClick = {
                        flashMode = when (flashMode) {
                            ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_AUTO
                            ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
                            else -> ImageCapture.FLASH_MODE_OFF
                        }
                    }
                ) {
                    val (flashIcon, flashDesc) = when (flashMode) {
                        ImageCapture.FLASH_MODE_AUTO -> Icons.Default.FlashAuto to "Flash Auto"
                        ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn to "Flash On"
                        else -> Icons.Default.FlashOff to "Flash Off"
                    }
                    Icon(flashIcon, contentDescription = flashDesc, tint = Color.White)
                }
            }
        }

        // Bottom controls: mode toggle + lens flip + shutter/record
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isRecording) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { captureMode = CaptureMode.PHOTO },
                        enabled = captureMode != CaptureMode.PHOTO
                    ) {
                        Text(
                            "Photo",
                            color = if (captureMode == CaptureMode.PHOTO) Color.Yellow else Color.White
                        )
                    }
                    TextButton(
                        onClick = { captureMode = CaptureMode.VIDEO },
                        enabled = captureMode != CaptureMode.VIDEO
                    ) {
                        Text(
                            "Video",
                            color = if (captureMode == CaptureMode.VIDEO) Color.Yellow else Color.White
                        )
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Flip lens (disabled while recording)
                IconButton(
                    onClick = {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                            CameraSelector.LENS_FACING_FRONT
                        } else {
                            CameraSelector.LENS_FACING_BACK
                        }
                    },
                    enabled = !isRecording,
                    modifier = Modifier.padding(end = 24.dp)
                ) {
                    Icon(
                        Icons.Default.FlipCameraAndroid,
                        contentDescription = "Switch Camera",
                        tint = if (isRecording) Color.Gray else Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                if (captureMode == CaptureMode.PHOTO) {
                    FloatingActionButton(
                        onClick = { takePhoto() },
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Icon(
                            Icons.Default.Camera,
                            contentDescription = "Take Photo",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                } else {
                    FloatingActionButton(
                        onClick = {
                            if (isRecording) stopVideoRecording() else startVideoRecording()
                        },
                        shape = CircleShape,
                        containerColor = if (isRecording) Color.Red else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Icon(
                            if (isRecording) Icons.Default.Stop else Icons.Default.Videocam,
                            contentDescription = if (isRecording) "Stop Recording" else "Record Video",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
            if (isRecording) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Icon(
                        Icons.Default.FiberManualRecord,
                        contentDescription = null,
                        tint = Color.Red,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        "  Recording… tap stop when done",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

private fun formatElapsed(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format(AppConstants.APP_LOCALE, "● %02d:%02d", mins, secs)
}

/**
 * Static camera chrome extracted for @Preview (CameraX needs hardware,
 * so previews render this placeholder instead of binding a real preview).
 */
@Composable
fun CameraCaptureContent(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close Camera", tint = Color.White)
            }
            IconButton(onClick = {}) {
                Icon(Icons.Default.FlashOff, contentDescription = "Flash Off", tint = Color.White)
            }
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {}, modifier = Modifier.padding(end = 24.dp)) {
                Icon(
                    Icons.Default.FlipCameraAndroid,
                    contentDescription = "Switch Camera",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            FloatingActionButton(
                onClick = {},
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    Icons.Default.Camera,
                    contentDescription = "Take Photo",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}

@ComposePreview(
    showBackground = true,
    name = "1. Camera Capture - Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun CameraCaptureViewPreview_Dark() {
    AppTheme(darkTheme = true) {
        CameraCaptureContent(onDismiss = {})
    }
}

@ComposePreview(
    showBackground = true,
    name = "2. Camera Capture - Light",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
fun CameraCaptureViewPreview_Light() {
    AppTheme(darkTheme = false) {
        CameraCaptureContent(onDismiss = {})
    }
}
