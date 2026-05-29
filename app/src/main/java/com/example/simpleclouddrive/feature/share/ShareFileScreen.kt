package com.example.simpleclouddrive.feature.share

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.simpleclouddrive.core.ui.component.EmptyView
import com.example.simpleclouddrive.core.ui.component.ErrorView
import com.example.simpleclouddrive.core.ui.component.LoadingView
import com.example.simpleclouddrive.domain.model.CloudFile
import com.example.simpleclouddrive.domain.model.FileType
import com.example.simpleclouddrive.domain.repository.FileRepository
import kotlinx.coroutines.launch

@Composable
fun ShareFileScreen(
    shareId: String,
    fileRepository: FileRepository,
    onOpenReader: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: ShareViewModel = viewModel(
        key = shareId,
        factory = ShareViewModelFactory(
            shareId = shareId,
            fileRepository = fileRepository
        )
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    ShareFileScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onFileClick = { file ->
            when (file.type) {
                FileType.TXT -> onOpenReader(file.fileId)
                FileType.VIDEO -> coroutineScope.launch {
                    snackbarHostState.showSnackbar("视频打开功能待实现")
                }
                FileType.FOLDER -> coroutineScope.launch {
                    snackbarHostState.showSnackbar("暂不支持从分享页直接进入文件夹")
                }
                FileType.OTHER -> Unit
            }
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShareFileScreen(
    uiState: ShareUiState,
    snackbarHostState: SnackbarHostState,
    onFileClick: (CloudFile) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("分享文件") }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> LoadingView(modifier = Modifier.fillMaxSize())
            uiState.errorMessage != null -> ErrorView(message = uiState.errorMessage)
            uiState.files.isEmpty() -> EmptyView(message = "暂无分享文件")
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = innerPadding
            ) {
                items(
                    items = uiState.files,
                    key = { file -> file.fileId }
                ) { file ->
                    ShareFileItem(
                        file = file,
                        onClick = { onFileClick(file) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun ShareFileItem(
    file: CloudFile,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = {
            Icon(
                imageVector = when (file.type) {
                    FileType.FOLDER -> Icons.Outlined.Folder
                    FileType.TXT -> Icons.Outlined.Description
                    FileType.VIDEO -> Icons.Outlined.Videocam
                    FileType.OTHER -> Icons.AutoMirrored.Outlined.InsertDriveFile
                },
                contentDescription = file.type.name,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        headlineContent = {
            Text(
                text = file.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    )
}
