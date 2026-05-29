package com.example.simpleclouddrive.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.simpleclouddrive.domain.repository.FileRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(
    fileRepository: FileRepository
) : ViewModel() {
    val uiState: StateFlow<HomeUiState> = combine(
        fileRepository.observeFileCount(),
        fileRepository.observeUsedBytes(),
        fileRepository.observeRecentTransferFiles(limit = RECENT_LIMIT),
        fileRepository.observeRecentBrowseFiles(limit = RECENT_LIMIT)
    ) { fileCount, usedBytes, recentTransfers, recentBrowses ->
        HomeUiState(
            fileCount = fileCount,
            usedBytes = usedBytes,
            recentTransfers = recentTransfers,
            recentBrowses = recentBrowses,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

    private companion object {
        const val RECENT_LIMIT = 10
    }
}

class HomeViewModelFactory(
    private val fileRepository: FileRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(fileRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
