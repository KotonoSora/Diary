package com.kotonosora.todolist.data.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MediaDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var mediaDao: MediaDao
    private lateinit var todoDao: TodoDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        mediaDao = database.mediaDao()
        todoDao = database.todoDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    // Helper to insert a parent TodoEntity (required by foreign key constraint)
    private suspend fun insertParentTodo(id: String = "todo1") {
        todoDao.insertTodo(
            TodoEntity(id = id, title = "Parent", description = null,
                dueDate = null, filePath = null, isCompleted = false)
        )
    }

    @Test
    fun insertMedia_andRetrieveByTodoId() = runTest {
        insertParentTodo("t1")
        val media = MediaEntity(id = "m1", todoId = "t1", type = "photo",
            filePath = "/path/photo.jpg")
        mediaDao.insertMedia(media)

        val result = mediaDao.getMediaForTodo("t1").first()
        assertEquals(1, result.size)
        assertEquals("photo", result[0].type)
        assertEquals("/path/photo.jpg", result[0].filePath)
    }

    @Test
    fun getAllMedia_returnsAllEntries() = runTest {
        insertParentTodo("t2")
        mediaDao.insertMedia(MediaEntity(id = "m2", todoId = "t2", type = "photo",
            filePath = "/photos/a.jpg"))
        mediaDao.insertMedia(MediaEntity(id = "m3", todoId = "t2", type = "audio",
            filePath = "/audio/b.m4a"))

        val all = mediaDao.getAllMedia().first()
        assertEquals(2, all.size)
    }

    @Test
    fun deleteMedia_removesEntry() = runTest {
        insertParentTodo("t3")
        val media = MediaEntity(id = "m4", todoId = "t3", type = "audio",
            filePath = "/audio/c.m4a")
        mediaDao.insertMedia(media)
        mediaDao.deleteMedia(media)

        val result = mediaDao.getMediaForTodo("t3").first()
        assertEquals(0, result.size)
    }

    @Test
    fun deleteAllMediaForTodo_removesAllChildren() = runTest {
        insertParentTodo("t4")
        mediaDao.insertMedia(MediaEntity(id = "m5", todoId = "t4", type = "photo",
            filePath = "/p1.jpg"))
        mediaDao.insertMedia(MediaEntity(id = "m6", todoId = "t4", type = "photo",
            filePath = "/p2.jpg"))

        mediaDao.deleteAllMediaForTodo("t4")

        val result = mediaDao.getMediaForTodo("t4").first()
        assertEquals(0, result.size)
    }

    @Test
    fun cascadeDelete_removesMediaWhenTodoDeleted() = runTest {
        insertParentTodo("t5")
        mediaDao.insertMedia(MediaEntity(id = "m7", todoId = "t5", type = "photo",
            filePath = "/p3.jpg"))

        // Delete the parent todo — cascade should remove child media
        todoDao.deleteTodo(
            TodoEntity(id = "t5", title = "Parent", description = null,
                dueDate = null, filePath = null, isCompleted = false)
        )

        val result = mediaDao.getMediaForTodo("t5").first()
        assertEquals(0, result.size)
    }

    @Test
    fun insertMedia_withNullTodoId_standaloneGalleryEntry() = runTest {
        // MediaViewModel uses todoId=null for standalone gallery items.
        // SQLite skips FK enforcement for NULL values, so no parent TodoEntity is needed.
        val media = MediaEntity(id = "m8", todoId = null, type = "photo",
            filePath = "/standalone.jpg")
        mediaDao.insertMedia(media)

        val all = mediaDao.getAllMedia().first()
        assertEquals(1, all.size)
        assertEquals(null, all[0].todoId)
    }
}

