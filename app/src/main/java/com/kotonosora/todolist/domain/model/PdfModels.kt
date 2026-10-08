package com.kotonosora.todolist.domain.model

/**
 * Domain model for one named bookmark inside a PDF: 0-based [pageIndex] plus
 * a user label (defaults to "Page N"). Persistence lives in `pdf_bookmarks`;
 * this type is what the reader UI and ViewModels operate on.
 */
data class PdfBookmark(
    val id: Long = 0,
    val filePath: String,
    val pageIndex: Int,
    val label: String,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Last-read position of one PDF (vault path or SAF uri string). One per file —
 * reopening the reader resumes where the user left off.
 */
data class PdfReadingPosition(
    val filePath: String,
    val lastPageIndex: Int,
    val updatedAt: Long = System.currentTimeMillis()
)

const val PDF_BOOKMARK_LABEL_MAX_LENGTH = 80

/**
 * Single source of truth for the bookmark label policy (previously inline in
 * the reader ViewModel): trimmed, capped, falling back to "Page N" on blank.
 */
fun pdfBookmarkLabel(pageIndex: Int, raw: String): String =
    raw.trim().take(PDF_BOOKMARK_LABEL_MAX_LENGTH).ifBlank { "Page ${pageIndex + 1}" }
