package com.appblish.filora.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FileNameValidatorTest {
    @Test
    fun `trims surrounding whitespace and accepts a clean name`() {
        val result = FileNameValidator.validate("  Reports  ")
        assertThat(result).isEqualTo(FileNameValidation.Valid("Reports"))
    }

    @Test
    fun `allows internal spaces`() {
        val result = FileNameValidator.validate("My Documents")
        assertThat(result).isEqualTo(FileNameValidation.Valid("My Documents"))
    }

    @Test
    fun `blank or whitespace-only is rejected`() {
        assertThat(FileNameValidator.validate("")).isEqualTo(FileNameValidation.Invalid(FileNameError.Blank))
        assertThat(FileNameValidator.validate("   ")).isEqualTo(FileNameValidation.Invalid(FileNameError.Blank))
    }

    @Test
    fun `dot and dot-dot are reserved`() {
        assertThat(FileNameValidator.validate(".")).isEqualTo(FileNameValidation.Invalid(FileNameError.Reserved))
        assertThat(FileNameValidator.validate("..")).isEqualTo(FileNameValidation.Invalid(FileNameError.Reserved))
    }

    @Test
    fun `illegal characters are reported back`() {
        val result = FileNameValidator.validate("a/b:c")
        assertThat(result).isEqualTo(
            FileNameValidation.Invalid(FileNameError.IllegalCharacters(setOf('/', ':'))),
        )
    }

    @Test
    fun `names longer than the max length are rejected`() {
        val tooLong = "x".repeat(FileNameValidator.MAX_LENGTH + 1)
        assertThat(FileNameValidator.validate(tooLong))
            .isEqualTo(FileNameValidation.Invalid(FileNameError.TooLong))
    }

    @Test
    fun `name at exactly the max length is allowed`() {
        val maxName = "x".repeat(FileNameValidator.MAX_LENGTH)
        assertThat(FileNameValidator.validate(maxName)).isEqualTo(FileNameValidation.Valid(maxName))
    }

    @Test
    fun `duplicate sibling name is rejected case-insensitively`() {
        val result = FileNameValidator.validate("photos", existingNames = setOf("Photos", "docs"))
        assertThat(result).isEqualTo(FileNameValidation.Invalid(FileNameError.Duplicate))
    }

    @Test
    fun `non-duplicate name passes when siblings are supplied`() {
        val result = FileNameValidator.validate("videos", existingNames = setOf("Photos", "docs"))
        assertThat(result).isEqualTo(FileNameValidation.Valid("videos"))
    }
}
