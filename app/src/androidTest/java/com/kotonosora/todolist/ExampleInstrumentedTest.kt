package com.kotonosora.todolist

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented smoke tests verifying the app package and native library load.
 */
@RunWith(AndroidJUnit4::class)
class AppInstrumentedTest {

    @Test
    fun appContext_hasCorrectPackageName() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.kotonosora.todolist", appContext.packageName)
    }

    @Test
    fun nativeLibrary_loadsWithoutException() {
        // TodoApplication loads the library on start; verify it is already loaded by
        // exercising a simple JNI round-trip using TodoNativeHelper.
        val result = com.kotonosora.todolist.data.native.TodoNativeHelper.sortStrings(
            arrayOf("banana", "apple", "cherry")
        )
        assertArrayEquals(arrayOf("apple", "banana", "cherry"), result)
    }

    @Test
    fun nativeLibrary_filterByPrefix_returnsMatches() {
        val titles = arrayOf("Buy groceries", "Read book", "Build app", "Bake cake")
        val matches = com.kotonosora.todolist.data.native.TodoNativeHelper.filterByPrefix(titles, "b")
        assertEquals(3, matches.size)
        assertTrue(matches.contains("Buy groceries"))
        assertTrue(matches.contains("Build app"))
        assertTrue(matches.contains("Bake cake"))
    }
}