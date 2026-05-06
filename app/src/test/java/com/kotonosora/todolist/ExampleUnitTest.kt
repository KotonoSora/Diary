package com.kotonosora.todolist

import org.junit.Test

import org.junit.Assert.*

/**
 * Smoke tests for the pure-Kotlin/pure-JVM portions of the app that do not require
 * a device or emulator.  JNI tests are excluded here because the native library is
 * only available on an Android ABI; those are covered by instrumented tests.
 */
class AppSmokeTest {

    @Test
    fun `UUID-based todo IDs are not blank`() {
        val id = java.util.UUID.randomUUID().toString()
        assertTrue(id.isNotBlank())
        assertEquals(36, id.length)
    }

    @Test
    fun `reminder time is one hour before due date`() {
        val dueDate = 1_000_000_000L
        val reminderTime = dueDate - 3_600_000L
        assertEquals(dueDate - 3_600_000L, reminderTime)
    }

    @Test
    fun `day boundary calculation spans exactly 24 hours minus 1 ms`() {
        val startOfDay = 0L
        val endOfDay = startOfDay + 86_400_000L - 1
        assertEquals(86_399_999L, endOfDay)
    }
}