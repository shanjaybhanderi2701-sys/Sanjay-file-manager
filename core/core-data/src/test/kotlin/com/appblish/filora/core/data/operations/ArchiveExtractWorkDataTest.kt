package com.appblish.filora.core.data.operations

import androidx.work.workDataOf
import com.appblish.filora.core.common.result.OperationError
import com.appblish.filora.core.data.archive.ExtractionSummary
import com.appblish.filora.core.domain.model.ConflictStrategy
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ArchiveExtractWorkDataTest {
    @Test
    fun `input arguments round-trip through the data bag`() {
        val args = ArchiveExtractArgs(
            archivePath = "/sd/archive.zip",
            destinationDir = "/sd/out",
            strategy = ConflictStrategy.Replace,
        )

        val decoded = ArchiveExtractWorkData.decodeInput(ArchiveExtractWorkData.encodeInput(args))

        assertThat(decoded).isEqualTo(args)
    }

    @Test
    fun `decode returns null when required paths are missing`() {
        assertThat(ArchiveExtractWorkData.decodeInput(workDataOf())).isNull()
    }

    @Test
    fun `unknown strategy falls back to keep-both`() {
        val data = workDataOf(
            "ax_archive" to "/a.zip",
            "ax_dest" to "/out",
            "ax_strategy" to "Bogus",
        )

        assertThat(ArchiveExtractWorkData.decodeInput(data)!!.strategy).isEqualTo(ConflictStrategy.KeepBoth)
    }

    @Test
    fun `success summary round-trips`() {
        val summary = ExtractionSummary(extractedFiles = 5, skippedFiles = 2, createdDirectories = 3)

        val decoded = ArchiveExtractWorkData.decodeSummary(ArchiveExtractWorkData.encodeSuccess(summary))

        assertThat(decoded).isEqualTo(summary)
    }

    @Test
    fun `failure tag round-trips to the same error type`() {
        val encoded = ArchiveExtractWorkData.encodeFailure(OperationError.Io())

        assertThat(ArchiveExtractWorkData.decodeError(encoded)).isInstanceOf(OperationError.Io::class.java)
    }
}
