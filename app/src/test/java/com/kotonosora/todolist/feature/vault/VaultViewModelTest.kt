package com.kotonosora.todolist.feature.vault

import com.kotonosora.todolist.domain.model.NoteType
import com.kotonosora.todolist.domain.model.VaultNode
import com.kotonosora.todolist.domain.repository.VaultRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VaultViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val vaultRepository: VaultRepository = mockk(relaxed = true)
    private lateinit var viewModel: VaultViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val rootNode = VaultNode.FolderNode(name = "Vault", relativePath = "")
        coEvery { vaultRepository.getVaultTree(any()) } returns rootNode
        viewModel = VaultViewModel(vaultRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadVault populates root folder node`() = runTest {
        viewModel.loadVault()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Vault", viewModel.uiState.value.rootNode.name)
        coVerify { vaultRepository.syncVaultFilesToDb(any()) }
    }

    @Test
    fun `createZettelNoteInFolder saves new note with template and UID`() = runTest {
        var createdPath = ""
        coEvery { vaultRepository.saveNote(any()) } returns true

        viewModel.createZettelNoteInFolder(
            folderPath = "Projects",
            title = "Roadmap",
            noteType = NoteType.PERMANENT
        ) { path ->
            createdPath = path
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(createdPath.startsWith("Projects/"))
        assertTrue(createdPath.endsWith("-Roadmap.md"))
        coVerify { vaultRepository.saveNote(any()) }
    }
}
