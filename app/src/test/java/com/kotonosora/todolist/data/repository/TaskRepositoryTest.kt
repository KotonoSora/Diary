package com.kotonosora.todolist.data.repository

import com.kotonosora.todolist.data.database.TaskDao
import com.kotonosora.todolist.data.database.TaskEntity
import com.kotonosora.todolist.data.file.AppFileManager
import com.kotonosora.todolist.domain.model.TaskItem
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class TaskRepositoryTest {

    private val taskDao: TaskDao = mockk(relaxed = true)
    private val appFileManager: AppFileManager = mockk(relaxed = true)
    private val repository = TaskRepositoryImpl(taskDao, appFileManager)

    @Test
    fun `insertTask should save directly to database`() = runTest {
        val task = TaskItem("id1", "Test task", null, null, null, false)

        coEvery { taskDao.insertTask(any()) } just Runs

        repository.insertTask(task)

        coVerify { taskDao.insertTask(any()) }
    }

    @Test
    fun `updateTask should update database`() = runTest {
        val task = TaskItem("id1", "Updated Title", null, null, null, true)

        coEvery { taskDao.updateTask(any()) } just Runs

        repository.updateTask(task)

        coVerify { taskDao.updateTask(any()) }
    }

    @Test
    fun `deleteTask should delete database entry`() = runTest {
        val task = TaskItem("id1", "Title", null, null, null, false)

        coEvery { taskDao.deleteTask(any()) } just Runs

        repository.deleteTask(task)

        coVerify { taskDao.deleteTask(any()) }
    }

    @Test
    fun `getAllTasks should return mapped domain objects`() = runTest {
        val entity = TaskEntity("id1", "Entity Title", null, null, null, false, null)
        every { taskDao.getAllTasks() } returns flowOf(listOf(entity))

        val result = mutableListOf<List<TaskItem>>()
        repository.getAllTasks().collect { result.add(it) }

        assert(result.isNotEmpty())
        assert(result[0][0].title == "Entity Title")
    }
}
