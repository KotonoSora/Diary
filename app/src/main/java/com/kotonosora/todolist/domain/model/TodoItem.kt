package com.kotonosora.todolist.domain.model

data class TodoItem(
    val id: String,
    val title: String,
    val description: String?,
    val dueDate: Long?,
    val filePath: String?,
    val isCompleted: Boolean,
    val reminderTime: Long? = null
)
