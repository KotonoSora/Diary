package com.kotonosora.todolist.data.mapper

import com.kotonosora.todolist.data.database.TaskEntity
import com.kotonosora.todolist.domain.model.TaskItem

fun TaskEntity.toDomain(): TaskItem {
    return TaskItem(
        id = id,
        title = title,
        description = description,
        dueDate = dueDate,
        filePath = filePath,
        isCompleted = isCompleted,
        reminderTime = reminderTime,
        fileFormat = fileFormat
    )
}

fun TaskItem.toEntity(): TaskEntity {
    return TaskEntity(
        id = id,
        title = title,
        description = description,
        dueDate = dueDate,
        filePath = filePath,
        isCompleted = isCompleted,
        reminderTime = reminderTime,
        fileFormat = fileFormat
    )
}
