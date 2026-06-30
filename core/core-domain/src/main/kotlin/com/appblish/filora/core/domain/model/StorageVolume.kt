package com.appblish.filora.core.domain.model

/** A mountable storage location (internal, SD card, USB OTG). */
data class StorageVolume(
    val id: String,
    val label: String,
    val rootPath: String,
    val totalBytes: Long,
    val availableBytes: Long,
    val isRemovable: Boolean,
    val isPrimary: Boolean,
) {
    val usedBytes: Long get() = (totalBytes - availableBytes).coerceAtLeast(0)
}
