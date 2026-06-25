package io.github.imove.domain.model

data class StorageDevice(
    val id: String,
    val volumeUuid: String,
    val volumeLabel: String,
    val sourcePath: String,
    val lastConnected: Long
) {
    /** True when this represents a folder picked from local phone storage rather than a USB device. */
    val isLocal: Boolean get() = id == LOCAL_DEVICE_ID

    companion object {
        const val LOCAL_DEVICE_ID = "local"
    }
}
