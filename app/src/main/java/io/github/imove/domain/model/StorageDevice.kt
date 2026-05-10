package io.github.imove.domain.model

data class StorageDevice(
    val id: String,
    val volumeUuid: String,
    val volumeLabel: String,
    val sourcePath: String,
    val lastConnected: Long
)
