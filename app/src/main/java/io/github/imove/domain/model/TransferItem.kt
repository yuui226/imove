package io.github.imove.domain.model

data class TransferItem(
    val id: String,
    val file: MediaFile,
    val status: TransferStatus,
    val addedAt: Long,
    val completedAt: Long? = null
)
