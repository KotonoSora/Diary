package com.kotonosora.todolist.ui.components

import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
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
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kotonosora.todolist.data.file.MediaFileManager
import com.kotonosora.todolist.data.file.MediaOutputLocation
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * CameraX Photo Capture View featuring:
 * 1. Tap-to-Focus & Auto-Exposure Metering
 * 2. Flash Mode Controls (Off / Auto / On)
 * 3. Front / Back Lens Switching
 * 4. Shutter Pulse Flash Animation
 */
@Composable
fun CameraCaptureView(
    onPhotoCaptured: (String) -> Unit,
    onDismiss: () -> Unit,
    customFolderUriStr: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_OFF) }
    var activeCamera by remember { mutableStateOf<Camera?>(null) }

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

    LaunchedEffect(lensFacing, flashMode) {
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
                activeCamera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
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
                    val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
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

        // Camera Controls Row (Lens Flip & Shutter)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flip lens
            IconButton(
                onClick = {
                    lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                        CameraSelector.LENS_FACING_FRONT
                    } else {
                        CameraSelector.LENS_FACING_BACK
                    }
                },
                modifier = Modifier.padding(end = 24.dp)
            ) {
                Icon(
                    Icons.Default.FlipCameraAndroid,
                    contentDescription = "Switch Camera",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Shutter Button
            FloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        shutterFlashAlpha.snapTo(0.8f)
                        shutterFlashAlpha.animateTo(0f, animationSpec = tween(200))
                    }

                    val mediaFileManager = MediaFileManager(context)
                    val location = mediaFileManager.createPhotoOutputLocation(customFolderUriStr)

                    val targetFile = when (location) {
                        is MediaOutputLocation.DocumentFileUri -> File(context.cacheDir, "TEMP_${System.currentTimeMillis()}.jpg")
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
                                            context.contentResolver.openOutputStream(location.uri, "w")?.use { outStream ->
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
                            }
                        }
                    )
                },
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
