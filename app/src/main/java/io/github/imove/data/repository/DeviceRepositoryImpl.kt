package io.github.imove.data.repository

import io.github.imove.data.local.database.dao.DeviceDao
import io.github.imove.data.local.database.entity.DeviceEntity
import io.github.imove.domain.model.StorageDevice
import io.github.imove.domain.repository.DeviceRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRepositoryImpl @Inject constructor(
    private val deviceDao: DeviceDao
) : DeviceRepository {

    override suspend fun getDeviceByVolume(volumeUuid: String): StorageDevice? {
        return deviceDao.getByVolumeUuid(volumeUuid)?.toDomain()
    }

    override suspend fun saveDevice(device: StorageDevice) {
        deviceDao.insert(device.toEntity())
    }

    private fun DeviceEntity.toDomain() = StorageDevice(
        id = id,
        volumeUuid = volumeUuid,
        volumeLabel = volumeLabel,
        sourcePath = sourcePath,
        lastConnected = lastConnected
    )

    private fun StorageDevice.toEntity() = DeviceEntity(
        id = id,
        volumeUuid = volumeUuid,
        volumeLabel = volumeLabel,
        sourcePath = sourcePath,
        lastConnected = lastConnected
    )
}
