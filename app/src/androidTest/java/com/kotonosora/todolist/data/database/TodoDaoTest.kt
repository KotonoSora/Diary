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
class TodoDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var todoDao: TodoDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        todoDao = database.todoDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertTodo_andRetrieveById() = runTest {
        val entity = TodoEntity(id = "1", title = "Test", description = null,
            dueDate = null, filePath = null, isCompleted = false)
        todoDao.insertTodo(entity)

        val retrieved = todoDao.getTodoById("1")
        assertNotNull(retrieved)
        assertEquals("Test", retrieved?.title)
    }

    @Test
    fun getAllTodos_returnsInsertedEntities() = runTest {
        val e1 = TodoEntity(id = "a", title = "Alpha", description = null,
            dueDate = 1000L, filePath = null, isCompleted = false)
        val e2 = TodoEntity(id = "b", title = "Beta", description = null,
            dueDate = 2000L, filePath = null, isCompleted = true)
        todoDao.insertTodo(e1)
        todoDao.insertTodo(e2)

        val todos = todoDao.getAllTodos().first()
        assertEquals(2, todos.size)
    }

    @Test
    fun getAllTodosOnce_returnsAllEntities() = runTest {
        val e1 = TodoEntity(id = "x", title = "X", description = null,
            dueDate = null, filePath = null, isCompleted = false)
        todoDao.insertTodo(e1)

        val result = todoDao.getAllTodosOnce()
        assertEquals(1, result.size)
        assertEquals("x", result[0].id)
    }

    @Test
    fun updateTodo_changesValues() = runTest {
        val entity = TodoEntity(id = "2", title = "Original", description = null,
            dueDate = null, filePath = null, isCompleted = false)
        todoDao.insertTodo(entity)

        val updated = entity.copy(title = "Updated", isCompleted = true)
        todoDao.updateTodo(updated)

        val retrieved = todoDao.getTodoById("2")
        assertEquals("Updated", retrieved?.title)
        assertEquals(true, retrieved?.isCompleted)
    }

    @Test
    fun deleteTodo_removesEntity() = runTest {
        val entity = TodoEntity(id = "3", title = "ToDelete", description = null,
            dueDate = null, filePath = null, isCompleted = false)
        todoDao.insertTodo(entity)
        todoDao.deleteTodo(entity)

        val retrieved = todoDao.getTodoById("3")
        assertNull(retrieved)
    }

    @Test
    fun getTodosByDate_returnsOnlyMatchingRange() = runTest {
        val inRange = TodoEntity(id = "r1", title = "In Range", description = null,
            dueDate = 5000L, filePath = null, isCompleted = false)
        val outOfRange = TodoEntity(id = "r2", title = "Out of Range", description = null,
            dueDate = 10_000L, filePath = null, isCompleted = false)
        todoDao.insertTodo(inRange)
        todoDao.insertTodo(outOfRange)

        val results = todoDao.getTodosByDate(1000L, 7000L).first()
        assertEquals(1, results.size)
        assertEquals("r1", results[0].id)
    }

    @Test
    fun insertTodo_withSameId_replacesExisting() = runTest {
        val original = TodoEntity(id = "dup", title = "Original", description = null,
            dueDate = null, filePath = null, isCompleted = false)
        val replacement = original.copy(title = "Replaced")
        todoDao.insertTodo(original)
        todoDao.insertTodo(replacement)

        val retrieved = todoDao.getTodoById("dup")
        assertEquals("Replaced", retrieved?.title)
    }
}

