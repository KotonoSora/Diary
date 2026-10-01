package com.kotonosora.todolist.data.file

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaFileManagerPdfTest {

    @Test
    fun `sanitizePdfFileName strips extension and unsafe chars`() {
        assertEquals("Lecture_Notes", MediaFileManager.sanitizePdfFileName("Lecture Notes.pdf"))
        assertEquals("Report", MediaFileManager.sanitizePdfFileName("a/b:c\\Report.PDF"))
        assertEquals("document", MediaFileManager.sanitizePdfFileName(null))
        assertEquals("document", MediaFileManager.sanitizePdfFileName("   "))
        assertEquals("document", MediaFileManager.sanitizePdfFileName(".pdf"))
    }

    @Test
    fun `sanitizePdfFileName caps length`() {
        val long = "a".repeat(200) + ".pdf"
        val result = MediaFileManager.sanitizePdfFileName(long)
        assertTrue(result.length <= 60)
        assertTrue(result.all { it == 'a' })
    }
}
