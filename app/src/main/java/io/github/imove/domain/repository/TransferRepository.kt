package io.github.imove.domain.repository

import io.github.imove.domain.model.MediaFile
import io.github.imove.domain.model.TransferItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface TransferRepository {
    fun addToQueue(files: List<MediaFile>)
    fun getQueue(): Flow<List<TransferItem>>
    fun getTransferredFileIds(): StateFlow<Set<String>>
    fun clearTransferredIds()
}
