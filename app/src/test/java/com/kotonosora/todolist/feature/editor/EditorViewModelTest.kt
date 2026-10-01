package com.kotonosora.todolist.feature.editor

import com.kotonosora.todolist.domain.model.NoteItem
import com.kotonosora.todolist.domain.repository.VaultRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditorViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val vaultRepository: VaultRepository = mockk(relaxed = true)
    private lateinit var viewModel: EditorViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = EditorViewModel(vaultRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadNote populates note state from vault repository`() = runTest {
        val note = NoteItem(
            id = "Meeting.md",
            title = "Meeting",
            relativePath = "",
            content = "Discussion text"
        )
        coEvery { vaultRepository.getNoteById("Meeting.md") } returns note

        viewModel.loadNote("Meeting.md")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Meeting", viewModel.uiState.value.note.title)
        assertEquals("Discussion text", viewModel.uiState.value.note.content)
    }

    @Test
    fun `onContentChange triggers autocomplete when double brackets are typed`() = runTest {
        val targetNote = NoteItem(id = "Project.md", title = "Project", relativePath = "", content = "")
        coEvery { vaultRepository.searchNotes("Proj") } returns flowOf(listOf(targetNote))

        viewModel.onContentChange("Check [[Proj")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showSuggestions)
        assertEquals(listOf("Project"), viewModel.uiState.value.suggestions)
    }

    @Test
    fun `onSuggestionSelected replaces double brackets query with target note title`() = runTest {
        viewModel.onContentChange("Check [[Proj")
        viewModel.onSuggestionSelected("Project")

        assertEquals("Check [[Project]] ", viewModel.uiState.value.note.content)
        assertFalse(viewModel.uiState.value.showSuggestions)
    }

    @Test
    fun `saveNote calls repository saveNote`() = runTest {
        val note = NoteItem(id = "Test.md", title = "Test", relativePath = "", content = "Initial")
        coEvery { vaultRepository.getNoteById("Test.md") } returns note

        viewModel.loadNote("Test.md")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onContentChange("Updated text")
        viewModel.saveNote()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { vaultRepository.saveNote(any()) }
    }

    @Test
    fun `loadCustomTemplates populates template names`() = runTest {
        coEvery { vaultRepository.getCustomTemplateNames() } returns listOf("Standup")

        viewModel.loadCustomTemplates()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("Standup"), viewModel.uiState.value.customTemplates)
    }

    @Test
    fun `applyCustomTemplate substitutes title placeholder`() = runTest {
        val note = NoteItem(id = "Sprint.md", title = "Sprint 12", relativePath = "", content = "")
        coEvery { vaultRepository.getNoteById("Sprint.md") } returns note
        coEvery { vaultRepository.getCustomTemplateContent("Standup") } returns "# {{title}}\n\n- Item"

        viewModel.loadNote("Sprint.md")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.applyCustomTemplate("Standup")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("# Sprint 12\n\n- Item", viewModel.uiState.value.note.content)
    }

    @Test
    fun `saveCurrentNoteAsTemplate persists and refreshes list`() = runTest {
        val note = NoteItem(id = "Sprint.md", title = "Sprint", relativePath = "", content = "# Body")
        coEvery { vaultRepository.getNoteById("Sprint.md") } returns note
        coEvery { vaultRepository.saveCustomTemplate(any(), any()) } returns true
        coEvery { vaultRepository.getCustomTemplateNames() } returns listOf("SprintTpl")

        viewModel.loadNote("Sprint.md")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.saveCurrentNoteAsTemplate("SprintTpl")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { vaultRepository.saveCustomTemplate("SprintTpl", "# Body") }
        assertEquals(listOf("SprintTpl"), viewModel.uiState.value.customTemplates)
    }

    @Test
    fun `deleteCustomTemplate refreshes list`() = runTest {
        coEvery { vaultRepository.getCustomTemplateNames() } returns emptyList()

        viewModel.deleteCustomTemplate("Old")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { vaultRepository.deleteCustomTemplate("Old") }
        assertEquals(emptyList<String>(), viewModel.uiState.value.customTemplates)
    }
}
