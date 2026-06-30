package com.appblish.filora.core.data.operations

import androidx.work.workDataOf
import com.appblish.filora.core.common.result.OperationError
import com.appblish.filora.core.data.archive.CompressionSummary
import com.appblish.filora.core.domain.model.ArchiveProgress
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ArchiveCompressWorkDataTest {
    @Test
    fun `input arguments round-trip through the data bag`() {
        val args = ArchiveCompressArgs(
            sources = listOf("/sd/a.txt", "/sd/folder"),
            destinationArchivePath = "/sd/out.zip",
        )

        val decoded = ArchiveCompressWorkData.decodeInput(ArchiveCompressWorkData.encodeInput(args))

        assertThat(decoded).isEqualTo(args)
    }

    @Test
    fun `decode returns null when destination is missing`() {
        val data = workDataOf("cz_sources" to arrayOf("/sd/a.txt"))

        assertThat(ArchiveCompressWorkData.decodeInput(data)).isNull()
    }

    @Test
    fun `decode returns null when sources are empty`() {
        val data = workDataOf("cz_dest" to "/sd/out.zip")

        assertThat(ArchiveCompressWorkData.decodeInput(data)).isNull()
    }

    @Test
    fun `progress round-trips`() {
        val running = ArchiveProgress.Running(processedEntries = 2, totalEntries = 5, currentName = "b.txt")

        val decoded = ArchiveCompressWorkData.decodeProgress(ArchiveCompressWorkData.encodeProgress(running))

        assertThat(decoded).isEqualTo(running)
    }

    @Test
    fun `success output round-trips to Succeeded`() {
        val encoded = ArchiveCompressWorkData.encodeSuccess("/sd/out.zip", CompressionSummary(entryCount = 7))

        val decoded = ArchiveCompressWorkData.decodeSuccess(encoded)

        assertThat(decoded).isEqualTo(ArchiveProgress.Succeeded(archivePath = "/sd/out.zip", entryCount = 7))
    }

    @Test
    fun `failure tag round-trips to the same error type`() {
        val encoded = ArchiveCompressWorkData.encodeFailure(OperationError.OutOfSpace())

        assertThat(ArchiveCompressWorkData.decodeError(encoded)).isInstanceOf(OperationError.OutOfSpace::class.java)
    }
}
