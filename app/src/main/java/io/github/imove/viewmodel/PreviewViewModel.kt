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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PreviewViewModel @Inject constructor(
    private val transferRepository: TransferRepository,
    private val filesStore: LoadedFilesStore
) : ViewModel() {

    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    val files: StateFlow<List<MediaFile>> = filesStore.displayFiles

    val queue: StateFlow<List<TransferItem>> = transferRepository.getQueue()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val queuedFileIds: StateFlow<Set<String>> = queue
        .map { items -> items.map { it.file.id }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val transferredIds: StateFlow<Set<String>> = transferRepository.getTransferredFileIds()

    private val _justQueued = MutableStateFlow<Set<String>>(emptySet())
    val justQueued: StateFlow<Set<String>> = _justQueued.asStateFlow()

    fun setInitialFile(fileId: String) {
        viewModelScope.launch {
            val loaded = filesStore.displayFiles.first { it.isNotEmpty() }
            val index = loaded.indexOfFirst { it.id == fileId }
            _currentIndex.value = if (index >= 0) index else 0
        }
    }

    fun setInitialIndex(index: Int) {
        _currentIndex.value = index.coerceIn(0, (files.value.size - 1).coerceAtLeast(0))
    }

    fun moveCurrentToQueue() {
        val file = files.value.getOrNull(_currentIndex.value) ?: return
        transferRepository.addToQueue(listOf(file))
        _justQueued.value = _justQueued.value + file.id
    }

    fun cleanJustQueued(transferred: Set<String>) {
        _justQueued.value = _justQueued.value - transferred
    }
}
