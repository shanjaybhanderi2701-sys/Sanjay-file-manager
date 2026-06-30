package com.appblish.filora.core.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DomainModelTest {
    @Test
    fun `storage usedBytes is total minus available`() {
        val volume =
            StorageVolume(
                id = "primary",
                label = "Internal storage",
                rootPath = "/storage/emulated/0",
                totalBytes = 128_000L,
                availableBytes = 28_000L,
                isRemovable = false,
                isPrimary = true,
            )
        assertThat(volume.usedBytes).isEqualTo(100_000L)
    }

    @Test
    fun `storage usedBytes never goes negative`() {
        val volume =
            StorageVolume(
                id = "sd",
                label = "SD card",
                rootPath = "/storage/SDCARD",
                totalBytes = 0L,
                availableBytes = 50L,
                isRemovable = true,
                isPrimary = false,
            )
        assertThat(volume.usedBytes).isEqualTo(0L)
    }

    @Test
    fun `sort order default is name ascending, folders first`() {
        val default = SortOrder.Default
        assertThat(default.by).isEqualTo(SortOrder.By.Name)
        assertThat(default.ascending).isTrue()
        assertThat(default.foldersFirst).isTrue()
    }

    @Test
    fun `file item has sensible defaults`() {
        val item =
            FileItem(
                name = "notes.txt",
                path = "/docs/notes.txt",
                isDirectory = false,
                sizeBytes = 42L,
                lastModifiedEpochMillis = 0L,
            )
        assertThat(item.isHidden).isFalse()
        assertThat(item.mimeType).isNull()
        assertThat(item.childCount).isNull()
    }
}
