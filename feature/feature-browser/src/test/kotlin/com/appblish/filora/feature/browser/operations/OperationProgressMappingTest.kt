package com.appblish.filora.feature.browser.operations

import com.appblish.filora.core.common.result.OperationError
import com.appblish.filora.core.domain.model.ArchiveProgress
import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.domain.model.FileOperationKind
import com.appblish.filora.core.domain.model.OperationProgress
import com.appblish.filora.feature.browser.R
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class OperationProgressMappingTest {
    private fun file(
        name: String,
        isDirectory: Boolean = false,
    ) = FileItem(
        name = name,
        path = "/root/$name",
        isDirectory = isDirectory,
        sizeBytes = 0,
        lastModifiedEpochMillis = 0,
        isHidden = false,
    )

    @Test
    fun `single source archive uses its base name`() {
        assertThat(archiveDestinationPath("/root/dest", listOf(file("report.pdf"))))
            .isEqualTo("/root/dest/report.zip")
    }

    @Test
    fun `a trailing slash on the destination is not doubled`() {
        assertThat(archiveDestinationPath("/root/dest/", listOf(file("report.pdf"))))
            .isEqualTo("/root/dest/report.zip")
    }

    @Test
    fun `multiple sources fall back to a generic archive name`() {
        assertThat(archiveDestinationPath("/root", listOf(file("a.txt"), file("b.txt"))))
            .isEqualTo("/root/archive.zip")
    }

    @Test
    fun `running copy maps to a running update with item-granularity fraction`() {
        val update =
            OperationProgress
                .Running(FileOperationKind.Copy, itemIndex = 1, itemCount = 4, currentName = "b.txt")
                .toUpdate("op-1", BatchOperationKind.COPY)

        assertThat(update).isInstanceOf(OperationUpdate.Running::class.java)
        val active = (update as OperationUpdate.Running).active
        assertThat(active.operationId).isEqualTo("op-1")
        assertThat(active.fraction).isEqualTo(0.25f)
        assertThat(active.currentName).isEqualTo("b.txt")
    }

    @Test
    fun `pending maps to an indeterminate running update`() {
        val update = OperationProgress.Pending(FileOperationKind.Move).toUpdate("op-1", BatchOperationKind.MOVE)
        val active = (update as OperationUpdate.Running).active
        assertThat(active.fraction).isNull()
        assertThat(active.currentName).isNull()
    }

    @Test
    fun `success and failure map to the right terminal messages per kind`() {
        val moved =
            OperationProgress
                .Succeeded(FileOperationKind.Move, processedCount = 2)
                .toUpdate("op-1", BatchOperationKind.MOVE)
        assertThat(moved).isEqualTo(OperationUpdate.Terminal(R.string.browser_moved, succeeded = true))

        val failed =
            OperationProgress
                .Failed(FileOperationKind.Copy, OperationError.PermissionDenied())
                .toUpdate("op-1", BatchOperationKind.COPY)
        assertThat(failed).isEqualTo(OperationUpdate.Terminal(R.string.browser_error_permission, succeeded = false))
    }

    @Test
    fun `archive success and cancellation map to zip terminals`() {
        assertThat(ArchiveProgress.Succeeded("/root/a.zip", entryCount = 3).toUpdate("op-1"))
            .isEqualTo(OperationUpdate.Terminal(R.string.browser_zipped, succeeded = true))
        assertThat(ArchiveProgress.Cancelled.toUpdate("op-1"))
            .isEqualTo(OperationUpdate.Terminal(R.string.browser_op_cancelled, succeeded = false))
    }
}
