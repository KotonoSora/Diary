package com.kotonosora.todolist.feature.media

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotonosora.todolist.data.database.PdfBookmarkEntity
import com.kotonosora.todolist.data.database.PdfReaderDao
import com.kotonosora.todolist.data.database.PdfReadingStateEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Reader state for one PDF ([filePath] is the vault path or SAF uri string,
 * same key the reader opens): named bookmarks plus the auto-saved resume
 * position. One instance per open document (keyed `pdfReader/<path>`).
 */
class PdfReaderViewModel(
    private val filePath: String,
    private val dao: PdfReaderDao?
) : ViewModel() {

    val bookmarks: StateFlow<List<PdfBookmarkEntity>> = if (dao != null) {
        dao.bookmarks(filePath)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    } else {
        MutableStateFlow<List<PdfBookmarkEntity>>(emptyList()).asStateFlow()
    }

    /** True once the saved position (or its absence) has loaded. */
    private val _positionLoaded = MutableStateFlow(false)
    val positionLoaded: StateFlow<Boolean> = _positionLoaded.asStateFlow()

    private val _savedPage = MutableStateFlow<Int?>(null)
    val savedPage: StateFlow<Int?> = _savedPage.asStateFlow()

    init {
        if (dao != null) {
            viewModelScope.launch {
                try {
                    _savedPage.value = dao.readingStateOnce(filePath)?.lastPageIndex
                } catch (_: Exception) {
                }
                _positionLoaded.value = true
            }
        } else {
            _positionLoaded.value = true
        }
    }

    fun savePage(pageIndex: Int) {
        val dao = dao ?: return
        viewModelScope.launch {
            try {
                dao.saveReadingState(
                    PdfReadingStateEntity(
                        filePath = filePath,
                        lastPageIndex = pageIndex.coerceAtLeast(0)
                    )
                )
            } catch (_: Exception) {
            }
        }
    }

    fun addBookmark(pageIndex: Int, label: String) {
        val dao = dao ?: return
        val trimmed = label.trim().take(80).ifBlank { "Page ${pageIndex + 1}" }
        viewModelScope.launch {
            try {
                dao.replaceBookmarkForPage(
                    PdfBookmarkEntity(
                        filePath = filePath,
                        pageIndex = pageIndex,
                        label = trimmed
                    )
                )
            } catch (_: Exception) {
            }
        }
    }

    fun removeBookmark(id: Long) {
        val dao = dao ?: return
        viewModelScope.launch {
            try {
                dao.deleteBookmark(id)
            } catch (_: Exception) {
            }
        }
    }

    fun removeBookmarkAt(pageIndex: Int) {
        val dao = dao ?: return
        viewModelScope.launch {
            try {
                dao.deleteBookmarkAt(filePath, pageIndex)
            } catch (_: Exception) {
            }
        }
    }
}
