package com.kotonosora.todolist.domain.usecase

import com.kotonosora.todolist.domain.model.PdfBookmark
import com.kotonosora.todolist.domain.model.PdfReadingPosition
import com.kotonosora.todolist.domain.repository.PdfReaderRepository
import kotlinx.coroutines.flow.Flow

/**
 * PDF bounded context — application layer (DDD).
 *
 * ViewModels depend on [PdfUseCases], never on [PdfReaderRepository] directly.
 * Validation (blank path, negative page) lives here so every caller shares it.
 * Mirrors the [TaskUseCases] bundle pattern.
 */

class ObservePdfBookmarksUseCase(private val repository: PdfReaderRepository) {
    operator fun invoke(path: String): Flow<List<PdfBookmark>> {
        require(path.isNotBlank()) { "PDF path must not be blank" }
        return repository.bookmarks(path)
    }
}

class GetPdfReadingPositionUseCase(private val repository: PdfReaderRepository) {
    suspend operator fun invoke(path: String): PdfReadingPosition? {
        if (path.isBlank()) return null
        return repository.readingPositionOnce(path)
    }
}

class SavePdfReadingPositionUseCase(private val repository: PdfReaderRepository) {
    suspend operator fun invoke(path: String, pageIndex: Int) {
        if (path.isBlank()) return
        repository.saveReadingPosition(path, pageIndex)
    }
}

class AddPdfBookmarkUseCase(private val repository: PdfReaderRepository) {
    suspend operator fun invoke(path: String, pageIndex: Int, label: String): Long {
        require(path.isNotBlank()) { "PDF path must not be blank" }
        require(pageIndex >= 0) { "pageIndex must be >= 0" }
        return repository.addBookmark(path, pageIndex, label)
    }
}

class RemovePdfBookmarkUseCase(private val repository: PdfReaderRepository) {
    suspend operator fun invoke(id: Long) {
        repository.removeBookmark(id)
    }
}

class RemovePdfBookmarkAtUseCase(private val repository: PdfReaderRepository) {
    suspend operator fun invoke(path: String, pageIndex: Int) {
        if (path.isBlank() || pageIndex < 0) return
        repository.removeBookmarkAt(path, pageIndex)
    }
}

class DeletePdfStateForFileUseCase(private val repository: PdfReaderRepository) {
    suspend operator fun invoke(path: String) {
        if (path.isBlank()) return
        repository.deleteForFile(path)
    }
}

class GetTrackedPdfPathsUseCase(private val repository: PdfReaderRepository) {
    suspend operator fun invoke(): Set<String> = repository.trackedPaths()
}

/**
 * Drops reader state whose file no longer exists anywhere.
 * [fileExists] is supplied by the caller (ViewModel/data layer owns Context/SAF checks).
 */
class CleanupOrphanPdfStateUseCase(private val repository: PdfReaderRepository) {
    suspend operator fun invoke(
        vaultPaths: Set<String>,
        fileExists: (String) -> Boolean
    ): Int {
        var removed = 0
        for (pdfPath in repository.trackedPaths()) {
            if (!vaultPaths.contains(pdfPath) && !fileExists(pdfPath)) {
                repository.deleteForFile(pdfPath)
                removed++
            }
        }
        return removed
    }
}

data class PdfUseCases(
    val observeBookmarks: ObservePdfBookmarksUseCase,
    val getReadingPosition: GetPdfReadingPositionUseCase,
    val saveReadingPosition: SavePdfReadingPositionUseCase,
    val addBookmark: AddPdfBookmarkUseCase,
    val removeBookmark: RemovePdfBookmarkUseCase,
    val removeBookmarkAt: RemovePdfBookmarkAtUseCase,
    val deleteForFile: DeletePdfStateForFileUseCase,
    val trackedPaths: GetTrackedPdfPathsUseCase,
    val cleanupOrphans: CleanupOrphanPdfStateUseCase
) {
    companion object {
        fun from(repository: PdfReaderRepository): PdfUseCases = PdfUseCases(
            observeBookmarks = ObservePdfBookmarksUseCase(repository),
            getReadingPosition = GetPdfReadingPositionUseCase(repository),
            saveReadingPosition = SavePdfReadingPositionUseCase(repository),
            addBookmark = AddPdfBookmarkUseCase(repository),
            removeBookmark = RemovePdfBookmarkUseCase(repository),
            removeBookmarkAt = RemovePdfBookmarkAtUseCase(repository),
            deleteForFile = DeletePdfStateForFileUseCase(repository),
            trackedPaths = GetTrackedPdfPathsUseCase(repository),
            cleanupOrphans = CleanupOrphanPdfStateUseCase(repository)
        )
    }
}
