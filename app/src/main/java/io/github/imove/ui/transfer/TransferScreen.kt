package io.github.imove.ui.transfer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.Badge
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.imove.domain.model.MediaFile
import io.github.imove.domain.model.TransferItem
import io.github.imove.ui.components.MediaThumbnail
import io.github.imove.ui.components.QueueBottomSheet
import io.github.imove.viewmodel.TransferViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferScreen(
    viewModel: TransferViewModel,
    onBack: () -> Unit,
    onPreview: (Int) -> Unit
) {
    val files by viewModel.files.collectAsState()
    val selectedFiles by viewModel.selectedFiles.collectAsState()
    val isMultiSelect by viewModel.isMultiSelectMode.collectAsState()
    val queue by viewModel.queue.collectAsState()
    var showQueue by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        if (isMultiSelect) "已选 ${selectedFiles.size} 个"
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
        },
        bottomBar = {
            if (queue.isNotEmpty()) {
                BottomAppBar {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        TextButton(onClick = { showQueue = true }) {
                            Text("队列: ${queue.size} 个文件")
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(viewModel.gridColumns.collectAsState().value),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(files) { file ->
                MediaThumbnail(
                    file = file,
                    isTransferred = false, // TODO: check from repository
                    isQueued = queue.any { it.file.id == file.id },
                    isSelected = file.id in selectedFiles,
                    onClick = { viewModel.onFileClick(file) },
                    onLongClick = {
                        val index = files.indexOf(file)
                        onPreview(index)
                    }
                )
            }
        }
    }

    if (showQueue) {
        QueueBottomSheet(
            queue = queue,
            onRemove = { viewModel.removeFromQueue(it) },
            onClear = { viewModel.clearQueue() },
            onDismiss = { showQueue = false }
        )
    }
}
