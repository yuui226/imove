package io.github.imove.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import io.github.imove.data.local.database.dao.DeviceDao
import io.github.imove.data.local.database.dao.TransferredFileDao
import io.github.imove.data.local.database.entity.DeviceEntity
import io.github.imove.data.local.database.entity.TransferredFileEntity

@Database(
    entities = [DeviceEntity::class, TransferredFileEntity::class],
    version = 1,
    exportSchema = false
)
abstract class IMoveDatabase : RoomDatabase() {
    abstract fun deviceDao(): DeviceDao
    abstract fun transferredFileDao(): TransferredFileDao
}
