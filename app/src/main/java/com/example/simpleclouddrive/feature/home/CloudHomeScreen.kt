package com.example.simpleclouddrive.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.simpleclouddrive.core.ui.component.LoadingView
import com.example.simpleclouddrive.core.util.VideoPlayerUtil
import com.example.simpleclouddrive.domain.model.CloudFile
import com.example.simpleclouddrive.domain.model.FileType
import com.example.simpleclouddrive.domain.repository.FileRepository
import kotlinx.coroutines.launch

@Composable
fun CloudHomeScreen(
    fileRepository: FileRepository,
    onOpenReader: (String) -> Unit,
    onOpenFilesRoot: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(fileRepository)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    CloudHomeScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onFileClick = { file ->
            when (file.type) {
                FileType.TXT -> onOpenReader(file.fileId)
                FileType.VIDEO -> coroutineScope.launch {
                    runCatching {
                        fileRepository.markFileBrowsed(file.fileId)
                    }.onFailure { throwable ->
                        snackbarHostState.showSnackbar(throwable.message ?: "打开视频失败")
                        return@launch
                    }
                    VideoPlayerUtil.openVideo(context, file)
                        .onFailure { throwable ->
                            snackbarHostState.showSnackbar(throwable.message ?: "打开视频失败")
                        }
                }
                FileType.FOLDER -> {
                    onOpenFilesRoot()
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("已切换到文件页根目录，暂未直接进入目标文件夹")
                    }
                }
                FileType.OTHER -> Unit
            }
        },
        modifier = modifier
    )
}

@Composable
private fun CloudHomeScreen(
    uiState: HomeUiState,
    snackbarHostState: SnackbarHostState,
    onFileClick: (CloudFile) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        if (uiState.isLoading) {
            LoadingView(
                modifier = Modifier.padding(innerPadding),
                message = "加载中"
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                UserInfoCard(uiState = uiState)
            }
            item {
                RecentSection(
                    title = "最近转存",
                    emptyMessage = "暂无转存记录",
                    files = uiState.recentTransfers,
                    onFileClick = onFileClick
                )
            }
            item {
                RecentSection(
                    title = "最近浏览",
                    emptyMessage = "暂无浏览记录",
                    files = uiState.recentBrowses,
                    onFileClick = onFileClick
                )
            }
        }
    }
}

@Composable
private fun UserInfoCard(uiState: HomeUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = uiState.nickname,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                UserMetric(
                    label = "文件总数",
                    value = "${uiState.fileCount}"
                )
                UserMetric(
                    label = "已使用容量",
                    value = formatFileSize(uiState.usedBytes)
                )
            }
        }
    }
}

@Composable
private fun UserMetric(
    label: String,
    value: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun RecentSection(
    title: String,
    emptyMessage: String,
    files: List<CloudFile>,
    onFileClick: (CloudFile) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        if (files.isEmpty()) {
            Text(
                text = emptyMessage,
                modifier = Modifier.padding(vertical = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                files.forEachIndexed { index, file ->
                    RecentFileItem(
                        file = file,
                        onClick = { onFileClick(file) }
                    )
                    if (index != files.lastIndex) {
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentFileItem(
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
            Text(
                text = formatFileSize(file.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

private fun formatFileSize(size: Long): String {
    val sizeDouble = size.toDouble()
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "%.1f KB".format(sizeDouble / 1024)
        size < 1024 * 1024 * 1024 -> "%.1f MB".format(sizeDouble / 1024 / 1024)
        else -> "%.1f GB".format(sizeDouble / 1024 / 1024 / 1024)
    }
}
