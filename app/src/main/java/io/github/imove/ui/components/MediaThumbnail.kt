package io.github.imove.ui.components

import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import io.github.imove.domain.model.MediaFile
import io.github.imove.util.BitmapCache

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaThumbnail(
    file: MediaFile,
    isTransferred: Boolean = false,
    isQueued: Boolean = false,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    var cachedBitmap by remember(file.id) {
        mutableStateOf(BitmapCache.get(file.path))
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        key(file.id) {
            if (cachedBitmap != null) {
                Image(
                    bitmap = cachedBitmap!!.asImageBitmap(),
                    contentDescription = file.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            } else {
                AsyncImage(
                    model = file.path,
                    contentDescription = file.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    onState = { state ->
                        if (state is AsyncImagePainter.State.Success) {
                            val bitmap = (state.result.drawable as? BitmapDrawable)?.bitmap
                            if (bitmap != null) {
                                BitmapCache.put(file.path, bitmap)
                                cachedBitmap = bitmap
                            }
                        }
                    }
                )
            }

            if (file.isVideo) {
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = "视频",
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(36.dp)
                )
            }
        }

        if (isTransferred) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "已传输",
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

        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x442196F3))
            )
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "已选中",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .size(24.dp)
            )
        }
    }
}
