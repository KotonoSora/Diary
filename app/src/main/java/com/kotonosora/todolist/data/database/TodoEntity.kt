package com.kotonosora.todolist.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "todo_items",
    indices = [
        Index(value = ["dueDate"]),
        Index(value = ["isCompleted"])
    ]
)
data class TodoEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String?,
    val dueDate: Long?,
    val filePath: String?,
    val isCompleted: Boolean = false,
    val reminderTime: Long? = null,
    val fileFormat: String = "md"
)
