package com.kotonosora.todolist.data.mapper

import com.kotonosora.todolist.data.database.TodoEntity
import com.kotonosora.todolist.domain.model.TodoItem

fun TodoEntity.toDomain(): TodoItem {
    return TodoItem(
        id = id,
        title = title,
        description = description,
        dueDate = dueDate,
        filePath = filePath,
        isCompleted = isCompleted,
        reminderTime = reminderTime
    )
}

fun TodoItem.toEntity(): TodoEntity {
    return TodoEntity(
        id = id,
        title = title,
        description = description,
        dueDate = dueDate,
        filePath = filePath,
        isCompleted = isCompleted,
        reminderTime = reminderTime
    )
}
