package io.github.imove.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.imove.data.transfer.LoadedFilesStore
import io.github.imove.data.usb.UsbDeviceManager
import io.github.imove.domain.model.MediaFile
import io.github.imove.domain.model.TransferItem
import io.github.imove.domain.repository.MediaRepository
import io.github.imove.domain.repository.TransferRepository
import io.github.imove.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransferViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val transferRepository: TransferRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val usbDeviceManager: UsbDeviceManager,
    private val filesStore: LoadedFilesStore
) : ViewModel() {

    val files: StateFlow<List<MediaFile>> = filesStore.files
    val isLoading: StateFlow<Boolean> = filesStore.isLoading

    private val _selectedFiles = MutableStateFlow<Set<String>>(emptySet())
    val selectedFiles: StateFlow<Set<String>> = _selectedFiles.asStateFlow()

    val transferredIds: StateFlow<Set<String>> = transferRepository.getTransferredFileIds()

    private val _isMultiSelectMode = MutableStateFlow(false)
    val isMultiSelectMode: StateFlow<Boolean> = _isMultiSelectMode.asStateFlow()

    val queue: StateFlow<List<TransferItem>> = transferRepository.getQueue()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val gridColumns: StateFlow<Int> = preferencesRepository.getPreferences()
        .map { it.gridColumns }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 3)

    fun loadFiles(startDate: Long = 0, endDate: Long = 0) {
        if (filesStore.isSameLoad(startDate, endDate)) {
            Log.d("TransferViewModel", "loadFiles: same params already loaded, skipping")
            return
        }
        if (filesStore.isLoading.value) return

        val device = usbDeviceManager.connectedDevice.value
        if (device == null) {
            Log.w("TransferViewModel", "loadFiles: no connected device")
            return
        }
        Log.d("TransferViewModel", "loadFiles: sourcePath=${device.sourcePath}, startDate=$startDate, endDate=$endDate")
        filesStore.setLoading(true)
        viewModelScope.launch {
            try {
                val flow = if (startDate > 0 && endDate > 0) {
                    mediaRepository.getFilesByDateRange(device, startDate, endDate)
                } else {
                    mediaRepository.getFilesFromDevice(device)
                }
                flow.collect { fileList ->
                    filesStore.setFiles(fileList, startDate, endDate)
                }
            } catch (e: Exception) {
                Log.e("TransferViewModel", "loadFiles error", e)
            } finally {
                filesStore.setLoading(false)
            }
        }
    }

    fun loadLatestDay() {
        if (filesStore.isLoading.value) return
        if (filesStore.isSameLoad(-1, -1)) return
        val device = usbDeviceManager.connectedDevice.value
        if (device == null) {
            Log.w("TransferViewModel", "loadLatestDay: no connected device")
            return
        }
        Log.d("TransferViewModel", "loadLatestDay: sourcePath=${device.sourcePath}")
        filesStore.setLoading(true)
        viewModelScope.launch {
            try {
                mediaRepository.getFilesFromDevice(device).collect { allFiles ->
                    if (allFiles.isEmpty()) {
                        filesStore.setFiles(emptyList(), -1, -1)
                        return@collect
                    }
                    val latestDayKey = allFiles
                        .maxOf { it.dateTaken.takeIf { d -> d > 0 } ?: it.dateModified }
                        .let { io.github.imove.util.DateUtils.getDayKey(it) }
                    val latestDayFiles = allFiles.filter { file ->
                        val date = file.dateTaken.takeIf { d -> d > 0 } ?: file.dateModified
                        io.github.imove.util.DateUtils.getDayKey(date) == latestDayKey
                    }
                    filesStore.setFiles(latestDayFiles, -1, -1)
                }
            } catch (e: Exception) {
                Log.e("TransferViewModel", "loadLatestDay error", e)
            } finally {
                filesStore.setLoading(false)
            }
        }
    }

    fun onFileClick(file: MediaFile) {
        if (_isMultiSelectMode.value) {
            toggleSelection(file.id)
        } else {
            transferRepository.addToQueue(listOf(file))
        }
    }

    fun onFileLongPress(file: MediaFile) {}

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
        val selected = filesStore.files.value.filter { it.id in _selectedFiles.value }
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
}
