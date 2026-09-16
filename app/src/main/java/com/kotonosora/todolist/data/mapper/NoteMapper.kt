package com.kotonosora.todolist.data.mapper

import com.kotonosora.todolist.data.database.NoteEntity
import com.kotonosora.todolist.domain.model.NoteItem
import com.kotonosora.todolist.domain.model.NoteType

fun NoteEntity.toDomainModel(): NoteItem {
    val parsedNoteType = try {
        NoteType.valueOf(noteType)
    } catch (_: Throwable) {
        NoteType.PERMANENT
    }

    return NoteItem(
        id = relativePath,
        uid = uid,
        title = title,
        noteType = parsedNoteType,
        relativePath = relativePath,
        content = content,
        fileFormat = fileFormat,
        updatedAt = updatedAt,
        sizeBytes = sizeBytes
    )
}

fun NoteItem.toEntity(): NoteEntity {
    return NoteEntity(
        id = id,
        uid = uid,
        title = title,
        noteType = noteType.name,
        relativePath = relativePath,
        content = content,
        fileFormat = fileFormat,
        updatedAt = updatedAt,
        sizeBytes = sizeBytes
    )
}
