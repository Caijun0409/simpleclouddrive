package com.example.simpleclouddrive.feature.share

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.simpleclouddrive.domain.repository.FileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ShareViewModel(
    private val shareId: String,
    private val fileRepository: FileRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ShareUiState())

    val uiState: StateFlow<ShareUiState> = _uiState

    init {
        loadShare()
    }

    private fun loadShare() {
        viewModelScope.launch {
            _uiState.value = ShareUiState(isLoading = true)
            runCatching {
                fileRepository.getSharedFiles(shareId)
            }.onSuccess { files ->
                _uiState.value = ShareUiState(
                    files = files,
                    isLoading = false,
                    errorMessage = if (files.isEmpty()) "分享文件不存在" else null
                )
            }.onFailure { throwable ->
                _uiState.value = ShareUiState(
                    isLoading = false,
                    errorMessage = throwable.message ?: "分享链接无效"
                )
            }
        }
    }
}

class ShareViewModelFactory(
    private val shareId: String,
    private val fileRepository: FileRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShareViewModel::class.java)) {
            return ShareViewModel(
                shareId = shareId,
                fileRepository = fileRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
