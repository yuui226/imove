package io.github.imove.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.imove.data.local.database.entity.DeviceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {
    @Query("SELECT * FROM devices_table WHERE volume_uuid = :volumeUuid LIMIT 1")
    suspend fun getByVolumeUuid(volumeUuid: String): DeviceEntity?

    @Query("SELECT * FROM devices_table ORDER BY last_connected DESC LIMIT 1")
    fun getMostRecent(): Flow<DeviceEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(device: DeviceEntity)

    @Query("UPDATE devices_table SET source_path = :path WHERE id = :deviceId")
    suspend fun updateSourcePath(deviceId: String, path: String)

    @Query("UPDATE devices_table SET last_connected = :time WHERE id = :deviceId")
    suspend fun updateLastConnected(deviceId: String, time: Long)
}
