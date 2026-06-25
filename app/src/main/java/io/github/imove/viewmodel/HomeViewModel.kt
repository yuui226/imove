package io.github.imove.viewmodel

import android.net.Uri
import android.provider.DocumentsContract
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.imove.data.transfer.LoadedFilesStore
import io.github.imove.data.usb.StorageAccessManager
import io.github.imove.data.usb.UsbDeviceManager
import io.github.imove.domain.model.StorageDevice
import io.github.imove.domain.repository.DeviceRepository
import io.github.imove.domain.repository.TransferRepository
import io.github.imove.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
    private val usbDeviceManager: UsbDeviceManager,
    private val storageAccessManager: StorageAccessManager,
    private val filesStore: LoadedFilesStore,
    private val transferRepository: TransferRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val connectedDevice = usbDeviceManager.connectedDevice

    val targetDirectory: StateFlow<String> = preferencesRepository.getPreferences()
        .map { it.targetDirectory }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    private val _isDetecting = MutableStateFlow(true)
    val isDetecting: StateFlow<Boolean> = _isDetecting.asStateFlow()

    private val _isRestoring = MutableStateFlow(false)

    val uiState: StateFlow<HomeUiState> = combine(
        usbDeviceManager.connectedDevice, _isRestoring, targetDirectory
    ) { device, isRestoring, target ->
        when {
            device != null && !device.isLocal && device.sourcePath.isEmpty() && isRestoring ->
                HomeUiState.Restoring
            device != null && !device.isLocal && device.sourcePath.isEmpty() ->
                HomeUiState.PickSource(device.volumeLabel)
            device != null && device.sourcePath.isNotEmpty() && target.isNotEmpty() ->
                HomeUiState.Ready(device.isLocal, device.sourcePath, device.volumeLabel)
            device != null && !device.isLocal && target.isEmpty() ->
                HomeUiState.NeedTarget
            else ->
                HomeUiState.Setup(
                    localSourcePath = device?.takeIf { it.isLocal }?.sourcePath,
                    targetDirectory = target
                )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState.Setup(null, ""))

    init {
        usbDeviceManager.startListening()
        // Restore saved sourcePath for any connected device (including already-connected on launch)
        viewModelScope.launch {
            usbDeviceManager.connectedDevice
                .collect { device ->
                    if (device != null) _isDetecting.value = false
                    if (device == null) {
                        filesStore.reset()
                        transferRepository.clearTransferredIds()
                        return@collect
                    }
                    if (device.sourcePath.isEmpty()) {
                        _isRestoring.value = true
                        restoreSourcePath(device)
                        _isRestoring.value = false
                    } else {
                        filesStore.loadAllFiles(device)
                    }
                }
        }
        // Stop detecting only after all retries are done
        viewModelScope.launch {
            usbDeviceManager.detectionComplete
                .collect { complete ->
                    if (complete) _isDetecting.value = false
                }
        }
    }

    private suspend fun restoreSourcePath(device: StorageDevice) {
        val existing = deviceRepository.getDeviceByVolume(device.volumeUuid)
        if (existing != null && existing.sourcePath.isNotEmpty()) {
            val restored = existing.copy(lastConnected = System.currentTimeMillis())
            deviceRepository.saveDevice(restored)
            usbDeviceManager.updateConnectedDevice(restored)
        } else if (existing == null) {
            deviceRepository.saveDevice(device)
        }
    }

    fun saveSourcePath(uri: Uri) {
        storageAccessManager.takePersistablePermission(uri)
        // Store full tree URI so MediaRepository can parse it for SAF queries
        val sourcePath = uri.toString()
        val device = connectedDevice.value
        if (device == null) {
            // No physical device connected: treat the picked folder as a local source.
            // Intentionally not persisted — a local folder only lasts for the current session.
            val localDevice = StorageDevice(
                id = StorageDevice.LOCAL_DEVICE_ID,
                volumeUuid = StorageDevice.LOCAL_DEVICE_ID,
                volumeLabel = localFolderLabel(uri),
                sourcePath = sourcePath,
                lastConnected = System.currentTimeMillis()
            )
            usbDeviceManager.updateConnectedDevice(localDevice)
            return
        }
        viewModelScope.launch {
            val existing = deviceRepository.getDeviceByVolume(device.volumeUuid)
            val updated = (existing ?: device).copy(sourcePath = sourcePath)
            deviceRepository.saveDevice(updated)
            usbDeviceManager.updateConnectedDevice(updated)
        }
    }

    /** Derive a readable label from a tree URI, e.g. "primary:DCIM/Camera" -> "Camera". */
    private fun localFolderLabel(uri: Uri): String {
        val docId = runCatching { DocumentsContract.getTreeDocumentId(uri) }.getOrNull()
        return docId
            ?.substringAfterLast('/')
            ?.substringAfterLast(':')
            ?.takeIf { it.isNotBlank() }
            ?: "Local"
    }

    fun updateTargetDirectory(path: String) {
        viewModelScope.launch { preferencesRepository.updateTargetDirectory(path) }
    }

    override fun onCleared() {
        usbDeviceManager.stopListening()
        super.onCleared()
    }
}
