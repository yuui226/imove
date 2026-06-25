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

    private val _displayMode = MutableStateFlow("latest_day")

    val displayFiles: StateFlow<List<MediaFile>> = combine(
        _allFiles, _displayMode
    ) { files, mode ->
        when (mode) {
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

    // Key on device id + source path: the same USB device keeps its id when the user
    // picks a different folder, so keying on id alone would skip rescanning the new folder.
    @Volatile
    private var loadedKey: String? = null

    private fun keyOf(device: StorageDevice) = "${device.id}|${device.sourcePath}"

    fun reset() {
        loaded = false
        loadedKey = null
        _allFiles.value = emptyList()
    }

    fun setDisplayMode(mode: String) {
        _displayMode.value = mode
    }

    fun loadAllFiles(device: StorageDevice) {
        val key = keyOf(device)
        if (key != loadedKey) {
            loaded = false
            loadedKey = null
            _allFiles.value = emptyList()
        }
        if (loaded || _isLoading.value) return
        _isLoading.value = true
        scope.launch {
            try {
                mediaRepository.getFilesFromDevice(device).collect { files ->
                    _allFiles.value = files
                    // Mark loaded even for an empty result so a genuinely empty folder
                    // isn't re-scanned on every navigation. Failures keep loaded=false (see catch).
                    loaded = true
                    loadedKey = key
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
            .map { DateUtils.getDayKey(it.dateModified) }
            .distinct()
            .sortedDescending()
            .take(days)
            .toSet()
        return files.filter { file ->
            DateUtils.getDayKey(file.dateModified) in dayKeys
        }
    }
}
