package com.appblish.filora.core.data.storage

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

/**
 * Enumerates storage volumes via [StorageManager.getStorageVolumes] (API 24+).
 *
 * Resolving a volume's mount directory differs by API level:
 *  - API 30+ exposes [StorageVolume.getDirectory] directly.
 *  - On API 24–29 there is no public accessor, so we fall back to the long-stable
 *    `getPathFile()` reflection (primary storage additionally falls back to
 *    [Environment.getExternalStorageDirectory]).
 *
 * Volumes whose mount path can't be resolved (e.g. an unmounted/ejected SD slot)
 * are skipped — the repository only surfaces volumes it can size.
 */
class AndroidVolumeEnumerator
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val storageManager: StorageManager,
    ) : VolumeEnumerator {
        override fun enumerate(): List<RawVolume> =
            storageManager.storageVolumes.mapNotNull { volume ->
                val root = volume.resolveDirectory() ?: return@mapNotNull null
                RawVolume(
                    id = volume.uuid ?: PRIMARY_ID,
                    label = volume
                        .getDescription(context)
                        .orEmpty()
                        .ifBlank { if (volume.isPrimary) DEFAULT_PRIMARY_LABEL else root.name },
                    rootPath = root.absolutePath,
                    isRemovable = volume.isRemovable,
                    isPrimary = volume.isPrimary,
                )
            }

        private fun StorageVolume.resolveDirectory(): File? {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                directory?.let { return it }
            }
            reflectivePathFile()?.let { return it }
            return if (isPrimary) Environment.getExternalStorageDirectory() else null
        }

        /** API 24–29: `StorageVolume.getPathFile()` is hidden but stable. */
        private fun StorageVolume.reflectivePathFile(): File? =
            runCatching {
                StorageVolume::class.java
                    .getMethod("getPathFile")
                    .invoke(this) as? File
            }.getOrNull()?.takeIf { it.exists() }

        private companion object {
            const val PRIMARY_ID = "primary"
            const val DEFAULT_PRIMARY_LABEL = "Internal storage"
        }
    }
