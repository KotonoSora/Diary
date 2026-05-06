package com.kotonosora.todolist.feature.media
import com.kotonosora.todolist.data.database.MediaDao
import com.kotonosora.todolist.data.database.MediaEntity
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File
@OptIn(ExperimentalCoroutinesApi::class)
class MediaViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mediaDao: MediaDao
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mediaDao = mockk(relaxed = true)
    }
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    private fun buildViewModel(entities: List<MediaEntity> = emptyList()): MediaViewModel {
        every { mediaDao.getAllMedia() } returns flowOf(entities)
        val context = mockk<android.content.Context>(relaxed = true) {
            every { getExternalFilesDir(any()) } returns null
            every { filesDir } returns File(System.getProperty("java.io.tmpdir")!!)
            every { packageName } returns "com.kotonosora.todolist.test"
        }
        return MediaViewModel(context, mediaDao)
    }
    @Test
    fun `initial state has empty photo and audio lists`() = runTest {
        val vm = buildViewModel()
        advanceUntilIdle()
        assertTrue(vm.capturedPhotoPaths.value.isEmpty())
        assertTrue(vm.recordedAudioPaths.value.isEmpty())
    }
    @Test
    fun `initial isRecording is false`() = runTest {
        val vm = buildViewModel()
        assertFalse(vm.isRecording.value)
    }
    @Test
    fun `photos and audio are split from DB entities on init`() = runTest {
        val entities = listOf(
            MediaEntity(id = "1", todoId = null, type = "photo", filePath = "/p1.jpg"),
            MediaEntity(id = "2", todoId = null, type = "audio", filePath = "/a1.m4a"),
            MediaEntity(id = "3", todoId = null, type = "photo", filePath = "/p2.jpg")
        )
        val vm = buildViewModel(entities)
        val job = launch { vm.capturedPhotoPaths.collect {} }
        advanceUntilIdle()
        assertEquals(listOf("/p1.jpg", "/p2.jpg"), vm.capturedPhotoPaths.value)
        assertEquals(listOf("/a1.m4a"), vm.recordedAudioPaths.value)
        job.cancel()
    }
    @Test
    fun `onPhotoCaptured inserts a standalone photo entity into the DAO`() = runTest {
        val vm = buildViewModel()
        advanceUntilIdle()
        vm.onPhotoCaptured("/new_photo.jpg")
        advanceUntilIdle()
        coVerify {
            mediaDao.insertMedia(match {
                it.filePath == "/new_photo.jpg" &&
                it.type == "photo" &&
                it.todoId == null
            })
        }
    }
    @Test
    fun `deletePhoto calls DAO deleteMedia for the matching entity`() = runTest {
        val entity = MediaEntity(id = "p1", todoId = null, type = "photo", filePath = "/old.jpg")
        val vm = buildViewModel(listOf(entity))
        val job = launch { vm.capturedPhotoPaths.collect {} }
        advanceUntilIdle()
        mockkConstructor(File::class)
        every { anyConstructed<File>().delete() } returns true
        vm.deletePhoto("/old.jpg")
        advanceUntilIdle()
        coVerify { mediaDao.deleteMedia(entity) }
        unmockkConstructor(File::class)
        job.cancel()
    }
    @Test
    fun `deleteAudio calls DAO deleteMedia for the matching entity`() = runTest {
        val entity = MediaEntity(id = "a1", todoId = null, type = "audio", filePath = "/rec.m4a")
        val vm = buildViewModel(listOf(entity))
        val job = launch { vm.recordedAudioPaths.collect {} }
        advanceUntilIdle()
        mockkConstructor(File::class)
        every { anyConstructed<File>().delete() } returns true
        vm.deleteAudio("/rec.m4a")
        advanceUntilIdle()
        coVerify { mediaDao.deleteMedia(entity) }
        unmockkConstructor(File::class)
        job.cancel()
    }
}
