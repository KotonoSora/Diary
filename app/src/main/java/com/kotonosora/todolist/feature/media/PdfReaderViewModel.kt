package com.kotonosora.todolist.feature.media

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotonosora.todolist.domain.model.PdfBookmark
import com.kotonosora.todolist.domain.usecase.PdfUseCases
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
 *
 * DDD: depends on [PdfUseCases] (application layer), never on the DAO or
 * entities directly.
 */
class PdfReaderViewModel(
    private val filePath: String,
    private val useCases: PdfUseCases
) : ViewModel() {

    val bookmarks: StateFlow<List<PdfBookmark>> = useCases.observeBookmarks(filePath)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** True once the saved position (or its absence) has loaded. */
    private val _positionLoaded = MutableStateFlow(false)
    val positionLoaded: StateFlow<Boolean> = _positionLoaded.asStateFlow()

    private val _savedPage = MutableStateFlow<Int?>(null)
    val savedPage: StateFlow<Int?> = _savedPage.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                _savedPage.value = useCases.getReadingPosition(filePath)?.lastPageIndex
            } catch (_: Exception) {
            }
            _positionLoaded.value = true
        }
    }

    fun savePage(pageIndex: Int) {
        viewModelScope.launch {
            try {
                useCases.saveReadingPosition(filePath, pageIndex)
            } catch (_: Exception) {
            }
        }
    }

    fun addBookmark(pageIndex: Int, label: String) {
        viewModelScope.launch {
            try {
                useCases.addBookmark(filePath, pageIndex, label)
            } catch (_: Exception) {
            }
        }
    }

    fun removeBookmark(id: Long) {
        viewModelScope.launch {
            try {
                useCases.removeBookmark(id)
            } catch (_: Exception) {
            }
        }
    }

    fun removeBookmarkAt(pageIndex: Int) {
        viewModelScope.launch {
            try {
                useCases.removeBookmarkAt(filePath, pageIndex)
            } catch (_: Exception) {
            }
        }
    }
}
