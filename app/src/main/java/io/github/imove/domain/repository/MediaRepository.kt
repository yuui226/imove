package io.github.imove.domain.repository

import io.github.imove.domain.model.MediaFile
import io.github.imove.domain.model.StorageDevice
import kotlinx.coroutines.flow.Flow

interface MediaRepository {
    fun getFilesFromDevice(device: StorageDevice): Flow<List<MediaFile>>
    fun getFilesByDateRange(device: StorageDevice, startDate: Long, endDate: Long): Flow<List<MediaFile>>
    suspend fun isFileTransferred(fileName: String, deviceId: String): Boolean
    suspend fun markAsTransferred(file: MediaFile, deviceId: String)
}
