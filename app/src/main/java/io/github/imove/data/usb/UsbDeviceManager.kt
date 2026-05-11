package io.github.imove.data.usb

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.os.Handler
import android.os.Looper
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.imove.domain.model.StorageDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsbDeviceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _connectedDevice = MutableStateFlow<StorageDevice?>(null)
    val connectedDevice: StateFlow<StorageDevice?> = _connectedDevice.asStateFlow()

    private val handler = Handler(Looper.getMainLooper())

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    // Storage volume may not be mounted immediately after attach
                    detectStorageVolumes()
                    handler.postDelayed({ detectStorageVolumes() }, 1000)
                    handler.postDelayed({ detectStorageVolumes() }, 3000)
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    _connectedDevice.value = null
                }
            }
        }
    }

    fun startListening() {
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        context.registerReceiver(usbReceiver, filter)
        detectStorageVolumes()
        // Retry in case volumes are still mounting when app is launched by USB intent
        handler.postDelayed({ detectStorageVolumes() }, 1000)
        handler.postDelayed({ detectStorageVolumes() }, 3000)
    }

    fun stopListening() {
        handler.removeCallbacksAndMessages(null)
        try {
            context.unregisterReceiver(usbReceiver)
        } catch (_: IllegalArgumentException) {}
    }

    fun updateConnectedDevice(device: StorageDevice) {
        _connectedDevice.value = device
    }

    fun detectStorageVolumes() {
        val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
        val volumes = storageManager.storageVolumes

        val removableVolume = volumes.firstOrNull { volume ->
            volume.isRemovable && volume.state == MEDIA_MOUNTED
        }

        if (removableVolume != null) {
            val newDevice = volumeToDevice(removableVolume)
            val existing = _connectedDevice.value
            // Don't overwrite a device that already has its sourcePath restored
            if (existing == null || existing.volumeUuid != newDevice.volumeUuid) {
                _connectedDevice.value = newDevice
            }
        }
    }

    private fun volumeToDevice(volume: StorageVolume): StorageDevice {
        val uuid = volume.uuid ?: UUID.randomUUID().toString()
        val label = volume.getDescription(context) ?: "Unknown Device"

        return StorageDevice(
            id = uuid,
            volumeUuid = uuid,
            volumeLabel = label,
            sourcePath = "",
            lastConnected = System.currentTimeMillis()
        )
    }

    companion object {
        private const val MEDIA_MOUNTED = "mounted"
    }
}
