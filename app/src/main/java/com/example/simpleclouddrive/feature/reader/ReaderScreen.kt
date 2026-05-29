package com.example.simpleclouddrive.feature.reader

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.simpleclouddrive.core.ui.component.EmptyView
import com.example.simpleclouddrive.core.ui.component.ErrorView
import com.example.simpleclouddrive.core.ui.component.LoadingView
import com.example.simpleclouddrive.domain.repository.FileRepository

@Composable
fun ReaderScreen(
    fileId: String,
    fileRepository: FileRepository,
    modifier: Modifier = Modifier
) {
    val viewModel: ReaderViewModel = viewModel(
        key = fileId,
        factory = ReaderViewModelFactory(
            fileId = fileId,
            fileRepository = fileRepository
        )
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ReaderScreen(
        uiState = uiState,
        onNextPage = viewModel::nextPage,
        onPreviousPage = viewModel::previousPage,
        onReaderSizeChanged = viewModel::paginate,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderScreen(
    uiState: ReaderUiState,
    onNextPage: () -> Unit,
    onPreviousPage: () -> Unit,
    onReaderSizeChanged: (Int, Int, Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val fontSize = 18.sp
    val lineHeight = 30.sp

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.title.ifBlank { "TXT 阅读器" },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        },
        bottomBar = {
            PageIndicator(
                currentPage = uiState.currentPageNumber,
                totalPages = uiState.totalPages
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val widthPx = with(density) { maxWidth.toPx().toInt() }
            val heightPx = with(density) { maxHeight.toPx().toInt() }
            val fontSizePx = with(density) { fontSize.toPx() }
            val lineHeightPx = with(density) { lineHeight.toPx() }

            LaunchedEffect(widthPx, heightPx, fontSizePx, lineHeightPx, uiState.title) {
                onReaderSizeChanged(widthPx, heightPx, fontSizePx, lineHeightPx)
            }

            when {
                uiState.isLoading || uiState.isPaginating -> LoadingView(message = "加载中")
                uiState.errorMessage != null -> ErrorView(message = uiState.errorMessage)
                uiState.pages.isEmpty() -> EmptyView(message = "文档为空")
                else -> ReaderPage(
                    text = uiState.currentPageText,
                    fontSize = fontSize,
                    lineHeight = lineHeight,
                    onNextPage = onNextPage,
                    onPreviousPage = onPreviousPage
                )
            }
        }
    }
}

@Composable
private fun ReaderPage(
    text: String,
    fontSize: androidx.compose.ui.unit.TextUnit,
    lineHeight: androidx.compose.ui.unit.TextUnit,
    onNextPage: () -> Unit,
    onPreviousPage: () -> Unit
) {
    var horizontalDragAmount by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { horizontalDragAmount = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        horizontalDragAmount += dragAmount
                    },
                    onDragEnd = {
                        when {
                            horizontalDragAmount < -SWIPE_THRESHOLD_PX -> onNextPage()
                            horizontalDragAmount > SWIPE_THRESHOLD_PX -> onPreviousPage()
                        }
                        horizontalDragAmount = 0f
                    },
                    onDragCancel = { horizontalDragAmount = 0f }
                )
            }
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = fontSize,
                lineHeight = lineHeight
            )
        )
    }
}

@Composable
private fun PageIndicator(
    currentPage: Int,
    totalPages: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "$currentPage / $totalPages",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private const val SWIPE_THRESHOLD_PX = 80f
