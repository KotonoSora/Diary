package com.kotonosora.todolist.feature.search

import com.kotonosora.todolist.domain.model.NoteItem
import com.kotonosora.todolist.domain.repository.VaultRepository
import io.mockk.coEvery
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val vaultRepository: VaultRepository = mockk(relaxed = true)
    private lateinit var viewModel: SearchViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = SearchViewModel(vaultRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onQueryChange updates results from searchNotes`() = runTest {
        val note = NoteItem(
            id = "Roadmap.md",
            title = "Project Roadmap",
            relativePath = "",
            content = "Q1 and Q2 milestones"
        )
        coEvery { vaultRepository.searchNotes("Roadmap") } returns flowOf(listOf(note))

        viewModel.onQueryChange("Roadmap")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Roadmap", viewModel.uiState.value.query)
        assertEquals(1, viewModel.uiState.value.searchResults.size)
        assertEquals("Project Roadmap", viewModel.uiState.value.searchResults[0].title)
    }

    @Test
    fun `clearQuery resets query and results`() = runTest {
        viewModel.onQueryChange("Roadmap")
        viewModel.clearQuery()

        assertEquals("", viewModel.uiState.value.query)
        assertEquals(0, viewModel.uiState.value.searchResults.size)
    }
}
