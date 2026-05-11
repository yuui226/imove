package io.github.imove.ui.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import android.os.SystemClock
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.imove.R
import coil.compose.AsyncImage
import io.github.imove.viewmodel.PreviewViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(
    viewModel: PreviewViewModel,
    onBack: () -> Unit
) {
    val files by viewModel.files.collectAsState()
    val currentIndex by viewModel.currentIndex.collectAsState()
    val queuedFileIds by viewModel.queuedFileIds.collectAsState()
    val transferredIds by viewModel.transferredIds.collectAsState()
    val justQueued by viewModel.justQueued.collectAsState()

    if (currentIndex < 0 || files.isEmpty()) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(stringResource(R.string.preview_photo)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.loading), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    val pagerState = rememberPagerState(
        initialPage = currentIndex,
        pageCount = { files.size }
    )

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            viewModel.setInitialIndex(page)
        }
    }

    LaunchedEffect(transferredIds) {
        if (transferredIds.isNotEmpty()) {
            viewModel.cleanJustQueued(transferredIds)
        }
    }

    val currentPage = pagerState.currentPage

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("${currentPage + 1} / ${files.size}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.moveCurrentToQueue() }) {
                Text(stringResource(R.string.move))
            }
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) { page ->
            val file = files[page]
            val isTransferred = file.id in transferredIds
            val isQueued = !isTransferred && (justQueued.contains(file.id) || file.id in queuedFileIds)

            var scale by remember(page) { mutableFloatStateOf(1f) }
            var offsetX by remember(page) { mutableFloatStateOf(0f) }
            var offsetY by remember(page) { mutableFloatStateOf(0f) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(page) {
                        var lastTapTime = 0L
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            val now = SystemClock.uptimeMillis()
                            val isDoubleTap = now - lastTapTime < 300L
                            lastTapTime = now
                            if (isDoubleTap) {
                                scale = 1f
                                offsetX = 0f
                                offsetY = 0f
                            }
                            do {
                                val event = awaitPointerEvent()
                                val zoom = event.calculateZoom()
                                val pan = event.calculatePan()
                                val newScale = (scale * zoom).coerceIn(1f, 5f)
                                if (newScale > 1f) {
                                    scale = newScale
                                    offsetX += pan.x
                                    offsetY += pan.y
                                    event.changes.forEach { it.consume() }
                                } else if (scale > 1f) {
                                    scale = newScale
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            } while (event.changes.any { it.pressed })
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = file.path,
                    contentDescription = file.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offsetX
                            translationY = offsetY
                        }
                )
                if (file.isVideo) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.video),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxSize(0.2f),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                if (isQueued) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xCC4CAF50))
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = stringResource(R.string.add_to_queue),
                            tint = Color.White,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(20.dp)
                        )
                    }
                }
                if (isTransferred) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = stringResource(R.string.transferred),
                        tint = Color.Green,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .size(32.dp)
                    )
                }
            }
        }
    }
}
