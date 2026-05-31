package io.github.imove.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import io.github.imove.data.local.database.entity.TransferredFileEntity

@Dao
interface TransferredFileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(file: TransferredFileEntity)
}
