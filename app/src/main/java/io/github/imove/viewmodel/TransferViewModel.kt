package io.github.imove.viewmodel

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.imove.data.transfer.LoadedFilesStore
import io.github.imove.data.usb.UsbDeviceManager
import io.github.imove.domain.model.MediaFile
import io.github.imove.domain.model.TransferItem
import io.github.imove.domain.repository.TransferRepository
import io.github.imove.domain.repository.UserPreferencesRepository
import io.github.imove.util.BitmapCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import javax.inject.Inject

@HiltViewModel
class TransferViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val transferRepository: TransferRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val usbDeviceManager: UsbDeviceManager,
    private val filesStore: LoadedFilesStore
) : ViewModel() {

    val allFiles: StateFlow<List<MediaFile>> = filesStore.allFiles
    val displayFiles: StateFlow<List<MediaFile>> = filesStore.displayFiles
    val isLoading: StateFlow<Boolean> = filesStore.isLoading

    val transferredIds: StateFlow<Set<String>> = transferRepository.getTransferredFileIds()

    val queue: StateFlow<List<TransferItem>> = transferRepository.getQueue()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val queuedFileIds: StateFlow<Set<String>> = queue
        .map { items -> items.map { it.file.id }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val gridColumns: StateFlow<Int> = preferencesRepository.getPreferences()
        .map { it.gridColumns }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 3)

    fun setDisplayMode(mode: String) {
        filesStore.setDisplayMode(mode)
    }

    fun loadAllFiles() {
        val device = usbDeviceManager.connectedDevice.value ?: return
        filesStore.loadAllFiles(device)
    }

    fun onFileClick(file: MediaFile) {
        if (file.id !in transferredIds.value && file.id !in queuedFileIds.value) {
            transferRepository.addToQueue(listOf(file))
        }
    }

    fun addToQueue(files: List<MediaFile>) {
        val transferred = transferredIds.value
        val queued = queuedFileIds.value
        val pending = files.filter { it.id !in transferred && it.id !in queued }
        if (pending.isNotEmpty()) transferRepository.addToQueue(pending)
    }

    fun removeFromQueue(itemId: String) {
        transferRepository.removeFromQueue(itemId)
    }

    fun clearQueue() {
        transferRepository.clearQueue()
    }

    private var lastPreloadedIndex = -1

    fun preloadImages(fromIndex: Int, count: Int = 150) {
        val files = displayFiles.value
        if (files.isEmpty()) return
        if (fromIndex == 0) lastPreloadedIndex = -1
        if (fromIndex <= lastPreloadedIndex) return
        lastPreloadedIndex = fromIndex
        val end = (fromIndex + count).coerceAtMost(files.size)
        val semaphore = Semaphore(4)
        for (i in fromIndex until end) {
            val file = files[i]
            if (BitmapCache.contains(file.path)) continue
            viewModelScope.launch(Dispatchers.IO) {
                semaphore.withPermit {
                    try {
                        context.contentResolver.openInputStream(Uri.parse(file.path))?.use { stream ->
                            val options = BitmapFactory.Options().apply { inSampleSize = 4 }
                            val bitmap = BitmapFactory.decodeStream(stream, null, options)
                            if (bitmap != null) BitmapCache.put(file.path, bitmap)
                        }
                    } catch (_: Exception) {}
                }
            }
        }
    }
}
