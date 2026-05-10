package io.github.imove.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.imove.data.local.database.entity.TransferredFileEntity

@Dao
interface TransferredFileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(file: TransferredFileEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM transferred_files_table WHERE file_name = :fileName AND source_device_id = :deviceId)")
    suspend fun exists(fileName: String, deviceId: String): Boolean
}
