package com.kotonosora.todolist

import android.app.Application
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.svg.SvgDecoder
import coil3.video.VideoFrameDecoder
import com.kotonosora.todolist.data.sync.FileSyncWorker
import com.kotonosora.todolist.di.AppContainer
import com.kotonosora.todolist.notification.AppNotificationManager

class MainApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        // Force English (US) as the app locale so resources, date/time/number
        // formatting and UI strings stay consistent on non-English devices.
        // Mirrors AppConstants.APP_LOCALE used by every SimpleDateFormat /
        // String.format callsite.
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en-US"))
        container = AppContainer(this)

        // Image pipeline: SVG + animated GIF support on top of Coil defaults
        // (JPEG/PNG/WebP/BMP). Every AsyncImage in the app picks this up.
        // VideoFrameDecoder adds cheap video thumbnails for the media gallery
        // grid (MediaMetadataRetriever, cached) without an ExoPlayer per cell.
        SingletonImageLoader.setSafe { context ->
            ImageLoader.Builder(context)
                .components {
                    add(SvgDecoder.Factory())
                    add(VideoFrameDecoder.Factory())
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        add(AnimatedImageDecoder.Factory())
                    } else {
                        add(GifDecoder.Factory())
                    }
                }
                .build()
        }

        // Create the notification channel
        AppNotificationManager.createNotificationChannel(this)
        // Trigger a one-time bidirectional sync of .md files <-> Room DB on every launch.
        WorkManager.getInstance(this).enqueueUniqueWork(
            FileSyncWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<FileSyncWorker>().build()
        )
    }
}
