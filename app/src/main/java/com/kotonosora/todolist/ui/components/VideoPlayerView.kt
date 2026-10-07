package com.kotonosora.todolist.ui.components

import android.content.res.Configuration
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.kotonosora.todolist.ui.theme.AppTheme
import java.io.File

/**
 * Media3 ExoPlayer video player. Handles the common local containers/codecs
 * (MP4/MOV/3GP, WebM, MKV, TS and MP3/AAC/FLAC/OGG/WAV audio); anything else
 * surfaces an inline error instead of a black box.
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerView(
    filePath: String,
    modifier: Modifier = Modifier,
    fullscreen: Boolean = false
) {
    if (LocalInspectionMode.current) {
        // Preview placeholder for IDE layout renderer (ExoPlayer needs a real context)
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayCircleOutline,
                    contentDescription = "Video preview",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
        return
    }

    val context = LocalContext.current
    var playbackError by remember(filePath) { mutableStateOf<String?>(null) }

    val exoPlayer = remember(filePath) {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = if (filePath.startsWith("content://")) {
                MediaItem.fromUri(Uri.parse(filePath))
            } else {
                MediaItem.fromUri(Uri.fromFile(File(filePath)))
            }
            setMediaItem(mediaItem)
            addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    playbackError = error.message ?: "Unsupported video format"
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    // ExoPlayer retries some failures on its own — drop a stale
                    // error overlay once playback actually recovers.
                    if (playbackState == Player.STATE_READY) {
                        playbackError = null
                    }
                }
            })
            prepare()
        }
    }

    DisposableEffect(filePath) {
        onDispose {
            exoPlayer.release()
        }
    }

    Card(
        modifier = if (fullscreen) modifier.fillMaxSize() else modifier.fillMaxWidth(),
        // Fullscreen is edge-to-edge: no rounded corners or tinted margins.
        shape = if (fullscreen) RectangleShape else RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (fullscreen) {
                Color.Black
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            }
        )
    ) {
        Box(
            modifier = if (fullscreen) {
                Modifier.fillMaxSize()
            } else {
                Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            }.clip(if (fullscreen) RectangleShape else RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            val error = playbackError
            if (error != null) {
                Text(
                    text = "Couldn't play this video ($error).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            } else {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = true
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Preview(
    showBackground = true,
    name = "1. Video Player - Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun VideoPlayerViewPreview_Dark() {
    AppTheme(darkTheme = true) {
        VideoPlayerView(filePath = "preview_sample.mp4")
    }
}

@Preview(
    showBackground = true,
    name = "2. Video Player - Light",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
fun VideoPlayerViewPreview_Light() {
    AppTheme(darkTheme = false) {
        VideoPlayerView(filePath = "preview_sample.mp4")
    }
}
