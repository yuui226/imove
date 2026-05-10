package io.github.imove.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.imove.domain.model.MediaFile
import io.github.imove.domain.repository.TransferRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class PreviewViewModel @Inject constructor(
    private val transferRepository: TransferRepository
) : ViewModel() {

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private var files: List<MediaFile> = emptyList()

    fun setFiles(fileList: List<MediaFile>, initialIndex: Int = 0) {
        files = fileList
        _currentIndex.value = initialIndex
    }

    fun moveToNext() {
        if (_currentIndex.value < files.size - 1) {
            _currentIndex.value++
        }
    }

    fun moveToPrevious() {
        if (_currentIndex.value > 0) {
            _currentIndex.value--
        }
    }

    fun moveCurrentToQueue() {
        val file = files.getOrNull(_currentIndex.value) ?: return
        transferRepository.addToQueue(listOf(file))
    }

    fun getCurrentFile(): MediaFile? = files.getOrNull(_currentIndex.value)

    fun getTotalFiles(): Int = files.size
}
