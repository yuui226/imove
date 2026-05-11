package io.github.imove.ui.transfer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.imove.domain.model.MediaFile
import io.github.imove.ui.components.MediaThumbnail
import io.github.imove.ui.components.QueueBottomSheet
import io.github.imove.util.DateUtils
import io.github.imove.viewmodel.TransferViewModel

private sealed class GridItem {
    data class Header(val label: String, val count: Int) : GridItem()
    data class File(val file: MediaFile) : GridItem()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferScreen(
    viewModel: TransferViewModel,
    onBack: () -> Unit,
    onPreview: (String) -> Unit
) {
    val files by viewModel.displayFiles.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedFiles by viewModel.selectedFiles.collectAsState()
    val isMultiSelect by viewModel.isMultiSelectMode.collectAsState()
    val queue by viewModel.queue.collectAsState()
    val transferredIds by viewModel.transferredIds.collectAsState()
    var showQueue by remember { mutableStateOf(false) }

    val gridState = rememberLazyGridState()
    val gridColumns = viewModel.gridColumns.collectAsState().value

    val queuedFileIds = remember(queue) { queue.map { it.file.id }.toSet() }

    val gridItems = remember(files) {
        buildList {
            val grouped = files.groupBy { file ->
                DateUtils.getDayKey(file.dateTaken.takeIf { it > 0 } ?: file.dateModified)
            }
            grouped.forEach { (_, group) ->
                val representativeDate = group.first().dateTaken.takeIf { it > 0 } ?: group.first().dateModified
                add(GridItem.Header(DateUtils.formatDateHeader(representativeDate), group.size))
                group.forEach { add(GridItem.File(it)) }
            }
        }
    }

    // Preload thumbnails: initial batch on load, then more as user scrolls
    LaunchedEffect(files) {
        if (files.isNotEmpty()) {
            viewModel.preloadImages(0)
        }
    }
    LaunchedEffect(gridState) {
        snapshotFlow { gridState.firstVisibleItemIndex }
            .collect { index ->
                val half = files.size / 2
                if (half > 0 && index >= half) {
                    viewModel.preloadImages(half)
                }
            }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        if (isMultiSelect) "已选 ${selectedFiles.size} 个"
                        else if (isLoading && files.isEmpty()) "加载中..."
                        else "共 ${files.size} 个文件"
                    )
                },
                navigationIcon = {
                    if (isMultiSelect) {
                        IconButton(onClick = { viewModel.toggleMultiSelectMode() }) {
                            Icon(Icons.Default.Close, contentDescription = "关闭")
                        }
                    } else {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    }
                },
                actions = {
                    if (isMultiSelect) {
                        Button(onClick = { viewModel.moveSelectedFiles() }) {
                            Text("Move (${selectedFiles.size})")
                        }
                    } else {
                        IconButton(onClick = { viewModel.toggleMultiSelectMode() }) {
                            Icon(Icons.Default.DoneAll, contentDescription = "多选")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading && files.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(modifier = Modifier.size(48.dp))
                            Text(
                                text = "正在扫描文件...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        }
                    }
                }
                files.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "未找到媒体文件",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(gridColumns),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        gridItems.forEach { item ->
                            when (item) {
                                is GridItem.Header -> {
                                    item(key = "header_${item.label}", span = { GridItemSpan(maxLineSpan) }) {
                                        Text(
                                            text = "${item.label}（${item.count}）",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                                is GridItem.File -> {
                                    item(key = item.file.id) {
                                        val file = item.file
                                        val onClick = remember(file.id) { { viewModel.onFileClick(file) } }
                                        val onLongClick = remember(file.id) { { onPreview(file.id) } }
                                        MediaThumbnail(
                                            file = file,
                                            isTransferred = file.id in transferredIds,
                                            isQueued = file.id in queuedFileIds,
                                            isSelected = file.id in selectedFiles,
                                            onClick = onClick,
                                            onLongClick = onLongClick
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Floating queue button at bottom-left
            SmallFloatingActionButton(
                onClick = { showQueue = true },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                if (queue.isNotEmpty()) {
                    BadgedBox(
                        badge = { Badge { Text("${queue.size}") } }
                    ) {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "队列"
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = "队列"
                    )
                }
            }
        }
    }

    if (showQueue) {
        QueueBottomSheet(
            queue = queue,
            onDismiss = { showQueue = false }
        )
    }
}
