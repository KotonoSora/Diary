package com.kotonosora.todolist.data.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TaskDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var taskDao: TaskDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        taskDao = database.taskDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertTask_andRetrieveById() = runTest {
        val entity = TaskEntity(id = "1", title = "Test", description = null,
            dueDate = null, filePath = null, isCompleted = false)
        taskDao.insertTask(entity)

        val retrieved = taskDao.getTaskById("1")
        assertNotNull(retrieved)
        assertEquals("Test", retrieved?.title)
    }

    @Test
    fun getAllTasks_returnsInsertedEntities() = runTest {
        val e1 = TaskEntity(id = "a", title = "Alpha", description = null,
            dueDate = 1000L, filePath = null, isCompleted = false)
        val e2 = TaskEntity(id = "b", title = "Beta", description = null,
            dueDate = 2000L, filePath = null, isCompleted = true)
        taskDao.insertTask(e1)
        taskDao.insertTask(e2)

        val tasks = taskDao.getAllTasks().first()
        assertEquals(2, tasks.size)
    }

    @Test
    fun getAllTasksOnce_returnsAllEntities() = runTest {
        val e1 = TaskEntity(id = "x", title = "X", description = null,
            dueDate = null, filePath = null, isCompleted = false)
        taskDao.insertTask(e1)

        val result = taskDao.getAllTasksOnce()
        assertEquals(1, result.size)
        assertEquals("x", result[0].id)
    }

    @Test
    fun updateTask_changesValues() = runTest {
        val entity = TaskEntity(id = "2", title = "Original", description = null,
            dueDate = null, filePath = null, isCompleted = false)
        taskDao.insertTask(entity)

        val updated = entity.copy(title = "Updated", isCompleted = true)
        taskDao.updateTask(updated)

        val retrieved = taskDao.getTaskById("2")
        assertEquals("Updated", retrieved?.title)
        assertEquals(true, retrieved?.isCompleted)
    }

    @Test
    fun deleteTask_removesEntity() = runTest {
        val entity = TaskEntity(id = "3", title = "ToDelete", description = null,
            dueDate = null, filePath = null, isCompleted = false)
        taskDao.insertTask(entity)
        taskDao.deleteTask(entity)

        val retrieved = taskDao.getTaskById("3")
        assertNull(retrieved)
    }

    @Test
    fun getTasksByDate_returnsOnlyMatchingRange() = runTest {
        val inRange = TaskEntity(id = "r1", title = "In Range", description = null,
            dueDate = 5000L, filePath = null, isCompleted = false)
        val outOfRange = TaskEntity(id = "r2", title = "Out of Range", description = null,
            dueDate = 10_000L, filePath = null, isCompleted = false)
        taskDao.insertTask(inRange)
        taskDao.insertTask(outOfRange)

        val results = taskDao.getTasksByDate(1000L, 7000L).first()
        assertEquals(1, results.size)
        assertEquals("r1", results[0].id)
    }

    @Test
    fun insertTask_withSameId_replacesExisting() = runTest {
        val original = TaskEntity(id = "dup", title = "Original", description = null,
            dueDate = null, filePath = null, isCompleted = false)
        val replacement = original.copy(title = "Replaced")
        taskDao.insertTask(original)
        taskDao.insertTask(replacement)

        val retrieved = taskDao.getTaskById("dup")
        assertEquals("Replaced", retrieved?.title)
    }
}
