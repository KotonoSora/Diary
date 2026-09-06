package com.kotonosora.todolist.data.repository

import com.kotonosora.todolist.data.database.LinkDao
import com.kotonosora.todolist.data.database.NoteDao
import com.kotonosora.todolist.data.database.NoteEntity
import com.kotonosora.todolist.data.database.TagDao
import com.kotonosora.todolist.data.file.VaultManager
import com.kotonosora.todolist.domain.model.NoteItem
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class VaultRepositoryTest {

    private val vaultManager: VaultManager = mockk(relaxed = true)
    private val noteDao: NoteDao = mockk(relaxed = true)
    private val linkDao: LinkDao = mockk(relaxed = true)
    private val tagDao: TagDao = mockk(relaxed = true)

    private lateinit var repository: VaultRepositoryImpl

    @Before
    fun setUp() {
        repository = VaultRepositoryImpl(
            vaultManager = vaultManager,
            noteDao = noteDao,
            linkDao = linkDao,
            tagDao = tagDao
        )
    }

    @Test
    fun `getAllNotes returns mapped note domain items`() = runTest {
        val noteEntities = listOf(
            NoteEntity(
                id = "Note1.md",
                title = "Note 1",
                relativePath = "",
                content = "Content 1"
            )
        )
        coEvery { noteDao.getAllNotes() } returns flowOf(noteEntities)

        val result = repository.getAllNotes().first()
        assertEquals(1, result.size)
        assertEquals("Note 1", result[0].title)
    }

    @Test
    fun `saveNote writes file to vaultManager and indexes to DB`() = runTest {
        val note = NoteItem(
            id = "Idea.md",
            title = "Idea Note",
            relativePath = "",
            content = "# Idea Note\n\n[[Roadmap]] and #important"
        )
        coEvery { vaultManager.saveNote(note, any()) } returns true

        val success = repository.saveNote(note)
        assertEquals(true, success)

        coVerify { noteDao.insertNote(any()) }
    }

    @Test
    fun `deleteNote removes file and cleans up DB references`() = runTest {
        coEvery { vaultManager.deleteNote("Old.md", any()) } returns true

        val success = repository.deleteNote("Old.md")
        assertEquals(true, success)

        coVerify { noteDao.deleteNoteById("Old.md") }
        coVerify { linkDao.deleteLinksForSource("Old.md") }
        coVerify { tagDao.deleteTagsForNote("Old.md") }
    }
}
