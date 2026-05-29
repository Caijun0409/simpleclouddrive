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
    private val _dialogState = MutableStateFlow<FileDialogState>(FileDialogState.None)
    private val _message = MutableSharedFlow<String>()

    val dialogState: StateFlow<FileDialogState> = _dialogState
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

    fun showSharePlaceholder(file: CloudFile) {
        viewModelScope.launch {
            _message.emit("分享功能待实现：${file.name}")
        }
    }

    fun showRenameDialog(file: CloudFile) {
        _dialogState.value = FileDialogState.Rename(file)
    }

    fun updateRenameName(name: String) {
        val current = _dialogState.value
        if (current is FileDialogState.Rename) {
            _dialogState.value = current.copy(name = name)
        }
    }

    fun confirmRename() {
        val current = _dialogState.value as? FileDialogState.Rename ?: return
        viewModelScope.launch {
            runCatching {
                fileRepository.renameFile(
                    fileId = current.file.fileId,
                    newName = current.name
                )
            }.onSuccess {
                _dialogState.value = FileDialogState.None
                _message.emit("重命名成功")
            }.onFailure { throwable ->
                _message.emit(throwable.message ?: "重命名失败")
            }
        }
    }

    fun showDeleteDialog(file: CloudFile) {
        _dialogState.value = FileDialogState.Delete(file)
    }

    fun confirmDelete() {
        val current = _dialogState.value as? FileDialogState.Delete ?: return
        viewModelScope.launch {
            runCatching {
                fileRepository.deleteFile(current.file.fileId)
            }.onSuccess {
                _dialogState.value = FileDialogState.None
                _message.emit("删除成功")
            }.onFailure { throwable ->
                _message.emit(throwable.message ?: "删除失败")
            }
        }
    }

    fun showMoveDialog(file: CloudFile) {
        _dialogState.value = FileDialogState.Move(file = file)
        viewModelScope.launch {
            runCatching {
                fileRepository.getMoveTargetFolders(file.fileId)
            }.onSuccess { targetFolders ->
                val current = _dialogState.value
                if (current is FileDialogState.Move && current.file.fileId == file.fileId) {
                    _dialogState.value = current.copy(
                        targetFolders = targetFolders,
                        isLoading = false
                    )
                }
            }.onFailure { throwable ->
                _dialogState.value = FileDialogState.None
                _message.emit(throwable.message ?: "加载目标文件夹失败")
            }
        }
    }

    fun confirmMove(targetParentId: String?) {
        val current = _dialogState.value as? FileDialogState.Move ?: return
        viewModelScope.launch {
            runCatching {
                fileRepository.moveFile(
                    fileId = current.file.fileId,
                    targetParentId = targetParentId
                )
            }.onSuccess {
                _dialogState.value = FileDialogState.None
                _message.emit("移动成功")
            }.onFailure { throwable ->
                _message.emit(throwable.message ?: "移动失败")
            }
        }
    }

    fun dismissDialog() {
        _dialogState.value = FileDialogState.None
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
