package io.github.imove.domain.repository

import io.github.imove.domain.model.MediaFile
import io.github.imove.domain.model.TransferItem
import kotlinx.coroutines.flow.Flow

interface TransferRepository {
    fun addToQueue(files: List<MediaFile>)
    fun getQueue(): Flow<List<TransferItem>>
    fun removeFromQueue(itemId: String)
    fun clearQueue()
    fun cancelTransfer()
}
