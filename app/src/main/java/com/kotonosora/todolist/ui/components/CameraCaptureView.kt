package com.kotonosora.todolist.ui.components

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kotonosora.todolist.common.AppConstants
import com.kotonosora.todolist.data.file.MediaFileManager
import com.kotonosora.todolist.data.file.MediaOutputLocation
import com.kotonosora.todolist.ui.theme.AppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import androidx.compose.ui.tooling.preview.Preview as ComposePreview

enum class CaptureMode { PHOTO, VIDEO }

/**
 * Chrome background for the camera sheet: dark gray instead of pure black so
 * the white overlay controls keep contrast in previews and while the camera
 * feed is still starting. (The live feed covers this once bound.)
 */
private val CameraChromeBackground = Color(0xFF242424)

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
    onCaptureError: (String) -> Unit = {},
    // Invoked when video recording needs RECORD_AUDIO but it isn't granted,
    // so the host can launch the mic permission request (the sheet itself
    // owns no permission launcher). The user taps record again after granting.
    onMicPermissionNeeded: () -> Unit = {}
) {
    if (LocalInspectionMode.current) {
        // Preview placeholder: CameraX needs a real lifecycle + camera hardware.
        CameraCaptureContent(onDismiss = onDismiss)
        return
    }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    // Application context for anything outliving the composition (camera
    // provider, PreviewView): holding the Activity across rotation would leak
    // it, and a recreated PreviewView would miss the bind below.
    val appContext = remember(context) { context.applicationContext }

    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_OFF) }
    var captureMode by remember { mutableStateOf(CaptureMode.PHOTO) }
    var activeCamera by remember { mutableStateOf<Camera?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var videoElapsedSeconds by remember { mutableIntStateOf(0) }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }
    // Held so dispose can unbind the camera. Without this the use-cases
    // stay bound to the Activity lifecycle after the sheet closes and keep
    // pushing frames to the destroyed PreviewView surface (abandoned
    // BufferQueue flood in logcat, ending in process death).
    var boundCameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    // Set on dispose; the bind listener posted to the main executor may run
    // after disposal (open + instant dismiss), so it must skip binding then
    // instead of leaking a camera bound to a dead surface.
    val cameraDisposed = remember { AtomicBoolean(false) }
    // True once the video use-case is bound and ready. Switching Photo→Video
    // rebinds asynchronously — recording before that completes always fails,
    // so the record button stays disabled until the bind lands.
    var isVideoBound by remember { mutableStateOf(false) }

    // Tap-to-focus indicator position & animation
    var focusOffset by remember { mutableStateOf<Offset?>(null) }
    val focusRingAlpha = remember { Animatable(0f) }

    // Shutter flash feedback animation
    val shutterFlashAlpha = remember { Animatable(0f) }

    val previewView = remember(appContext) { PreviewView(appContext) }
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
        // Entering video mode invalidates the previous bind; don't let a
        // record tap race the rebind below.
        if (captureMode == CaptureMode.VIDEO) isVideoBound = false
        val cameraProviderFuture = ProcessCameraProvider.getInstance(appContext)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            if (cameraDisposed.get()) {
                // Disposed before the bind ran: never bind to the dead
                // surface, just make sure nothing is left bound.
                try {
                    cameraProvider.unbindAll()
                } catch (_: Exception) {
                }
                return@addListener
            }

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
                if (captureMode == CaptureMode.VIDEO) isVideoBound = true
                boundCameraProvider = cameraProvider
            } catch (e: Exception) {
                e.printStackTrace()
                if (captureMode == CaptureMode.VIDEO) {
                    isVideoBound = false
                    onCaptureError("Couldn't start the video camera (${e.message}).")
                }
            }
        }, ContextCompat.getMainExecutor(context))
    }

    // Stop any in-flight recording and release the camera only when the view
    // leaves the composition (sheet dismissed / navigation away). Keyed on
    // Unit: a state key would re-dispose on every recording start, and
    // because `activeRecording` is a delegated state read fresh at dispose
    // time, that instantly stops the just-started recording (Finalize
    // ERROR_NO_VALID_DATA every time).
    DisposableEffect(Unit) {
        onDispose {
            cameraDisposed.set(true)
            try {
                activeRecording?.stop()
            } catch (_: Exception) {
            }
            try {
                boundCameraProvider?.unbindAll()
            } catch (_: Exception) {
            }
            boundCameraProvider = null
            activeCamera = null
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
        if (isRecording) return
        // The sheet opens on CAMERA alone, so the mic may still be denied at
        // this point. Fail open into the permission flow (host re-requests)
        // instead of the old dead-end SecurityException toast.
        val hasMicPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasMicPermission) {
            onCaptureError("Microphone permission is needed for video with audio.")
            onMicPermissionNeeded()
            return
        }
        if (!isVideoBound) {
            onCaptureError("Camera is starting — try again in a second.")
            return
        }
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
            onMicPermissionNeeded()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            onCaptureError("Camera is starting — try again in a second.")
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
            .background(CameraChromeBackground)
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
                .padding(bottom = 24.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isRecording) {
                CaptureModeToggle(
                    captureMode = captureMode,
                    onModeChange = { captureMode = it }
                )
                Text(
                    text = if (captureMode == CaptureMode.PHOTO) {
                        "Tap preview to focus • Saves to photos/"
                    } else {
                        "Needs microphone for audio • Saves to videos/"
                    },
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 6.dp)
                )
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
                        containerColor = when {
                            isRecording -> Color.Red
                            !isVideoBound -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            else -> MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier
                            .size(72.dp)
                            .alpha(if (!isRecording && !isVideoBound) 0.6f else 1f)
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
            if (!isRecording && captureMode == CaptureMode.VIDEO && !isVideoBound) {
                Text(
                    "Starting camera…",
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
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
 * Photo/Video mode toggle tuned for readability over the black camera
 * preview: the selected segment is solid white with black content, and
 * unselected segments are translucent black with white text and border —
 * the default M3 surface tones wash out on a dark viewfinder.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CaptureModeToggle(
    captureMode: CaptureMode,
    onModeChange: (CaptureMode) -> Unit,
    modifier: Modifier = Modifier
) {
    // Icons inherit the segment content color via LocalContentColor, so both
    // icon and label flip together with the selected state — no manual tints.
    val overlayColors = SegmentedButtonDefaults.colors(
        activeContainerColor = Color.White,
        activeContentColor = Color.Black,
        activeBorderColor = Color.White,
        inactiveContainerColor = Color.Black.copy(alpha = 0.45f),
        inactiveContentColor = Color.White,
        inactiveBorderColor = Color.White.copy(alpha = 0.6f)
    )
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        SegmentedButton(
            selected = captureMode == CaptureMode.PHOTO,
            onClick = { onModeChange(CaptureMode.PHOTO) },
            shape = SegmentedButtonDefaults.itemShape(0, 2),
            colors = overlayColors,
            icon = {
                SegmentedButtonDefaults.Icon(
                    active = captureMode == CaptureMode.PHOTO
                ) {
                    Icon(
                        Icons.Default.Camera,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            },
            label = {
                Text(
                    "Photo",
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        )
        SegmentedButton(
            selected = captureMode == CaptureMode.VIDEO,
            onClick = { onModeChange(CaptureMode.VIDEO) },
            shape = SegmentedButtonDefaults.itemShape(1, 2),
            colors = overlayColors,
            icon = {
                SegmentedButtonDefaults.Icon(
                    active = captureMode == CaptureMode.VIDEO
                ) {
                    Icon(
                        Icons.Default.Videocam,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            },
            label = {
                Text(
                    "Video",
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        )
    }
}

/**
 * Static camera chrome extracted for @Preview (CameraX needs hardware,
 * so previews render this placeholder instead of binding a real preview).
 * [captureMode] and [isRecording] mirror the real view's states so every
 * use case (photo / video / recording) is previewable.
 */
@Composable
fun CameraCaptureContent(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    captureMode: CaptureMode = CaptureMode.PHOTO,
    isRecording: Boolean = false
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CameraChromeBackground)
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
            if (isRecording) {
                Text(
                    text = "● 00:07",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .background(Color.Red.copy(alpha = 0.7f), CircleShape)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )
            } else if (captureMode == CaptureMode.PHOTO) {
                IconButton(onClick = {}) {
                    Icon(
                        Icons.Default.FlashOff,
                        contentDescription = "Flash Off",
                        tint = Color.White
                    )
                }
            }
        }
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isRecording) {
                CaptureModeToggle(
                    captureMode = captureMode,
                    onModeChange = {}
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (captureMode == CaptureMode.PHOTO) {
                        "Tap preview to focus • Saves to photos/"
                    } else {
                        "Needs microphone for audio • Saves to videos/"
                    },
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelSmall
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {},
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
                FloatingActionButton(
                    onClick = {},
                    shape = CircleShape,
                    containerColor = if (isRecording) {
                        Color.Red
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.size(72.dp)
                ) {
                    Icon(
                        if (isRecording) Icons.Default.Stop else Icons.Default.Camera,
                        contentDescription = if (isRecording) {
                            "Stop Recording"
                        } else {
                            "Take Photo"
                        },
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            if (isRecording) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(
                        "● Recording… tap stop when done",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
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

@ComposePreview(
    showBackground = true,
    name = "3. Camera Capture Video - Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun CameraCaptureViewPreview_Video_Dark() {
    AppTheme(darkTheme = true) {
        CameraCaptureContent(
            onDismiss = {},
            captureMode = CaptureMode.VIDEO
        )
    }
}

@ComposePreview(
    showBackground = true,
    name = "4. Camera Capture Recording - Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun CameraCaptureViewPreview_Recording_Dark() {
    AppTheme(darkTheme = true) {
        CameraCaptureContent(
            onDismiss = {},
            captureMode = CaptureMode.VIDEO,
            isRecording = true
        )
    }
}
