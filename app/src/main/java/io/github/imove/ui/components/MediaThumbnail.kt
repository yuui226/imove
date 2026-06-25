package io.github.imove.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Scale
import io.github.imove.R
import io.github.imove.domain.model.MediaFile

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaThumbnail(
    file: MediaFile,
    transferredIds: Set<String>,
    queuedFileIds: Set<String>,
    failedFileIds: Set<String>,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val isTransferred = file.id in transferredIds
    val isQueued = file.id in queuedFileIds
    val isFailed = file.id in failedFileIds

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        val context = LocalContext.current
        val imageRequest = remember(file.path) {
            ImageRequest.Builder(context)
                .data(file.path)
                .size(384, 384)
                .scale(Scale.FILL)
                .build()
        }
        AsyncImage(
            model = imageRequest,
            contentDescription = file.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )

        if (file.isVideo) {
            Icon(
                imageVector = Icons.Default.PlayCircle,
                contentDescription = stringResource(R.string.video),
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(36.dp)
            )
        }

        if (isTransferred) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = stringResource(R.string.transferred),
                tint = Color.Green,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .size(20.dp)
            )
        }

        if (isQueued) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color(0x88000000))
            ) {
                Text(
                    text = "⏳",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        if (isFailed) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = stringResource(R.string.transfer_failed_retry),
                tint = Color(0xFFE53935),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .size(20.dp)
            )
        }
    }
}
