package io.github.imove.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.imove.data.transfer.LoadedFilesStore
import io.github.imove.data.usb.StorageAccessManager
import io.github.imove.data.usb.UsbDeviceManager
import io.github.imove.domain.model.StorageDevice
import io.github.imove.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
    private val usbDeviceManager: UsbDeviceManager,
    private val storageAccessManager: StorageAccessManager,
    private val filesStore: LoadedFilesStore
) : ViewModel() {

    val connectedDevice = usbDeviceManager.connectedDevice

    private val _isDetecting = MutableStateFlow(true)
    val isDetecting: StateFlow<Boolean> = _isDetecting.asStateFlow()

    private val _isRestoring = MutableStateFlow(false)
    val isRestoring: StateFlow<Boolean> = _isRestoring.asStateFlow()

    init {
        usbDeviceManager.startListening()
        // Restore saved sourcePath for any connected device (including already-connected on launch)
        viewModelScope.launch {
            usbDeviceManager.connectedDevice
                .filterNotNull()
                .collect { device ->
                    _isDetecting.value = false
                    if (device.sourcePath.isEmpty()) {
                        _isRestoring.value = true
                        restoreSourcePath(device)
                        _isRestoring.value = false
                    } else {
                        filesStore.loadAllFiles(device)
                    }
                }
        }
        // Stop detecting after a timeout even if no device found
        viewModelScope.launch {
            kotlinx.coroutines.delay(3000)
            _isDetecting.value = false
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
        val device = connectedDevice.value ?: return
        storageAccessManager.takePersistablePermission(uri)
        // Store full tree URI so MediaRepository can parse it for SAF queries
        val sourcePath = uri.toString()
        viewModelScope.launch {
            val existing = deviceRepository.getDeviceByVolume(device.volumeUuid)
            val updated = (existing ?: device).copy(sourcePath = sourcePath)
            deviceRepository.saveDevice(updated)
            usbDeviceManager.updateConnectedDevice(updated)
        }
    }

    override fun onCleared() {
        usbDeviceManager.stopListening()
        super.onCleared()
    }
}
