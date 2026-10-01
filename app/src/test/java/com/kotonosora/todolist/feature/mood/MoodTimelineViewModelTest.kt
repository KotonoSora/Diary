package com.kotonosora.todolist.feature.mood

import com.kotonosora.todolist.domain.model.EmotionStamp
import com.kotonosora.todolist.domain.model.NoteItem
import com.kotonosora.todolist.domain.repository.VaultRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MoodTimelineViewModelTest {

    private val vaultRepository: VaultRepository = mockk(relaxed = true)
    private lateinit var viewModel: MoodTimelineViewModel

    @Before
    fun setUp() {
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `timeline groups stamped notes by day newest first and skips unstamped`() = runTest {
        val dayOne = 1_700_000_000_000L
        val notes = listOf(
            NoteItem(
                id = "a.md",
                title = "Morning",
                relativePath = "",
                content = "",
                updatedAt = dayOne,
                emotion = EmotionStamp.HAPPY
            ),
            NoteItem(
                id = "b.md",
                title = "Evening",
                relativePath = "",
                content = "",
                updatedAt = dayOne + 3_600_000L,
                emotion = EmotionStamp.CALM
            ),
            NoteItem(
                id = "c.md",
                title = "No mood",
                relativePath = "",
                content = "",
                updatedAt = dayOne + 7_200_000L
            ),
            NoteItem(
                id = "d.md",
                title = "Old",
                relativePath = "",
                content = "",
                updatedAt = dayOne - 86_400_000L,
                emotion = EmotionStamp.TIRED
            )
        )
        every { vaultRepository.getAllNotes() } returns flowOf(notes)

        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        viewModel = MoodTimelineViewModel(vaultRepository)

        // Subscribe so WhileSubscribed starts collecting the upstream flow
        val collectionJob = launch { viewModel.timeline.collect {} }
        advanceUntilIdle()
        val timeline = viewModel.timeline.value
        collectionJob.cancel()

        assertEquals(2, timeline.size)
        assertEquals(listOf("b.md", "a.md"), timeline[0].entries.map { it.noteId })
        assertEquals(listOf("d.md"), timeline[1].entries.map { it.noteId })
        assertEquals(
            MoodTimelineViewModel.startOfDayMillis(dayOne),
            timeline[0].dayStartMillis
        )
    }
}
