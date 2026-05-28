package com.example.simpleclouddrive.feature.file

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.simpleclouddrive.domain.model.CloudFile
import com.example.simpleclouddrive.domain.repository.FileRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class FileListViewModel(
    private val fileRepository: FileRepository
) : ViewModel() {
    private val folderState = MutableStateFlow(FolderState())
    private val _message = MutableSharedFlow<String>()

    val message: SharedFlow<String> = _message

    val uiState: StateFlow<FileUiState> = folderState
        .flatMapLatest { folder ->
            fileRepository.observeFiles(folder.currentFolderId)
                .map { files ->
                    FileUiState(
                        currentFolderId = folder.currentFolderId,
                        breadcrumb = folder.breadcrumb,
                        files = files,
                        isLoading = false,
                        errorMessage = null
                    )
                }
                .onStart {
                    emit(
                        FileUiState(
                            currentFolderId = folder.currentFolderId,
                            breadcrumb = folder.breadcrumb,
                            isLoading = true
                        )
                    )
                }
                .catch { throwable ->
                    emit(
                        FileUiState(
                            currentFolderId = folder.currentFolderId,
                            breadcrumb = folder.breadcrumb,
                            isLoading = false,
                            errorMessage = throwable.message ?: "文件加载失败"
                        )
                    )
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FileUiState()
        )

    fun openFolder(folder: CloudFile) {
        folderState.update { current ->
            current.copy(
                currentFolderId = folder.fileId,
                breadcrumb = current.breadcrumb + folder
            )
        }
    }

    fun navigateUp(): Boolean {
        val current = folderState.value
        if (current.currentFolderId == null) {
            return false
        }

        val newBreadcrumb = current.breadcrumb.dropLast(1)
        folderState.value = FolderState(
            currentFolderId = newBreadcrumb.lastOrNull()?.fileId,
            breadcrumb = newBreadcrumb
        )
        return true
    }

    fun uploadFile(uri: Uri) {
        viewModelScope.launch {
            val parentId = folderState.value.currentFolderId
            runCatching {
                fileRepository.uploadLocalFile(uri = uri, parentId = parentId)
            }.onSuccess {
                _message.emit("上传成功")
            }.onFailure { throwable ->
                _message.emit(throwable.message ?: "上传失败")
            }
        }
    }

    private data class FolderState(
        val currentFolderId: String? = null,
        val breadcrumb: List<CloudFile> = emptyList()
    )
}

class FileListViewModelFactory(
    private val fileRepository: FileRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FileListViewModel::class.java)) {
            return FileListViewModel(fileRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
