package com.example.simpleclouddrive.feature.home

import com.example.simpleclouddrive.domain.model.CloudFile

data class HomeUiState(
    val nickname: String = "训练营用户",
    val fileCount: Int = 0,
    val usedBytes: Long = 0L,
    val recentTransfers: List<CloudFile> = emptyList(),
    val recentBrowses: List<CloudFile> = emptyList(),
    val isLoading: Boolean = true
)
