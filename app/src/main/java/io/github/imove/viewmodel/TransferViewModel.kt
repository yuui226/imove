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
import kotlinx.coroutines.flow.MutableStateFlow
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

    val gridColumns: StateFlow<Int> = preferencesRepository.getPreferences()
        .map { it.gridColumns }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 3)

    private val _selectedFiles = MutableStateFlow<Set<String>>(emptySet())
    val selectedFiles: StateFlow<Set<String>> = _selectedFiles

    private val _isMultiSelectMode = MutableStateFlow(false)
    val isMultiSelectMode: StateFlow<Boolean> = _isMultiSelectMode

    fun setDisplayMode(mode: String) {
        filesStore.setDisplayMode(mode)
    }

    fun loadAllFiles() {
        val device = usbDeviceManager.connectedDevice.value ?: return
        filesStore.loadAllFiles(device)
    }

    fun onFileClick(file: MediaFile) {
        if (_isMultiSelectMode.value) {
            toggleSelection(file.id)
        } else {
            transferRepository.addToQueue(listOf(file))
        }
    }

    fun toggleMultiSelectMode() {
        _isMultiSelectMode.value = !_isMultiSelectMode.value
        if (!_isMultiSelectMode.value) {
            _selectedFiles.value = emptySet()
        }
    }

    private fun toggleSelection(fileId: String) {
        _selectedFiles.value = _selectedFiles.value.toMutableSet().apply {
            if (contains(fileId)) remove(fileId) else add(fileId)
        }
    }

    fun moveSelectedFiles() {
        val selected = filesStore.allFiles.value.filter { it.id in _selectedFiles.value }
        transferRepository.addToQueue(selected)
        _selectedFiles.value = emptySet()
        _isMultiSelectMode.value = false
    }

    fun removeFromQueue(itemId: String) {
        transferRepository.removeFromQueue(itemId)
    }

    fun clearQueue() {
        transferRepository.clearQueue()
    }

    private var lastPreloadedIndex = 0

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
