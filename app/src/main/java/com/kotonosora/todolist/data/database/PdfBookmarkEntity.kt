package com.kotonosora.todolist.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One named bookmark inside a PDF: 0-based [pageIndex] plus a user label
 * (defaults to "Page N"). Jump targets for the reader's bookmark list.
 */
@Entity(
    tableName = "pdf_bookmarks",
    indices = [Index(value = ["filePath", "pageIndex"], unique = true)]
)
data class PdfBookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val filePath: String,
    val pageIndex: Int,
    val label: String,
    val createdAt: Long = System.currentTimeMillis()
)
