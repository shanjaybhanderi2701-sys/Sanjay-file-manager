package com.appblish.filora.core.data.operations

import android.content.Context
import com.appblish.filora.core.common.result.OperationError
import com.appblish.filora.core.data.R
import com.appblish.filora.core.domain.model.FileOperationKind
import com.appblish.filora.core.domain.model.OperationProgress
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test

class OperationNotificationTextTest {
    private val context: Context = mockk()

    @Before
    fun setUp() {
        every { context.getString(R.string.ops_title_copy) } returns "Copying files"
        every { context.getString(R.string.ops_title_move) } returns "Moving files"
        every { context.getString(R.string.ops_title_delete) } returns "Deleting files"
        every { context.getString(R.string.ops_status_pending) } returns "Preparing…"
        every { context.getString(R.string.ops_status_progress, any(), any()) } answers {
            "${args[1]} of ${args[2]}"
        }
        every { context.getString(R.string.ops_status_progress_named, any(), any(), any()) } answers {
            "${args[1]} of ${args[2]} · ${args[3]}"
        }
        every { context.getString(R.string.ops_status_succeeded_one) } returns "Completed 1 item"
        every { context.getString(R.string.ops_status_succeeded_many, any()) } answers {
            "Completed ${args[1]} items"
        }
        every { context.getString(R.string.ops_status_failed) } returns "Couldn't complete the operation"
        every { context.getString(R.string.ops_status_cancelled) } returns "Cancelled"
    }

    @Test
    fun `title reflects the operation kind`() {
        assertThat(OperationNotificationText.title(context, FileOperationKind.Copy)).isEqualTo("Copying files")
        assertThat(OperationNotificationText.title(context, FileOperationKind.Move)).isEqualTo("Moving files")
        assertThat(OperationNotificationText.title(context, FileOperationKind.Delete)).isEqualTo("Deleting files")
    }

    @Test
    fun `running status shows one-based position and current name`() {
        val status = OperationNotificationText.status(
            context,
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
            context,
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
            OperationNotificationText.status(context, OperationProgress.Succeeded(FileOperationKind.Copy, 1)),
        ).isEqualTo("Completed 1 item")
        assertThat(
            OperationNotificationText.status(context, OperationProgress.Succeeded(FileOperationKind.Copy, 4)),
        ).isEqualTo("Completed 4 items")
    }

    @Test
    fun `failed and cancelled have terminal copy`() {
        assertThat(
            OperationNotificationText.status(
                context,
                OperationProgress.Failed(FileOperationKind.Move, OperationError.Io()),
            ),
        ).isEqualTo("Couldn't complete the operation")
        assertThat(
            OperationNotificationText.status(context, OperationProgress.Cancelled(FileOperationKind.Move)),
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
