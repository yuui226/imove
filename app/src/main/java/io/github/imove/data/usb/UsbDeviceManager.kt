package io.github.imove.data.usb

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
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

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> detectStorageVolumes()
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
    }

    fun stopListening() {
        try {
            context.unregisterReceiver(usbReceiver)
        } catch (_: IllegalArgumentException) {}
    }

    fun detectStorageVolumes() {
        val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
        val volumes = storageManager.storageVolumes

        val removableVolume = volumes.firstOrNull { volume ->
            volume.isRemovable && volume.state == Environment.MEDIA_MOUNTED
        }

        if (removableVolume != null) {
            _connectedDevice.value = volumeToDevice(removableVolume)
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
}

private object Environment {
    const val MEDIA_MOUNTED = "mounted"
}
