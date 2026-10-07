package com.kotonosora.todolist.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PdfReaderDao {

    @Query("SELECT * FROM pdf_reading_state WHERE filePath = :path")
    fun readingState(path: String): Flow<PdfReadingStateEntity?>

    @Query("SELECT * FROM pdf_reading_state WHERE filePath = :path")
    suspend fun readingStateOnce(path: String): PdfReadingStateEntity?

    @Upsert
    suspend fun saveReadingState(state: PdfReadingStateEntity)

    @Query("SELECT * FROM pdf_bookmarks WHERE filePath = :path ORDER BY pageIndex ASC")
    fun bookmarks(path: String): Flow<List<PdfBookmarkEntity>>

    @Insert
    suspend fun addBookmark(bookmark: PdfBookmarkEntity): Long

    /**
     * One bookmark per page: atomic delete-then-insert so a double-tap can
     * never leave duplicate rows for the same page (enforced by the unique
     * index as well).
     */
    @Transaction
    suspend fun replaceBookmarkForPage(bookmark: PdfBookmarkEntity): Long {
        deleteBookmarkAt(bookmark.filePath, bookmark.pageIndex)
        return addBookmark(bookmark)
    }

    @Query("DELETE FROM pdf_bookmarks WHERE id = :id")
    suspend fun deleteBookmark(id: Long)

    @Query("DELETE FROM pdf_bookmarks WHERE filePath = :path AND pageIndex = :pageIndex")
    suspend fun deleteBookmarkAt(path: String, pageIndex: Int)

    @Query("DELETE FROM pdf_bookmarks WHERE filePath = :path")
    suspend fun deleteBookmarksForFile(path: String)

    @Query("DELETE FROM pdf_reading_state WHERE filePath = :path")
    suspend fun deleteReadingState(path: String)

    @Query("SELECT DISTINCT filePath FROM pdf_bookmarks")
    suspend fun bookmarkedPaths(): List<String>

    @Query("SELECT filePath FROM pdf_reading_state")
    suspend fun readingStatePaths(): List<String>
}
