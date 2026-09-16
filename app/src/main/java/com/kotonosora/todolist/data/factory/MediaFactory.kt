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
        return ExoPlayer.Builder(context).build()
    }
}
