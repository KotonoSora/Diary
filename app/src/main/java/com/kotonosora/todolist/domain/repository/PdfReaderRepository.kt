package com.kotonosora.todolist.domain.repository

import com.kotonosora.todolist.domain.model.PdfBookmark
import com.kotonosora.todolist.domain.model.PdfReadingPosition
import kotlinx.coroutines.flow.Flow

/**
 * Reader state for PDFs (named bookmarks + auto-saved resume position),
 * keyed by vault path or SAF uri string. Implemented by
 * `PdfReaderRepositoryImpl` over Room; ViewModels depend on this interface,
 * never on the DAO or entities directly.
 */
interface PdfReaderRepository {
    fun bookmarks(path: String): Flow<List<PdfBookmark>>

    suspend fun readingPositionOnce(path: String): PdfReadingPosition?

    suspend fun saveReadingPosition(path: String, pageIndex: Int)

    suspend fun addBookmark(path: String, pageIndex: Int, label: String): Long

    suspend fun removeBookmark(id: Long)

    suspend fun removeBookmarkAt(path: String, pageIndex: Int)

    /** Drops bookmarks + resume position for one file (called on file delete). */
    suspend fun deleteForFile(path: String)

    /** Every filePath tracked by either table — for orphan cleanup on vault sync. */
    suspend fun trackedPaths(): Set<String>
}
