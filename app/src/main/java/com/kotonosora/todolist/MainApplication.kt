package com.kotonosora.todolist

import android.app.Application
import android.os.Build
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.svg.SvgDecoder
import com.kotonosora.todolist.data.sync.FileSyncWorker
import com.kotonosora.todolist.di.AppContainer
import com.kotonosora.todolist.notification.AppNotificationManager

class MainApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Image pipeline: SVG + animated GIF support on top of Coil defaults
        // (JPEG/PNG/WebP/BMP). Every AsyncImage in the app picks this up.
        SingletonImageLoader.setSafe { context ->
            ImageLoader.Builder(context)
                .components {
                    add(SvgDecoder.Factory())
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
