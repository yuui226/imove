package io.github.imove.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.imove.data.usb.UsbDeviceManager
import io.github.imove.domain.model.StorageDevice
import io.github.imove.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
    private val usbDeviceManager: UsbDeviceManager
) : ViewModel() {

    val connectedDevice: StateFlow<StorageDevice?> = usbDeviceManager.connectedDevice

    init {
        usbDeviceManager.startListening()
    }

    fun onDeviceConnected(device: StorageDevice) {
        viewModelScope.launch {
            val existing = deviceRepository.getDeviceByVolume(device.volumeUuid)
            if (existing != null) {
                deviceRepository.saveDevice(existing.copy(lastConnected = System.currentTimeMillis()))
            } else {
                deviceRepository.saveDevice(device)
            }
        }
    }

    override fun onCleared() {
        usbDeviceManager.stopListening()
        super.onCleared()
    }
}
