package com.appblish.filora.core.data.storage

/**
 * A storage volume as reported by the platform, before sizes are computed.
 *
 * [rootPath] is the mount directory's absolute path; the repository derives
 * total/available bytes from it via `java.io.File`, keeping the byte arithmetic
 * pure-JVM and unit-testable. The Android-specific work — talking to
 * `StorageManager` and resolving the mount path across API levels — lives behind
 * [VolumeEnumerator] so this layer's mapping logic can be tested without a device.
 */
data class RawVolume(
    val id: String,
    val label: String,
    val rootPath: String,
    val isRemovable: Boolean,
    val isPrimary: Boolean,
)

/** Lists the device's mounted storage volumes (internal, SD card, USB OTG). */
fun interface VolumeEnumerator {
    fun enumerate(): List<RawVolume>
}
