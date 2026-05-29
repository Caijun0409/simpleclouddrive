package com.example.simpleclouddrive.feature.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.simpleclouddrive.domain.repository.FileRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReaderViewModel(
    private val fileId: String,
    private val fileRepository: FileRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReaderUiState())
    private var rawText: String? = null

    val uiState: StateFlow<ReaderUiState> = _uiState

    init {
        loadFile()
    }

    fun paginate(
        widthPx: Int,
        heightPx: Int,
        fontSizePx: Float,
        lineHeightPx: Float
    ) {
        val text = rawText ?: return
        if (widthPx <= 0 || heightPx <= 0) {
            return
        }

        viewModelScope.launch {
            _uiState.update { state -> state.copy(isPaginating = true) }
            val pages = withContext(ioDispatcher) {
                TxtPaginator.paginate(
                    text = text,
                    widthPx = widthPx,
                    heightPx = heightPx,
                    fontSizePx = fontSizePx,
                    lineHeightPx = lineHeightPx
                )
            }
            _uiState.update { state ->
                state.copy(
                    pages = pages,
                    currentPageIndex = state.currentPageIndex.coerceInPageRange(pages),
                    isLoading = false,
                    isPaginating = false,
                    errorMessage = null
                )
            }
        }
    }

    fun nextPage() {
        _uiState.update { state ->
            state.copy(
                currentPageIndex = (state.currentPageIndex + 1)
                    .coerceAtMost((state.pages.size - 1).coerceAtLeast(0))
            )
        }
    }

    fun previousPage() {
        _uiState.update { state ->
            state.copy(currentPageIndex = (state.currentPageIndex - 1).coerceAtLeast(0))
        }
    }

    private fun loadFile() {
        viewModelScope.launch {
            _uiState.value = ReaderUiState(isLoading = true)
            runCatching {
                val file = fileRepository.getFileById(fileId) ?: error("文件不存在")
                val text = fileRepository.readTextFile(fileId)
                file.name to text
            }.onSuccess { (title, text) ->
                rawText = text
                _uiState.value = ReaderUiState(
                    title = title,
                    isLoading = false
                )
            }.onFailure { throwable ->
                _uiState.value = ReaderUiState(
                    isLoading = false,
                    errorMessage = throwable.message ?: "读取文件失败"
                )
            }
        }
    }

    private fun Int.coerceInPageRange(pages: List<String>): Int {
        return if (pages.isEmpty()) {
            0
        } else {
            coerceIn(0, pages.lastIndex)
        }
    }
}

class ReaderViewModelFactory(
    private val fileId: String,
    private val fileRepository: FileRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReaderViewModel::class.java)) {
            return ReaderViewModel(
                fileId = fileId,
                fileRepository = fileRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
