package com.kotonosora.todolist.data.factory

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import androidx.media3.exoplayer.ExoPlayer

/**
 * Factory Pattern interfaces for encapsulating complex media object creation
 * across Android API levels.
 */
interface MediaRecorderFactory {
    fun createMediaRecorder(): MediaRecorder
}

interface ExoPlayerFactory {
    fun createExoPlayer(): ExoPlayer
}

class DefaultMediaRecorderFactory(
    private val context: Context
) : MediaRecorderFactory {
    override fun createMediaRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
    }
}

class DefaultExoPlayerFactory(
    private val context: Context
) : ExoPlayerFactory {
    override fun createExoPlayer(): ExoPlayer {
        // Prefer software extension renderers when bundled (e.g. future FFmpeg/
        // MIDI extensions) and fall back to platform MediaCodec renderers.
        // No extra repo needed: MIDI-family files already decode via the
        // platform on the vast majority of devices; the rest surface the
        // existing inline playback error.
        val renderersFactory = androidx.media3.exoplayer.DefaultRenderersFactory(context).apply {
            setExtensionRendererMode(
                androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
            )
        }
        return ExoPlayer.Builder(context, renderersFactory).build()
    }
}
