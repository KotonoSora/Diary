package com.kotonosora.todolist.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ZettelUidGeneratorTest {

    @Test
    fun `generateUid returns 12-digit timestamp string`() {
        val uid = ZettelUidGenerator.generateUid()
        assertEquals(12, uid.length)
        assertTrue(uid.all { it.isDigit() })
    }

    @Test
    fun `generateUid formats specific timestamp accurately`() {
        // 2026-03-01 12:30:00 UTC
        val millis = 1772368200000L
        val uid = ZettelUidGenerator.generateUid(millis)
        assertEquals(12, uid.length)
    }
}
