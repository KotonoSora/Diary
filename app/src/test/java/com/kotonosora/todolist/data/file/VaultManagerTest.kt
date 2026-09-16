package com.kotonosora.todolist.data.file

import android.content.Context
import com.kotonosora.todolist.domain.model.NoteItem
import com.kotonosora.todolist.domain.model.VaultNode
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class VaultManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val context: Context = mockk(relaxed = true)
    private lateinit var vaultManager: VaultManager

    @Before
    fun setUp() {
        val rootDir = tempFolder.newFolder("Vault")
        every { context.getExternalFilesDir(any()) } returns rootDir
        every { context.filesDir } returns rootDir
        vaultManager = VaultManager(context)
    }

    @Test
    fun `saveNote writes markdown file to local directory`() = runTest {
        val note = NoteItem(
            id = "Projects/Roadmap.md",
            title = "Project Roadmap",
            relativePath = "Projects",
            content = "# Project Roadmap\n\n[[Tasks]] and #ideas",
            fileFormat = "md"
        )

        val success = vaultManager.saveNote(note)
        assertTrue(success)

        val allNotes = vaultManager.readAllNotes()
        assertEquals(1, allNotes.size)
        assertEquals("Project Roadmap", allNotes[0].title)
        assertEquals("# Project Roadmap\n\n[[Tasks]] and #ideas", allNotes[0].content)
    }

    @Test
    fun `getVaultTree builds hierarchical folder node structure`() = runTest {
        val note1 = NoteItem(
            id = "Note1.md",
            title = "Note 1",
            relativePath = "",
            content = "Root note"
        )
        val note2 = NoteItem(
            id = "Work/Meeting.md",
            title = "Meeting Notes",
            relativePath = "Work",
            content = "Discussion notes"
        )

        vaultManager.saveNote(note1)
        vaultManager.saveNote(note2)

        val tree = vaultManager.getVaultTree()
        assertNotNull(tree)
        assertTrue(tree.children.any { it.name == "Note1.md" })

        val workFolder = tree.children.firstOrNull { it.name == "Work" } as? VaultNode.FolderNode
        assertNotNull(workFolder)
        assertTrue(workFolder!!.children.any { it.name == "Meeting.md" })
    }

    @Test
    fun `deleteNote removes file from storage`() = runTest {
        val note = NoteItem(
            id = "ToDelete.md",
            title = "To Delete",
            relativePath = "",
            content = "Temporary note"
        )

        vaultManager.saveNote(note)
        assertEquals(1, vaultManager.readAllNotes().size)

        val deleted = vaultManager.deleteNote("ToDelete.md")
        assertTrue(deleted)
        assertEquals(0, vaultManager.readAllNotes().size)
    }
}
