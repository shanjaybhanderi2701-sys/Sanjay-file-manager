package com.appblish.filora.core.common.result

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import org.junit.Test

class ResultTest {
    @Test
    fun `asSuccess wraps value in Success`() {
        val result = 42.asSuccess()
        assertThat(result).isInstanceOf(Result.Success::class.java)
        assertThat((result as Result.Success).data).isEqualTo(42)
    }

    @Test
    fun `asError wraps error in Error`() {
        val error = OperationError.NotFound(path = "/missing")
        val result = error.asError()
        assertThat(result).isInstanceOf(Result.Error::class.java)
        assertThat((result as Result.Error).error).isEqualTo(error)
    }

    @Test
    fun `map transforms a Success value`() {
        val result = "hello".asSuccess().map { it.length }
        assertThat(result).isEqualTo(Result.Success(5))
    }

    @Test
    fun `map leaves an Error untouched`() {
        val error: Result<Int> = OperationError.Io().asError()
        val result = error.map { it * 2 }
        assertThat(result).isSameInstanceAs(error)
    }

    @Test
    fun `fold runs onSuccess branch for Success`() {
        val folded = 7.asSuccess().fold(onSuccess = { "ok:$it" }, onError = { "err" })
        assertThat(folded).isEqualTo("ok:7")
    }

    @Test
    fun `fold runs onError branch for Error`() {
        val folded = OperationError
            .PermissionDenied()
            .asError()
            .fold(onSuccess = { "ok" }, onError = { "err:${it::class.simpleName}" })
        assertThat(folded).isEqualTo("err:PermissionDenied")
    }

    @Test
    fun `getOrNull returns data for Success and null for Error`() {
        assertThat("v".asSuccess().getOrNull()).isEqualTo("v")
        val errored: Result<String> = OperationError.Unknown().asError()
        assertThat(errored.getOrNull()).isNull()
    }

    @Test
    fun `onSuccess invokes action only for Success and returns same result`() {
        var seen: Int? = null
        val original = 3.asSuccess()
        val returned = original.onSuccess { seen = it }
        assertThat(seen).isEqualTo(3)
        assertThat(returned).isSameInstanceAs(original)

        var called = false
        OperationError.Io().asError().onSuccess { called = true }
        assertThat(called).isFalse()
    }

    @Test
    fun `onError invokes action only for Error and returns same result`() {
        var seen: OperationError? = null
        val err = OperationError.Conflict(path = "/dst")
        val returned = err.asError().onError { seen = it }
        assertThat(seen).isEqualTo(err)
        assertThat(returned).isInstanceOf(Result.Error::class.java)

        var called = false
        1.asSuccess().onError { called = true }
        assertThat(called).isFalse()
    }

    @Test
    fun `runCatchingResult wraps a successful block in Success`() {
        val result = runCatchingResult { 10 + 5 }
        assertThat(result).isEqualTo(Result.Success(15))
    }

    @Test
    fun `runCatchingResult maps a thrown exception to Error via mapper`() {
        val boom = IllegalStateException("boom")
        val result = runCatchingResult(map = { OperationError.Io(it) }) { throw boom }
        assertThat(result).isInstanceOf(Result.Error::class.java)
        val error = (result as Result.Error).error
        assertThat(error).isInstanceOf(OperationError.Io::class.java)
        assertThat(error.cause).isSameInstanceAs(boom)
    }

    @Test
    fun `runCatchingResult defaults to Unknown error when no mapper given`() {
        val result = runCatchingResult { error("oops") }
        val error = (result as Result.Error).error
        assertThat(error).isInstanceOf(OperationError.Unknown::class.java)
    }

    @Test
    fun `runCatchingResult rethrows CancellationException to preserve coroutine cancellation`() {
        var caught = false
        try {
            runCatchingResult<Int> { throw CancellationException("cancelled") }
        } catch (e: CancellationException) {
            caught = true
        }
        assertThat(caught).isTrue()
    }
}
