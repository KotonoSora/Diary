package com.kotonosora.todolist.ui.components.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.LruCache
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

/**
 * In-memory bitmap cache keyed by page + render width (a few display-sized
 * pages). Evicted bitmaps are left for GC — never recycled, since the same
 * instance may still be composed on screen.
 */
internal class PdfPageCache : LruCache<String, Bitmap>(32 * 1024) {
    override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
}

internal class PdfHandle(
    private val pfd: ParcelFileDescriptor,
    private val renderer: PdfRenderer
) {
    /**
     * Closed-renderer-safe count: returns -1 once the document is closed, so
     * UI-thread reads (tap handlers, recompositions racing a dismiss) can
     * never throw `IllegalStateException: Document already closed`.
     */
    val pageCount: Int
        get() = try {
            renderer.pageCount
        } catch (_: Exception) {
            -1
        }

    /**
     * Returns the page rendered at [targetWidthPx] (aspect preserved), from cache
     * when available. Out-of-range indexes return null. Serialized via [lock]:
     * `PdfRenderer` is not thread-safe, and without this concurrent row
     * renders (fast scrolling composes several pages at once) overlap in
     * native code and crash.
     */
    suspend fun page(index: Int, targetWidthPx: Int, cache: PdfPageCache, lock: Mutex): Bitmap? {
        return try {
            lock.withLock {
                val count = pageCount
                if (count <= 0 || index !in 0 until count) return@withLock null
                val key = "$index:$targetWidthPx"
                cache.get(key)?.let { return@withLock it }
                try {
                    renderer.openPage(index).use { page ->
                        if (page.width <= 0 || page.height <= 0) return@withLock null
                        val scale =
                            (targetWidthPx.toFloat() / page.width).coerceIn(0.5f, 4f)
                        // Fit the scaled page inside a 2048px box, preserving
                        // aspect: coercing width/height independently would
                        // squash tall pages. 2048x2048 ARGB_8888 is 16 MB, so
                        // one page plus LRU neighbors stays bounded.
                        var width = (page.width * scale).toInt().coerceAtLeast(1)
                        var height = (page.height * scale).toInt().coerceAtLeast(1)
                        val maxDim = 2048
                        if (width > maxDim || height > maxDim) {
                            val down = minOf(
                                maxDim.toFloat() / width,
                                maxDim.toFloat() / height
                            )
                            width = (width * down).toInt().coerceAtLeast(1)
                            height = (height * down).toInt().coerceAtLeast(1)
                        }
                        val bitmap = try {
                            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        } catch (e: OutOfMemoryError) {
                            android.util.Log.w("PdfReader", "bitmap ${width}x$height OOM", e)
                            try {
                                cache.evictAll()
                            } catch (_: Exception) {
                            }
                            return@withLock null
                        }
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        cache.put(key, bitmap)
                        bitmap
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: OutOfMemoryError) {
                    android.util.Log.w("PdfReader", "render page $index OOM", e)
                    try {
                        cache.evictAll()
                    } catch (_: Exception) {
                    }
                    null
                } catch (e: Exception) {
                    // Closed renderer, corrupt page, recycled bitmap — the UI
                    // shows a render error instead of crashing. Logged so a
                    // logcat reveals the real cause next time.
                    android.util.Log.w("PdfReader", "render page $index failed", e)
                    null
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    fun close() {
        try {
            renderer.close()
        } catch (e: Exception) {
            // ignore
        }
        try {
            pfd.close()
        } catch (e: Exception) {
            // ignore
        }
    }
}

internal fun openPdfRenderer(context: Context, filePath: String): PdfHandle? {
    return try {
        val pfd = if (filePath.startsWith("content://")) {
            context.contentResolver.openFileDescriptor(Uri.parse(filePath), "r")
        } else {
            val file = File(filePath)
            if (!file.exists()) return null
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        } ?: return null
        try {
            PdfHandle(pfd, PdfRenderer(pfd))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Corrupt/encrypted PDF: PdfRenderer ctor throws. Close the fd so
            // a failed open doesn't leak, then report "couldn't open".
            android.util.Log.w("PdfReader", "open ${filePath.takeLast(40)} failed", e)
            try {
                pfd.close()
            } catch (_: Exception) {
            }
            null
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // Missing file, revoked SAF permission, unreadable fd — error UI, not a crash.
        android.util.Log.w("PdfReader", "open ${filePath.takeLast(40)} failed", e)
        null
    }
}
