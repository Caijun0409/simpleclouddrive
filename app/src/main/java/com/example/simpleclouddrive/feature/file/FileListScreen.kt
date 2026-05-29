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
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
    val dialogState by viewModel.dialogState.collectAsStateWithLifecycle()
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
                arrayOf("text/plain", "application/octet-stream", "video/*", "*/*")
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
        onShareClick = viewModel::showSharePlaceholder,
        onRenameClick = viewModel::showRenameDialog,
        onMoveClick = viewModel::showMoveDialog,
        onDeleteClick = viewModel::showDeleteDialog,
        modifier = modifier
    )

    FileOperationDialog(
        dialogState = dialogState,
        onRenameNameChange = viewModel::updateRenameName,
        onConfirmRename = viewModel::confirmRename,
        onConfirmDelete = viewModel::confirmDelete,
        onConfirmMove = viewModel::confirmMove,
        onDismiss = viewModel::dismissDialog
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
    onShareClick: (CloudFile) -> Unit,
    onRenameClick: (CloudFile) -> Unit,
    onMoveClick: (CloudFile) -> Unit,
    onDeleteClick: (CloudFile) -> Unit,
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
                    onVideoClick = onVideoClick,
                    onShareClick = onShareClick,
                    onRenameClick = onRenameClick,
                    onMoveClick = onMoveClick,
                    onDeleteClick = onDeleteClick
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
private fun FileOperationDialog(
    dialogState: FileDialogState,
    onRenameNameChange: (String) -> Unit,
    onConfirmRename: () -> Unit,
    onConfirmDelete: () -> Unit,
    onConfirmMove: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    when (dialogState) {
        FileDialogState.None -> Unit
        is FileDialogState.Rename -> RenameDialog(
            dialogState = dialogState,
            onNameChange = onRenameNameChange,
            onConfirm = onConfirmRename,
            onDismiss = onDismiss
        )
        is FileDialogState.Delete -> DeleteDialog(
            file = dialogState.file,
            onConfirm = onConfirmDelete,
            onDismiss = onDismiss
        )
        is FileDialogState.Move -> MoveDialog(
            dialogState = dialogState,
            onConfirmMove = onConfirmMove,
            onDismiss = onDismiss
        )
    }
}

@Composable
private fun RenameDialog(
    dialogState: FileDialogState.Rename,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("重命名") },
        text = {
            OutlinedTextField(
                value = dialogState.name,
                onValueChange = onNameChange,
                singleLine = true,
                label = { Text("新名称") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun DeleteDialog(
    file: CloudFile,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("删除") },
        text = {
            Text(
                text = if (file.type == FileType.FOLDER) {
                    "确定删除“${file.name}”及其中所有文件吗？"
                } else {
                    "确定删除“${file.name}”吗？"
                }
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("删除")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun MoveDialog(
    dialogState: FileDialogState.Move,
    onConfirmMove: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("移动到") },
        text = {
            if (dialogState.isLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn {
                    item {
                        MoveTargetItem(
                            name = "根目录",
                            enabled = dialogState.file.parentId != null,
                            onClick = { onConfirmMove(null) }
                        )
                    }
                    items(
                        items = dialogState.targetFolders,
                        key = { folder -> folder.fileId }
                    ) { folder ->
                        MoveTargetItem(
                            name = folder.name,
                            enabled = dialogState.file.parentId != folder.fileId,
                            onClick = { onConfirmMove(folder.fileId) }
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun MoveTargetItem(
    name: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier.clickable(
            enabled = enabled,
            onClick = onClick
        ),
        leadingContent = {
            Icon(
                imageVector = Icons.Outlined.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        headlineContent = {
            Text(
                text = name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    )
}

@Composable
private fun FileListContent(
    files: List<CloudFile>,
    onFolderClick: (CloudFile) -> Unit,
    onTxtClick: (CloudFile) -> Unit,
    onVideoClick: (CloudFile) -> Unit,
    onShareClick: (CloudFile) -> Unit,
    onRenameClick: (CloudFile) -> Unit,
    onMoveClick: (CloudFile) -> Unit,
    onDeleteClick: (CloudFile) -> Unit
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
                },
                onShareClick = { onShareClick(file) },
                onRenameClick = { onRenameClick(file) },
                onMoveClick = { onMoveClick(file) },
                onDeleteClick = { onDeleteClick(file) }
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun FileListItem(
    file: CloudFile,
    onClick: () -> Unit,
    onShareClick: () -> Unit,
    onRenameClick: () -> Unit,
    onMoveClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var menuExpanded by remember(file.fileId) { mutableStateOf(false) }

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
        },
        trailingContent = {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = "更多操作"
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("分享") },
                    onClick = {
                        menuExpanded = false
                        onShareClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text("重命名") },
                    onClick = {
                        menuExpanded = false
                        onRenameClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text("移动") },
                    onClick = {
                        menuExpanded = false
                        onMoveClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text("删除") },
                    onClick = {
                        menuExpanded = false
                        onDeleteClick()
                    }
                )
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
