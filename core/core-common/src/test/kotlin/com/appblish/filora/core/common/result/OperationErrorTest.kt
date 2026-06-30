package com.appblish.filora.core.common.result

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class OperationErrorTest {
    @Test
    fun `every variant exposes its cause for logging`() {
        val cause = RuntimeException("root")
        val variants: List<OperationError> =
            listOf(
                OperationError.PermissionDenied(cause),
                OperationError.NotFound(path = "/a", cause = cause),
                OperationError.Conflict(path = "/b", cause = cause),
                OperationError.OutOfSpace(cause),
                OperationError.Cancelled(cause),
                OperationError.Io(cause),
                OperationError.Unknown(cause),
            )
        variants.forEach { assertThat(it.cause).isSameInstanceAs(cause) }
    }

    @Test
    fun `cause defaults to null when omitted`() {
        assertThat(OperationError.PermissionDenied().cause).isNull()
        assertThat(OperationError.OutOfSpace().cause).isNull()
        assertThat(OperationError.Unknown().cause).isNull()
    }

    @Test
    fun `path-carrying variants default path to null`() {
        assertThat(OperationError.NotFound().path).isNull()
        assertThat(OperationError.Conflict().path).isNull()
    }

    @Test
    fun `path-carrying variants retain their path`() {
        assertThat(OperationError.NotFound(path = "/missing").path).isEqualTo("/missing")
        assertThat(OperationError.Conflict(path = "/dst").path).isEqualTo("/dst")
    }

    @Test
    fun `data class equality is value-based`() {
        assertThat(OperationError.NotFound(path = "/x"))
            .isEqualTo(OperationError.NotFound(path = "/x"))
        assertThat(OperationError.NotFound(path = "/x"))
            .isNotEqualTo(OperationError.NotFound(path = "/y"))
    }
}
