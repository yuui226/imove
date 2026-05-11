package io.github.imove.data.transfer

import io.github.imove.domain.model.MediaFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton cache for the currently loaded file list.
 * Survives ViewModel recreation across navigation.
 * Files are never cleared until the app process dies.
 */
@Singleton
class LoadedFilesStore @Inject constructor() {

    private val _files = MutableStateFlow<List<MediaFile>>(emptyList())
    val files: StateFlow<List<MediaFile>> = _files.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var lastLoadKey: String = ""

    fun isSameLoad(startDate: Long, endDate: Long): Boolean {
        val key = "$startDate-$endDate"
        return key == lastLoadKey && _files.value.isNotEmpty()
    }

    /** Replace file list. Ignores empty results if we already have files. */
    fun setFiles(files: List<MediaFile>, startDate: Long = 0, endDate: Long = 0) {
        if (files.isEmpty() && _files.value.isNotEmpty()) {
            // Don't clear existing files on empty result (e.g. scan error)
            return
        }
        _files.value = files
        lastLoadKey = "$startDate-$endDate"
    }

    fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }
}
