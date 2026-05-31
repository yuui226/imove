package io.github.imove.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.imove.data.local.database.entity.DeviceEntity

@Dao
interface DeviceDao {
    @Query("SELECT * FROM devices_table WHERE volume_uuid = :volumeUuid LIMIT 1")
    suspend fun getByVolumeUuid(volumeUuid: String): DeviceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(device: DeviceEntity)
}
