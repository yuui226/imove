package io.github.imove.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.imove.data.transfer.LoadedFilesStore
import io.github.imove.domain.model.MediaFile
import io.github.imove.domain.model.TransferItem
import io.github.imove.domain.repository.TransferRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class PreviewViewModel @Inject constructor(
    private val transferRepository: TransferRepository,
    private val filesStore: LoadedFilesStore
) : ViewModel() {

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    val files: StateFlow<List<MediaFile>> = filesStore.files

    val queue: StateFlow<List<TransferItem>> = transferRepository.getQueue()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _justQueued = MutableStateFlow<Set<String>>(emptySet())
    val justQueued: StateFlow<Set<String>> = _justQueued.asStateFlow()

    fun setInitialIndex(index: Int) {
        _currentIndex.value = index.coerceIn(0, (files.value.size - 1).coerceAtLeast(0))
    }

    fun moveToNext() {
        if (_currentIndex.value < files.value.size - 1) {
            _currentIndex.value++
        }
    }

    fun moveToPrevious() {
        if (_currentIndex.value > 0) {
            _currentIndex.value--
        }
    }

    fun moveCurrentToQueue() {
        val file = files.value.getOrNull(_currentIndex.value) ?: return
        transferRepository.addToQueue(listOf(file))
        _justQueued.value = _justQueued.value + file.id
    }

    fun isCurrentQueued(): Boolean {
        val file = files.value.getOrNull(_currentIndex.value) ?: return false
        return _justQueued.value.contains(file.id) ||
                queue.value.any { it.file.id == file.id }
    }

    fun getCurrentFile(): MediaFile? = files.value.getOrNull(_currentIndex.value)
    fun getTotalFiles(): Int = files.value.size
}
