package com.kotonosora.todolist.data.mapper

import com.kotonosora.todolist.data.database.PdfBookmarkEntity
import com.kotonosora.todolist.data.database.PdfReadingStateEntity
import com.kotonosora.todolist.domain.model.PdfBookmark
import com.kotonosora.todolist.domain.model.PdfReadingPosition

fun PdfBookmarkEntity.toDomain(): PdfBookmark {
    return PdfBookmark(
        id = id,
        filePath = filePath,
        pageIndex = pageIndex,
        label = label,
        createdAt = createdAt
    )
}

fun PdfBookmark.toEntity(): PdfBookmarkEntity {
    return PdfBookmarkEntity(
        id = id,
        filePath = filePath,
        pageIndex = pageIndex,
        label = label,
        createdAt = createdAt
    )
}

fun PdfReadingStateEntity.toDomain(): PdfReadingPosition {
    return PdfReadingPosition(
        filePath = filePath,
        lastPageIndex = lastPageIndex,
        updatedAt = updatedAt
    )
}

fun PdfReadingPosition.toEntity(): PdfReadingStateEntity {
    return PdfReadingStateEntity(
        filePath = filePath,
        lastPageIndex = lastPageIndex,
        updatedAt = updatedAt
    )
}
