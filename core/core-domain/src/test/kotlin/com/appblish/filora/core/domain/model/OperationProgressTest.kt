package com.appblish.filora.core.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class OperationProgressTest {
    @Test
    fun `running fraction prefers byte granularity when total bytes known`() {
        val progress = OperationProgress.Running(
            kind = FileOperationKind.Copy,
            itemIndex = 0,
            itemCount = 10,
            currentName = "a.bin",
            completedBytes = 25,
            totalBytes = 100,
        )

        assertThat(progress.fraction).isEqualTo(0.25f)
    }

    @Test
    fun `running fraction falls back to item granularity when bytes unknown`() {
        val progress = OperationProgress.Running(
            kind = FileOperationKind.Move,
            itemIndex = 3,
            itemCount = 12,
            currentName = "b.txt",
        )

        assertThat(progress.fraction).isEqualTo(0.25f)
    }

    @Test
    fun `running fraction is zero when neither bytes nor items are known`() {
        val progress = OperationProgress.Running(
            kind = FileOperationKind.Delete,
            itemIndex = 0,
            itemCount = 0,
            currentName = "",
        )

        assertThat(progress.fraction).isEqualTo(0f)
    }

    @Test
    fun `running fraction is clamped to one when counters overflow`() {
        val progress = OperationProgress.Running(
            kind = FileOperationKind.Copy,
            itemIndex = 0,
            itemCount = 1,
            currentName = "big.iso",
            completedBytes = 200,
            totalBytes = 100,
        )

        assertThat(progress.fraction).isEqualTo(1f)
    }
}
