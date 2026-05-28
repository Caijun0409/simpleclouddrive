package com.example.simpleclouddrive.feature.file

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.simpleclouddrive.core.ui.component.EmptyView
import com.example.simpleclouddrive.core.ui.component.ErrorView
import com.example.simpleclouddrive.core.ui.component.LoadingView
import com.example.simpleclouddrive.domain.model.CloudFile
import com.example.simpleclouddrive.domain.model.FileType
import com.example.simpleclouddrive.domain.repository.FileRepository
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

@Composable
fun FileListRoute(
    fileRepository: FileRepository,
    onOpenReader: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: FileListViewModel = viewModel(
        factory = FileListViewModelFactory(fileRepository)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.uploadFile(uri)
            }
        }
    )

    BackHandler(enabled = uiState.currentFolderId != null) {
        viewModel.navigateUp()
    }

    LaunchedEffect(viewModel) {
        viewModel.message.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    FileListScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onUploadClick = {
            openDocumentLauncher.launch(
                arrayOf("text/plain", "video/*", "*/*")
            )
        },
        onNavigateUp = { viewModel.navigateUp() },
        onFolderClick = viewModel::openFolder,
        onTxtClick = { file -> onOpenReader(file.fileId) },
        onVideoClick = {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("准备打开视频")
            }
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileListScreen(
    uiState: FileUiState,
    snackbarHostState: SnackbarHostState,
    onUploadClick: () -> Unit,
    onNavigateUp: () -> Unit,
    onFolderClick: (CloudFile) -> Unit,
    onTxtClick: (CloudFile) -> Unit,
    onVideoClick: (CloudFile) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.breadcrumb.lastOrNull()?.name ?: "文件列表",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    if (uiState.currentFolderId != null) {
                        IconButton(onClick = onNavigateUp) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "返回上一级"
                            )
                        }
                    }
                },
                actions = {
                    TextButton(onClick = onUploadClick) {
                        Icon(
                            imageVector = Icons.Outlined.UploadFile,
                            contentDescription = null
                        )
                        Text(text = "上传")
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            BreadcrumbRow(
                breadcrumb = uiState.breadcrumb,
                modifier = Modifier.fillMaxWidth()
            )

            when {
                uiState.isLoading -> LoadingView()
                uiState.errorMessage != null -> ErrorView(message = uiState.errorMessage)
                uiState.files.isEmpty() -> EmptyView(message = "当前文件夹为空")
                else -> FileListContent(
                    files = uiState.files,
                    onFolderClick = onFolderClick,
                    onTxtClick = onTxtClick,
                    onVideoClick = onVideoClick
                )
            }
        }
    }
}

@Composable
private fun BreadcrumbRow(
    breadcrumb: List<CloudFile>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "根目录",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        breadcrumb.forEach { folder ->
            Text(
                text = "/",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = folder.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FileListContent(
    files: List<CloudFile>,
    onFolderClick: (CloudFile) -> Unit,
    onTxtClick: (CloudFile) -> Unit,
    onVideoClick: (CloudFile) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            items = files,
            key = { file -> file.fileId }
        ) { file ->
            FileListItem(
                file = file,
                onClick = {
                    when (file.type) {
                        FileType.FOLDER -> onFolderClick(file)
                        FileType.TXT -> onTxtClick(file)
                        FileType.VIDEO -> onVideoClick(file)
                        FileType.OTHER -> Unit
                    }
                }
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun FileListItem(
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
        },
        supportingContent = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = formatFileSize(file),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatModifiedTime(file.modifiedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    )
}

private fun formatFileSize(file: CloudFile): String {
    if (file.type == FileType.FOLDER) {
        return "文件夹"
    }

    val size = file.size.toDouble()
    return when {
        size < 1024 -> "${file.size} B"
        size < 1024 * 1024 -> "%.1f KB".format(size / 1024)
        size < 1024 * 1024 * 1024 -> "%.1f MB".format(size / 1024 / 1024)
        else -> "%.1f GB".format(size / 1024 / 1024 / 1024)
    }
}

private fun formatModifiedTime(timestamp: Long): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    return Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .format(formatter)
}
