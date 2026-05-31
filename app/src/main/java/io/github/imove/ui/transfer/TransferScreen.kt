package io.github.imove.ui.transfer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.request.ImageRequest
import io.github.imove.R
import io.github.imove.domain.model.MediaFile
import io.github.imove.ui.components.MediaThumbnail
import io.github.imove.util.DateUtils
import io.github.imove.viewmodel.TransferViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

private sealed class GridItem {
    data class Header(val label: String, val files: List<MediaFile>) : GridItem()
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
    val queue by viewModel.queue.collectAsState()
    val transferredIds by viewModel.transferredIds.collectAsState()
    val queuedFileIds by viewModel.queuedFileIds.collectAsState()

    var showDone by remember { mutableStateOf(false) }
    var prevQueueSize by remember { mutableStateOf(queue.size) }
    LaunchedEffect(queue.size) {
        if (prevQueueSize > 0 && queue.isEmpty()) {
            showDone = true
            kotlinx.coroutines.delay(3000)
            showDone = false
        }
        prevQueueSize = queue.size
    }

    val gridState = rememberLazyGridState()
    val gridColumns = viewModel.gridColumns.collectAsState().value

    val gridItems = remember(files) {
        buildList {
            val grouped = files.groupBy { file ->
                DateUtils.getDayKey(file.dateTaken.takeIf { it > 0 } ?: file.dateModified)
            }
            grouped.forEach { (_, group) ->
                val representativeDate = group.first().dateTaken.takeIf { it > 0 } ?: group.first().dateModified
                add(GridItem.Header(DateUtils.formatDateHeader(representativeDate), group))
                group.forEach { add(GridItem.File(it)) }
            }
        }
    }

    // Coil prefetch: on scroll stop, concurrently load upcoming images into memory cache
    val context = LocalContext.current
    val imageLoader = remember { (context.applicationContext as coil.ImageLoaderFactory).newImageLoader() }
    LaunchedEffect(gridState, files) {
        var lastPrefetchIndex = -1
        snapshotFlow { gridState.isScrollInProgress }
            .collectLatest { scrolling ->
                if (!scrolling && files.isNotEmpty()) {
                    val snapshot = files.toList()  // local copy, won't change mid-iteration
                    val index = gridState.firstVisibleItemIndex
                    if (index == lastPrefetchIndex) return@collectLatest
                    lastPrefetchIndex = index
                    val start = index.coerceIn(snapshot.indices)
                    val end = (start + 160).coerceAtMost(snapshot.size)
                    val semaphore = Semaphore(4)
                    withContext(Dispatchers.IO) {
                        (start until end).map { i ->
                            launch {
                                val uri = snapshot[i].path
                                // Skip if already in memory cache
                                if (imageLoader.memoryCache?.get(coil.memory.MemoryCache.Key(uri)) != null) return@launch
                                semaphore.withPermit {
                                    val request = ImageRequest.Builder(context)
                                        .data(uri)
                                        .size(384, 384)
                                        .build()
                                    imageLoader.execute(request)
                                }
                            }
                        }.joinAll()
                    }
                }
            }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        if (isLoading && files.isEmpty()) stringResource(R.string.loading)
                        else stringResource(R.string.total_files, files.size),
                        style = MaterialTheme.typography.titleSmall
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    AnimatedVisibility(
                        visible = queue.isNotEmpty() || showDone,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Text(
                            text = if (queue.isNotEmpty()) "${queue.size}" else "Done",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    }
                },
                modifier = Modifier.height(62.dp)
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
                                text = stringResource(R.string.scanning_files),
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
                            text = stringResource(R.string.no_media_found),
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
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${item.label}（${item.files.size}）",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Button(
                                                onClick = { viewModel.addToQueue(item.files) },
                                                modifier = Modifier.height(24.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                                            ) {
                                                Text(stringResource(R.string.move), style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                                is GridItem.File -> {
                                    item(key = item.file.id) {
                                        val file = item.file
                                        val onClick = remember(file.id) { { viewModel.onFileClick(file) } }
                                        val onLongClick = remember(file.id) { { onPreview(file.id) } }
                                        MediaThumbnail(
                                            file = file,
                                            transferredIds = transferredIds,
                                            queuedFileIds = queuedFileIds,
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
        }
    }
}
