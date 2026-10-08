package com.kotonosora.todolist.data.repository

import com.kotonosora.todolist.data.database.PdfReaderDao
import com.kotonosora.todolist.data.mapper.toDomain
import com.kotonosora.todolist.data.mapper.toEntity
import com.kotonosora.todolist.domain.model.PdfBookmark
import com.kotonosora.todolist.domain.model.PdfReadingPosition
import com.kotonosora.todolist.domain.model.pdfBookmarkLabel
import com.kotonosora.todolist.domain.repository.PdfReaderRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class PdfReaderRepositoryImpl(
    private val pdfReaderDao: PdfReaderDao
) : PdfReaderRepository {

    override fun bookmarks(path: String): Flow<List<PdfBookmark>> {
        return pdfReaderDao.bookmarks(path).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun readingPositionOnce(path: String): PdfReadingPosition? {
        return pdfReaderDao.readingStateOnce(path)?.toDomain()
    }

    override suspend fun saveReadingPosition(path: String, pageIndex: Int) =
        withContext(Dispatchers.IO) {
            pdfReaderDao.saveReadingState(
                PdfReadingPosition(
                    filePath = path,
                    lastPageIndex = pageIndex.coerceAtLeast(0)
                ).toEntity()
            )
        }

    override suspend fun addBookmark(path: String, pageIndex: Int, label: String): Long =
        withContext(Dispatchers.IO) {
            pdfReaderDao.replaceBookmarkForPage(
                PdfBookmark(
                    filePath = path,
                    pageIndex = pageIndex,
                    label = pdfBookmarkLabel(pageIndex, label)
                ).toEntity()
            )
        }

    override suspend fun removeBookmark(id: Long) = withContext(Dispatchers.IO) {
        pdfReaderDao.deleteBookmark(id)
    }

    override suspend fun removeBookmarkAt(path: String, pageIndex: Int) =
        withContext(Dispatchers.IO) {
            pdfReaderDao.deleteBookmarkAt(path, pageIndex)
        }

    override suspend fun deleteForFile(path: String) = withContext(Dispatchers.IO) {
        pdfReaderDao.deleteBookmarksForFile(path)
        pdfReaderDao.deleteReadingState(path)
    }

    override suspend fun trackedPaths(): Set<String> = withContext(Dispatchers.IO) {
        (pdfReaderDao.bookmarkedPaths() + pdfReaderDao.readingStatePaths()).toSet()
    }
}
