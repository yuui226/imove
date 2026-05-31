package io.github.imove.domain.repository

import io.github.imove.domain.model.StorageDevice

interface DeviceRepository {
    suspend fun getDeviceByVolume(volumeUuid: String): StorageDevice?
    suspend fun saveDevice(device: StorageDevice)
}
