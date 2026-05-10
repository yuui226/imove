package io.github.imove.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.imove.domain.model.MediaFile
import io.github.imove.domain.model.StorageDevice
import io.github.imove.domain.model.TransferItem
import io.github.imove.domain.repository.MediaRepository
import io.github.imove.domain.repository.TransferRepository
import io.github.imove.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransferViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val transferRepository: TransferRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _files = MutableStateFlow<List<MediaFile>>(emptyList())
    val files: StateFlow<List<MediaFile>> = _files.asStateFlow()

    private val _selectedFiles = MutableStateFlow<Set<String>>(emptySet())
    val selectedFiles: StateFlow<Set<String>> = _selectedFiles.asStateFlow()

    private val _isMultiSelectMode = MutableStateFlow(false)
    val isMultiSelectMode: StateFlow<Boolean> = _isMultiSelectMode.asStateFlow()

    val queue: StateFlow<List<TransferItem>> = transferRepository.getQueue()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val gridColumns: StateFlow<Int> = preferencesRepository.getPreferences()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), io.github.imove.domain.model.UserPreferences())
        .let { prefs ->
            MutableStateFlow(3) // Will be properly connected
        }

    fun loadFiles(device: StorageDevice, startDate: Long = 0, endDate: Long = 0) {
        viewModelScope.launch {
            if (startDate > 0 && endDate > 0) {
                _files.value = mediaRepository.getFilesByDateRange(device, startDate, endDate)
            } else {
                mediaRepository.getFilesFromDevice(device).collect { fileList ->
                    _files.value = fileList
                }
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

    fun onFileLongPress(file: MediaFile) {
        // Navigate to preview - handled by UI
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
        val selected = _files.value.filter { it.id in _selectedFiles.value }
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
