package com.kotonosora.todolist.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Last-read position of one PDF (vault path or SAF uri string). One row per
 * file — reopening the reader resumes where the user left off.
 */
@Entity(tableName = "pdf_reading_state")
data class PdfReadingStateEntity(
    @PrimaryKey val filePath: String,
    val lastPageIndex: Int,
    val updatedAt: Long = System.currentTimeMillis()
)
