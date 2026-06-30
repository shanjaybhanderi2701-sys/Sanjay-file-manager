package com.appblish.filora.core.common.result

/**
 * The result of a domain operation. Use cases return this so errors are values,
 * not exceptions crossing layer boundaries.
 *
 * Note: this is `com.appblish.filora...Result`, intentionally distinct from
 * `kotlin.Result`. Import this type explicitly where both are in scope.
 */
sealed interface Result<out T> {
    data class Success<out T>(
        val data: T
    ) : Result<T>

    data class Error(
        val error: OperationError
    ) : Result<Nothing>
}

/** Wrap a successful value. */
fun <T> T.asSuccess(): Result<T> = Result.Success(this)

/** Wrap an error. */
fun OperationError.asError(): Result<Nothing> = Result.Error(this)

inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> =
    when (this) {
        is Result.Success -> Result.Success(transform(data))
        is Result.Error -> this
    }

inline fun <T, R> Result<T>.fold(
    onSuccess: (T) -> R,
    onError: (OperationError) -> R,
): R =
    when (this) {
        is Result.Success -> onSuccess(data)
        is Result.Error -> onError(error)
    }

fun <T> Result<T>.getOrNull(): T? = (this as? Result.Success)?.data

inline fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
    if (this is Result.Success) action(data)
    return this
}

inline fun <T> Result<T>.onError(action: (OperationError) -> Unit): Result<T> {
    if (this is Result.Error) action(error)
    return this
}

/** Run [block], converting any thrown exception into a [Result.Error] via [map]. */
inline fun <T> runCatchingResult(
    map: (Throwable) -> OperationError = { OperationError.Unknown(it) },
    block: () -> T,
): Result<T> =
    try {
        Result.Success(block())
    } catch (t: Throwable) {
        if (t is kotlin.coroutines.cancellation.CancellationException) throw t
        Result.Error(map(t))
    }
