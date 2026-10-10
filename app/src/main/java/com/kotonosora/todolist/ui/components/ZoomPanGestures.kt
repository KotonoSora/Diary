package com.kotonosora.todolist.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Pinch-to-zoom + pan that plays nicely inside a [ModalBottomSheet].
 *
 * Unlike [androidx.compose.foundation.gestures.detectTransformGestures],
 * single-finger drags are NOT consumed while [isZoomed] is false, so a
 * swipe-down dismisses the sheet instead of being swallowed. Two-finger
 * pinches always consume; one-finger pans consume only when zoomed in.
 */
internal fun Modifier.zoomPanGestures(
    isZoomed: () -> Boolean,
    onGesture: (zoomChange: Float, panChange: Offset) -> Unit,
    onEnd: () -> Unit = {}
): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        var event: PointerEvent
        do {
            event = awaitPointerEvent()
            val pressedCount = event.changes.count { it.pressed }
            if (pressedCount >= 2) {
                val zoomChange = event.calculateZoom()
                val panChange = event.calculatePan()
                if (zoomChange != 1f || panChange != Offset.Zero) {
                    onGesture(zoomChange, panChange)
                }
                event.changes.forEach { it.consume() }
            } else if (isZoomed()) {
                val panChange = event.calculatePan()
                if (panChange != Offset.Zero) {
                    onGesture(1f, panChange)
                }
                event.changes.forEach { it.consume() }
            }
            // Single finger at 1x: pass through so the sheet can drag/dismiss.
        } while (event.changes.any { it.pressed })
        // All pointers released: the reader uses this to commit the zoom
        // level (crisper re-render). Harmless after plain taps/scrolls.
        onEnd()
    }
}
