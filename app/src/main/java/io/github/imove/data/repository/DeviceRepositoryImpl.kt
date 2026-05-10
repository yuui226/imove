package io.github.imove.data.repository

import io.github.imove.data.local.database.dao.DeviceDao
import io.github.imove.data.local.database.entity.DeviceEntity
import io.github.imove.domain.model.StorageDevice
import io.github.imove.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRepositoryImpl @Inject constructor(
    private val deviceDao: DeviceDao
) : DeviceRepository {

    override fun getConnectedDevice(): Flow<StorageDevice?> {
        return deviceDao.getMostRecent().map { entity ->
            entity?.toDomain()
        }
    }

    override suspend fun getDeviceByVolume(volumeUuid: String): StorageDevice? {
        return deviceDao.getByVolumeUuid(volumeUuid)?.toDomain()
    }

    override suspend fun saveDevice(device: StorageDevice) {
        deviceDao.insert(device.toEntity())
    }

    override suspend fun updateSourcePath(deviceId: String, path: String) {
        deviceDao.updateSourcePath(deviceId, path)
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
