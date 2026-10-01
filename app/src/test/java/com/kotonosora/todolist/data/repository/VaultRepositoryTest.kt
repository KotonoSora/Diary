package com.kotonosora.todolist.data.repository

import com.kotonosora.todolist.data.database.LinkDao
import com.kotonosora.todolist.data.database.NoteDao
import com.kotonosora.todolist.data.database.NoteEntity
import com.kotonosora.todolist.data.database.NoteFtsDao
import com.kotonosora.todolist.data.database.TagDao
import com.kotonosora.todolist.data.database.ZettelMetadataDao
import com.kotonosora.todolist.data.file.VaultManager
import com.kotonosora.todolist.domain.model.NoteItem
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class VaultRepositoryTest {

    private val vaultManager: VaultManager = mockk(relaxed = true)
    private val noteDao: NoteDao = mockk(relaxed = true)
    private val linkDao: LinkDao = mockk(relaxed = true)
    private val tagDao: TagDao = mockk(relaxed = true)
    private val zettelMetadataDao: ZettelMetadataDao = mockk(relaxed = true)
    private val noteFtsDao: NoteFtsDao = mockk(relaxed = true)

    private lateinit var repository: VaultRepositoryImpl

    @Before
    fun setUp() {
        repository = VaultRepositoryImpl(
            vaultManager = vaultManager,
            noteDao = noteDao,
            linkDao = linkDao,
            tagDao = tagDao,
            zettelMetadataDao = zettelMetadataDao,
            noteFtsDao = noteFtsDao
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
        coVerify { zettelMetadataDao.insertMetadata(any()) }
        coVerify { noteFtsDao.replaceFtsForNote("Idea.md", match { it.noteId == "Idea.md" && it.tags.contains("important") }) }
    }

    @Test
    fun `renameNote renames file and refactors WikiLinks in other notes`() = runTest {
        val oldNoteEntity = NoteEntity(
            id = "Roadmap.md",
            title = "Roadmap",
            relativePath = "",
            content = "# Roadmap\n\nProject milestones"
        )
        val referencingNoteEntity = NoteEntity(
            id = "Project.md",
            title = "Project",
            relativePath = "",
            content = "# Project\n\nSee [[Roadmap]] for details."
        )

        coEvery { noteDao.getNoteById("Roadmap.md") } returns oldNoteEntity
        coEvery { vaultManager.renameNote("Roadmap.md", any(), any()) } returns true
        coEvery { noteDao.getAllNotesOnce() } returns listOf(referencingNoteEntity)

        val success = repository.renameNote("Roadmap.md", "Strategy")
        assertTrue(success)

        coVerify { vaultManager.saveNote(match { it.content.contains("[[Strategy]]") }, any()) }
    }

    @Test
    fun `deleteNote removes file and cleans up DB references`() = runTest {
        coEvery { vaultManager.deleteNote("Old.md", any()) } returns true

        val success = repository.deleteNote("Old.md")
        assertEquals(true, success)

        coVerify { noteDao.deleteNoteById("Old.md") }
        coVerify { linkDao.deleteLinksForSource("Old.md") }
        coVerify { tagDao.deleteTagsForNote("Old.md") }
        coVerify { zettelMetadataDao.deleteMetadataForNote("Old.md") }
        coVerify { noteFtsDao.deleteFtsForNote("Old.md") }
    }

    @Test
    fun `getNoteById rehydrates mood stamps from frontmatter`() = runTest {
        val entity = NoteEntity(
            id = "Diary.md",
            title = "Diary",
            relativePath = "",
            content = "---\nemotion: HAPPY\nactions: [WORK, BOGUS]\n---\n# Diary"
        )
        coEvery { noteDao.getNoteById("Diary.md") } returns entity

        val note = repository.getNoteById("Diary.md")
        assertEquals(
            com.kotonosora.todolist.domain.model.EmotionStamp.HAPPY,
            note?.emotion
        )
        assertEquals(
            listOf(com.kotonosora.todolist.domain.model.ActionStamp.WORK),
            note?.actions
        )
    }

    @Test
    fun `searchNotes falls back to title search for non-text queries`() = runTest {
        coEvery { noteDao.searchNotesByTitle("!!!") } returns flowOf(emptyList())

        val result = repository.searchNotes("!!!").first()
        assertTrue(result.isEmpty())

        coVerify { noteDao.searchNotesByTitle("!!!") }
    }

    @Test
    fun `searchNotes escapes LIKE wildcards in title fallback`() = runTest {
        coEvery { noteDao.searchNotesByTitle(any()) } returns flowOf(emptyList())
        every { noteDao.searchNotesFts(any()) } returns flowOf(emptyList())

        repository.searchNotes("100%_\\").first()

        coVerify { noteDao.searchNotesByTitle("100\\%\\_\\\\") }
    }

    @Test
    fun `searchNotes caps FTS terms for pathological input`() = runTest {
        coEvery { noteDao.searchNotesByTitle(any()) } returns flowOf(emptyList())
        val ftsSlot = slot<String>()
        every { noteDao.searchNotesFts(capture(ftsSlot)) } returns flowOf(emptyList())

        val query = (1..20).joinToString(" ") { "word$it" }
        repository.searchNotes(query).first()

        val terms = ftsSlot.captured.trim().split(" ")
        assertEquals(12, terms.size)
        assertTrue(terms.all { it.startsWith("\"") && it.endsWith("*\"") })
    }

    @Test
    fun `saveNote indexes frontmatter tags alongside body tags`() = runTest {
        val note = NoteItem(
            id = "Diary.md",
            title = "Diary",
            relativePath = "",
            content = "---\ntags: [diary, journal]\n---\n# Diary\n\n#morning"
        )
        coEvery { vaultManager.saveNote(note, any()) } returns true

        val success = repository.saveNote(note)
        assertTrue(success)

        coVerify {
            tagDao.insertTags(match { tags ->
                tags.map { it.tagName }.containsAll(listOf("#morning", "#diary", "#journal"))
            })
        }
        coVerify {
            noteFtsDao.replaceFtsForNote(
                "Diary.md",
                match { it.tags.contains("#diary") && it.tags.contains("#morning") }
            )
        }
    }

    @Test
    fun `custom templates delegate to vaultManager without touching the note index`() = runTest {
        coEvery { vaultManager.listTemplateNames(any()) } returns listOf("Standup")
        coEvery { vaultManager.readTemplate("Standup", any()) } returns "# Template"

        assertEquals(listOf("Standup"), repository.getCustomTemplateNames())
        assertEquals("# Template", repository.getCustomTemplateContent("Standup"))

        repository.saveCustomTemplate("Retro", "# Retro")
        coVerify { vaultManager.saveTemplate("Retro", "# Retro", any()) }
        coVerify(exactly = 0) { noteDao.insertNote(any()) }

        repository.deleteCustomTemplate("Retro")
        coVerify { vaultManager.deleteTemplate("Retro", any()) }
        coVerify(exactly = 0) { noteDao.deleteNoteById(any()) }
    }
}
