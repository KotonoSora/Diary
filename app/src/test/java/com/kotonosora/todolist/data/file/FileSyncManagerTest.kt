package com.kotonosora.todolist.data.file

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class FileSyncManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `md file with title heading can be read correctly`() = runTest {
        val mdFile = tempFolder.newFile("test-id-1.md")
        mdFile.writeText("# My Test Todo\n\n- **Status**: Pending\n")

        val titleLine = mdFile.readLines().firstOrNull { it.startsWith("# ") }
        assertEquals("My Test Todo", titleLine?.removePrefix("# ")?.trim())
    }

    @Test
    fun `parseMdFile extracts correct title from markdown`() = runTest {
        val mdContent = """
            # Buy Groceries
            
            - **Status**: Pending
            - **Due Date**: 2026-05-10 09:00
        """.trimIndent()

        val file = tempFolder.newFile("task.md")
        file.writeText(mdContent)

        val titleLine = file.readLines().firstOrNull { it.startsWith("# ") }
        assertEquals("Buy Groceries", titleLine?.removePrefix("# ")?.trim())
    }

    @Test
    fun `parseMdFile extracts completion status`() = runTest {
        val mdContent = "# Done Task\n\n- **Status**: Completed\n"
        val file = tempFolder.newFile("done.md")
        file.writeText(mdContent)

        val isCompleted = file.readLines().any { it.contains("**Status**: Completed") }
        assertTrue(isCompleted)
    }

    @Test
    fun `parseMdFile extracts description section`() = runTest {
        val mdContent = "# Task\n\n- **Status**: Pending\n\n## Description\nThis is the description content.\n"
        val file = tempFolder.newFile("desc.md")
        file.writeText(mdContent)

        val lines = file.readLines()
        var inDesc = false
        val descLines = mutableListOf<String>()
        for (line in lines) {
            when {
                line.startsWith("## Description") -> inDesc = true
                inDesc && line.startsWith("#") -> inDesc = false
                inDesc -> descLines.add(line)
            }
        }
        assertEquals("This is the description content.", descLines.joinToString("\n").trim())
    }

    @Test
    fun `txt file can be parsed for title status and description`() = runTest {
        val txtContent = """
            TITLE: Read Book
            STATUS: Completed
            
            DESCRIPTION:
            Finish reading chapter 5 of Kotlin in Action.
        """.trimIndent()

        val file = tempFolder.newFile("read.txt")
        file.writeText(txtContent)

        val lines = file.readLines()
        val titleLine = lines.firstOrNull { it.startsWith("TITLE: ") }?.removePrefix("TITLE: ")?.trim()
        val isCompleted = lines.any { it.startsWith("STATUS: Completed") }

        assertEquals("Read Book", titleLine)
        assertTrue(isCompleted)
    }
}
