package com.appblish.filora.core.data.operations

import com.appblish.filora.core.common.result.OperationError
import com.appblish.filora.core.domain.model.FileOperationKind
import com.appblish.filora.core.domain.model.OperationProgress
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class OperationNotificationTextTest {
    @Test
    fun `title reflects the operation kind`() {
        assertThat(OperationNotificationText.title(FileOperationKind.Copy)).isEqualTo("Copying files")
        assertThat(OperationNotificationText.title(FileOperationKind.Move)).isEqualTo("Moving files")
        assertThat(OperationNotificationText.title(FileOperationKind.Delete)).isEqualTo("Deleting files")
    }

    @Test
    fun `running status shows one-based position and current name`() {
        val status = OperationNotificationText.status(
            OperationProgress.Running(
                kind = FileOperationKind.Copy,
                itemIndex = 2,
                itemCount = 12,
                currentName = "report.pdf",
            ),
        )

        assertThat(status).isEqualTo("3 of 12 · report.pdf")
    }

    @Test
    fun `running status omits the separator when the name is blank`() {
        val status = OperationNotificationText.status(
            OperationProgress.Running(
                kind = FileOperationKind.Delete,
                itemIndex = 0,
                itemCount = 5,
                currentName = "",
            ),
        )

        assertThat(status).isEqualTo("1 of 5")
    }

    @Test
    fun `succeeded status pluralises the item count`() {
        assertThat(
            OperationNotificationText.status(OperationProgress.Succeeded(FileOperationKind.Copy, 1)),
        ).isEqualTo("Completed 1 item")
        assertThat(
            OperationNotificationText.status(OperationProgress.Succeeded(FileOperationKind.Copy, 4)),
        ).isEqualTo("Completed 4 items")
    }

    @Test
    fun `failed and cancelled have terminal copy`() {
        assertThat(
            OperationNotificationText.status(
                OperationProgress.Failed(FileOperationKind.Move, OperationError.Io()),
            ),
        ).isEqualTo("Couldn't complete the operation")
        assertThat(
            OperationNotificationText.status(OperationProgress.Cancelled(FileOperationKind.Move)),
        ).isEqualTo("Cancelled")
    }

    @Test
    fun `percent rounds the running fraction into 0 to 100`() {
        val percent = OperationNotificationText.percent(
            OperationProgress.Running(
                kind = FileOperationKind.Copy,
                itemIndex = 0,
                itemCount = 0,
                currentName = "x",
                completedBytes = 30,
                totalBytes = 40,
            ),
        )

        assertThat(percent).isEqualTo(75)
    }
}
