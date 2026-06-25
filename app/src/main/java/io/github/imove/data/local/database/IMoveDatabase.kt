package io.github.imove.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import io.github.imove.data.local.database.dao.DeviceDao
import io.github.imove.data.local.database.entity.DeviceEntity

@Database(
    entities = [DeviceEntity::class],
    version = 2,
    exportSchema = false
)
abstract class IMoveDatabase : RoomDatabase() {
    abstract fun deviceDao(): DeviceDao
}
