package com.example.simpleclouddrive.feature.share

import com.example.simpleclouddrive.domain.model.CloudFile

data class ShareUiState(
    val files: List<CloudFile> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)
