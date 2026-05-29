package com.example.simpleclouddrive.feature.file

import com.example.simpleclouddrive.domain.model.CloudFile

data class FileUiState(
    val currentFolderId: String? = null,
    val breadcrumb: List<CloudFile> = emptyList(),
    val files: List<CloudFile> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

sealed interface FileDialogState {
    data object None : FileDialogState

    data class Rename(
        val file: CloudFile,
        val name: String = file.name
    ) : FileDialogState

    data class Delete(
        val file: CloudFile
    ) : FileDialogState

    data class Move(
        val file: CloudFile,
        val targetFolders: List<CloudFile> = emptyList(),
        val isLoading: Boolean = true
    ) : FileDialogState
}
