package io.github.imove.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "devices_table")
data class DeviceEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "volume_uuid")
    val volumeUuid: String,
    @ColumnInfo(name = "volume_label")
    val volumeLabel: String,
    @ColumnInfo(name = "source_path")
    val sourcePath: String,
    @ColumnInfo(name = "last_connected")
    val lastConnected: Long
)
