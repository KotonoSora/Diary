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
}
