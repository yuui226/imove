package io.github.imove.domain.repository

import io.github.imove.domain.model.StorageDevice
import kotlinx.coroutines.flow.Flow

interface DeviceRepository {
    fun getConnectedDevice(): Flow<StorageDevice?>
    suspend fun getDeviceByVolume(volumeUuid: String): StorageDevice?
    suspend fun saveDevice(device: StorageDevice)
    suspend fun updateSourcePath(deviceId: String, path: String)
}
