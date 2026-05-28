package com.example.simpleclouddrive.feature.file

import com.example.simpleclouddrive.domain.model.CloudFile

data class FileUiState(
    val currentFolderId: String? = null,
    val breadcrumb: List<CloudFile> = emptyList(),
    val files: List<CloudFile> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)
