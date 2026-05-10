package io.github.imove.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transferred_files_table")
data class TransferredFileEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "file_name")
    val fileName: String,
    @ColumnInfo(name = "source_device_id")
    val sourceDeviceId: String,
    @ColumnInfo(name = "transferred_at")
    val transferredAt: Long,
    @ColumnInfo(name = "destination_path")
    val destinationPath: String
)
