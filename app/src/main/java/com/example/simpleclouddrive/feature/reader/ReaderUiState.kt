package com.example.simpleclouddrive.feature.reader

data class ReaderUiState(
    val title: String = "",
    val pages: List<String> = emptyList(),
    val currentPageIndex: Int = 0,
    val isLoading: Boolean = true,
    val isPaginating: Boolean = false,
    val errorMessage: String? = null
) {
    val currentPageText: String
        get() = pages.getOrNull(currentPageIndex).orEmpty()

    val currentPageNumber: Int
        get() = if (pages.isEmpty()) 0 else currentPageIndex + 1

    val totalPages: Int
        get() = pages.size
}
