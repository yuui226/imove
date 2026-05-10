package io.github.imove.data.repository

import io.github.imove.domain.model.MediaFile
import io.github.imove.domain.model.TransferItem
import io.github.imove.domain.model.TransferStatus
import io.github.imove.domain.repository.TransferRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransferRepositoryImpl @Inject constructor() : TransferRepository {

    private val _queue = MutableStateFlow<List<TransferItem>>(emptyList())

    override fun addToQueue(files: List<MediaFile>) {
        val newItems = files.map { file ->
            TransferItem(
                id = UUID.randomUUID().toString(),
                file = file,
                status = TransferStatus.QUEUED,
                addedAt = System.currentTimeMillis()
            )
        }
        _queue.update { current -> current + newItems }
    }

    override fun getQueue(): Flow<List<TransferItem>> = _queue.asStateFlow()

    override fun removeFromQueue(itemId: String) {
        _queue.update { current ->
            current.filterNot { it.id == itemId && it.status != TransferStatus.TRANSFERRING }
        }
    }

    override fun clearQueue() {
        _queue.update { current ->
            current.filter { it.status == TransferStatus.TRANSFERRING }
        }
    }

    override fun cancelTransfer() {
        _queue.update { current ->
            current.map { item ->
                if (item.status == TransferStatus.QUEUED) {
                    item.copy(status = TransferStatus.CANCELLED)
                } else item
            }
        }
    }

    fun updateItemStatus(itemId: String, status: TransferStatus, completedAt: Long? = null) {
        _queue.update { current ->
            current.map { item ->
                if (item.id == itemId) {
                    item.copy(status = status, completedAt = completedAt)
                } else item
            }
        }
    }

    fun removeCompleted() {
        _queue.update { current ->
            current.filter { it.status == TransferStatus.QUEUED || it.status == TransferStatus.TRANSFERRING }
        }
    }
}
