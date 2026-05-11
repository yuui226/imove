package io.github.imove.domain.model

import androidx.compose.runtime.Stable

@Stable
data class MediaFile(
    val id: String,
    val name: String,
    val path: String,
    val size: Long,
    val mimeType: String,
    val dateTaken: Long,
    val dateModified: Long,
    val isVideo: Boolean
)
