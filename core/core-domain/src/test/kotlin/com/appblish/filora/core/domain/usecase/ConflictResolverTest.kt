package com.appblish.filora.core.domain.usecase

import com.appblish.filora.core.domain.model.ConflictStrategy
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ConflictResolverTest {
    @Test
    fun `no collision proceeds with the original name regardless of strategy`() {
        ConflictStrategy.entries.forEach { strategy ->
            val resolution =
                ConflictResolver.resolve(
                    sourceName = "report.pdf",
                    isDirectory = false,
                    existingNames = setOf("notes.txt"),
                    strategy = strategy,
                )

            assertThat(resolution).isEqualTo(ConflictResolution.Proceed("report.pdf", overwrite = false))
        }
    }

    @Test
    fun `skip strategy returns Skip on a collision`() {
        val resolution =
            ConflictResolver.resolve(
                sourceName = "report.pdf",
                isDirectory = false,
                existingNames = setOf("report.pdf"),
                strategy = ConflictStrategy.Skip,
            )

        assertThat(resolution).isEqualTo(ConflictResolution.Skip)
    }

    @Test
    fun `replace strategy overwrites on a collision`() {
        val resolution =
            ConflictResolver.resolve(
                sourceName = "report.pdf",
                isDirectory = false,
                existingNames = setOf("report.pdf"),
                strategy = ConflictStrategy.Replace,
            )

        assertThat(resolution).isEqualTo(ConflictResolution.Proceed("report.pdf", overwrite = true))
    }

    @Test
    fun `keep-both inserts the counter before the extension`() {
        val resolution =
            ConflictResolver.resolve(
                sourceName = "report.pdf",
                isDirectory = false,
                existingNames = setOf("report.pdf"),
                strategy = ConflictStrategy.KeepBoth,
            )

        assertThat(resolution).isEqualTo(ConflictResolution.Proceed("report (1).pdf", overwrite = false))
    }

    @Test
    fun `keep-both increments past names already taken`() {
        val resolution =
            ConflictResolver.resolve(
                sourceName = "report.pdf",
                isDirectory = false,
                existingNames = setOf("report.pdf", "report (1).pdf"),
                strategy = ConflictStrategy.KeepBoth,
            )

        assertThat(resolution).isEqualTo(ConflictResolution.Proceed("report (2).pdf", overwrite = false))
    }

    @Test
    fun `keep-both on a directory appends the counter to the whole name`() {
        val resolution =
            ConflictResolver.resolve(
                sourceName = "Photos",
                isDirectory = true,
                existingNames = setOf("Photos"),
                strategy = ConflictStrategy.KeepBoth,
            )

        assertThat(resolution).isEqualTo(ConflictResolution.Proceed("Photos (1)", overwrite = false))
    }

    @Test
    fun `keep-both on a dotfile appends the counter at the end`() {
        val resolution =
            ConflictResolver.resolve(
                sourceName = ".env",
                isDirectory = false,
                existingNames = setOf(".env"),
                strategy = ConflictStrategy.KeepBoth,
            )

        assertThat(resolution).isEqualTo(ConflictResolution.Proceed(".env (1)", overwrite = false))
    }

    @Test
    fun `collisions are detected case-insensitively`() {
        val replace =
            ConflictResolver.resolve(
                sourceName = "report.pdf",
                isDirectory = false,
                existingNames = setOf("REPORT.PDF"),
                strategy = ConflictStrategy.Replace,
            )

        assertThat(replace).isEqualTo(ConflictResolution.Proceed("report.pdf", overwrite = true))
    }
}
