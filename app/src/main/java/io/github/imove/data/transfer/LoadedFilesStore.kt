package io.github.imove.data.transfer

import android.util.Log
import io.github.imove.domain.model.MediaFile
import io.github.imove.domain.model.StorageDevice
import io.github.imove.domain.repository.MediaRepository
import io.github.imove.util.DateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoadedFilesStore @Inject constructor(
    private val mediaRepository: MediaRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _allFiles = MutableStateFlow<List<MediaFile>>(emptyList())
    val allFiles: StateFlow<List<MediaFile>> = _allFiles.asStateFlow()

    private val _displayMode = MutableStateFlow("latest_day")

    val displayFiles: StateFlow<List<MediaFile>> = combine(
        _allFiles, _displayMode
    ) { files, mode ->
        if (files.isEmpty()) emptyList()
        else when (mode) {
            "latest_day" -> filterByRecentDays(files, 1)
            "three_days" -> filterByRecentDays(files, 3)
            "ten_days" -> filterByRecentDays(files, 10)
            else -> files
        }
    }.stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    @Volatile
    private var loaded = false

    @Volatile
    private var loadedDeviceId: String? = null

    fun isLoaded(): Boolean = loaded && _allFiles.value.isNotEmpty()

    fun reset() {
        loaded = false
        loadedDeviceId = null
        _allFiles.value = emptyList()
    }

    fun setDisplayMode(mode: String) {
        _displayMode.value = mode
    }

    fun loadAllFiles(device: StorageDevice) {
        if (device.id != loadedDeviceId) {
            loaded = false
            loadedDeviceId = null
        }
        if (loaded || _isLoading.value) return
        _isLoading.value = true
        scope.launch {
            try {
                mediaRepository.getFilesFromDevice(device).collect { files ->
                    _allFiles.value = files
                    if (files.isNotEmpty()) {
                        loaded = true
                        loadedDeviceId = device.id
                    }
                    Log.d("LoadedFilesStore", "Loaded ${files.size} files")
                }
            } catch (e: Exception) {
                Log.e("LoadedFilesStore", "Load failed", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun filterByRecentDays(files: List<MediaFile>, days: Int): List<MediaFile> {
        val dayKeys = files
            .map { DateUtils.getDayKey(it.dateTaken.takeIf { d -> d > 0 } ?: it.dateModified) }
            .distinct()
            .sortedDescending()
            .take(days)
            .toSet()
        return files.filter { file ->
            val date = file.dateTaken.takeIf { d -> d > 0 } ?: file.dateModified
            DateUtils.getDayKey(date) in dayKeys
        }
    }
}
