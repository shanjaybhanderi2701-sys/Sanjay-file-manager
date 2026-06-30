package com.appblish.filora.core.data.operations

import androidx.work.Data
import androidx.work.workDataOf
import com.appblish.filora.core.common.result.OperationError
import com.appblish.filora.core.domain.model.FileOperationKind
import com.appblish.filora.core.domain.model.OperationProgress
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class OperationWorkDataTest {
    @Test
    fun `small source lists round-trip inline through the data bag`() {
        val args = OperationArgs(
            kind = FileOperationKind.Copy,
            sources = listOf("/a/1.txt", "/a/2.txt"),
            destinationDir = "/dest",
            toTrash = true,
            sourcesRefKey = null,
        )

        val encoded = OperationWorkData.encodeInput(args)
        val decoded = OperationWorkData.decodeInput(encoded) { error("inline list should not hit the store") }

        assertThat(decoded).isNotNull()
        assertThat(decoded!!.kind).isEqualTo(FileOperationKind.Copy)
        assertThat(decoded.sources).containsExactly("/a/1.txt", "/a/2.txt").inOrder()
        assertThat(decoded.destinationDir).isEqualTo("/dest")
        assertThat(decoded.sourcesRefKey).isNull()
    }

    @Test
    fun `large source lists are passed by reference and resolved from the store`() {
        val many = (0..OperationWorkData.INLINE_SOURCE_LIMIT).map { "/big/$it" }
        val args = OperationArgs(
            kind = FileOperationKind.Move,
            sources = many,
            destinationDir = "/dest",
            toTrash = false,
            sourcesRefKey = "op-99",
        )
        val encoded = OperationWorkData.encodeInput(args)

        val decoded = OperationWorkData.decodeInput(encoded) { key ->
            assertThat(key).isEqualTo("op-99")
            many
        }

        assertThat(decoded!!.sources).hasSize(many.size)
        assertThat(decoded.sourcesRefKey).isEqualTo("op-99")
    }

    @Test
    fun `a referenced list that is gone decodes to null so the worker no-ops`() {
        val many = (0..OperationWorkData.INLINE_SOURCE_LIMIT).map { "/big/$it" }
        val encoded = OperationWorkData.encodeInput(
            OperationArgs(FileOperationKind.Copy, many, "/dest", true, "op-evicted"),
        )

        val decoded = OperationWorkData.decodeInput(encoded) { null }

        assertThat(decoded).isNull()
    }

    @Test
    fun `unrecognised input kind decodes to null`() {
        val garbage = workDataOf("op_kind" to "NotAKind")

        assertThat(OperationWorkData.decodeInput(garbage) { emptyList() }).isNull()
    }

    @Test
    fun `running progress round-trips through the data bag`() {
        val running = OperationProgress.Running(
            kind = FileOperationKind.Delete,
            itemIndex = 4,
            itemCount = 9,
            currentName = "clip.mp4",
            completedBytes = 12,
            totalBytes = 48,
        )

        val decoded = OperationWorkData.decodeProgress(
            FileOperationKind.Delete,
            OperationWorkData.encodeProgress(running)
        )

        assertThat(decoded).isEqualTo(running)
    }

    @Test
    fun `decodeProgress returns null when no progress has been published`() {
        assertThat(OperationWorkData.decodeProgress(FileOperationKind.Copy, Data.EMPTY)).isNull()
    }

    @Test
    fun `terminal decode maps work state to finished progress`() {
        val success = OperationWorkData.decodeTerminal(
            FileOperationKind.Copy,
            succeeded = true,
            cancelled = false,
            OperationWorkData.encodeSuccessOutput(7),
        )
        assertThat(success).isEqualTo(OperationProgress.Succeeded(FileOperationKind.Copy, 7))

        val cancelled = OperationWorkData.decodeTerminal(FileOperationKind.Move, false, true, Data.EMPTY)
        assertThat(cancelled).isEqualTo(OperationProgress.Cancelled(FileOperationKind.Move))

        val failed = OperationWorkData.decodeTerminal(
            FileOperationKind.Delete,
            succeeded = false,
            cancelled = false,
            OperationWorkData.encodeFailureOutput(OperationError.OutOfSpace()),
        )
        assertThat(failed).isInstanceOf(OperationProgress.Failed::class.java)
        assertThat((failed as OperationProgress.Failed).error).isInstanceOf(OperationError.OutOfSpace::class.java)
    }

    @Test
    fun `error wire tags round-trip`() {
        val errors = listOf(
            OperationError.PermissionDenied(),
            OperationError.NotFound(),
            OperationError.Conflict(),
            OperationError.InvalidName(),
            OperationError.OutOfSpace(),
            OperationError.Cancelled(),
            OperationError.Io(),
            OperationError.Unknown(),
        )

        errors.forEach { error ->
            val recovered = operationErrorFromTag(error.wireTag())
            assertThat(recovered::class).isEqualTo(error::class)
        }
    }
}
