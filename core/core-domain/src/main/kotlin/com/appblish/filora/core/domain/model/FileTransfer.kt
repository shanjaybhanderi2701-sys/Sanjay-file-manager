package com.appblish.filora.core.domain.model

import com.appblish.filora.core.common.result.OperationError

/**
 * How a copy/move should handle a name that already exists in the destination
 * directory (FR-3.3). The user picks one strategy for the whole batch; it is
 * applied per item by the conflict resolver.
 */
enum class ConflictStrategy {
    /** Leave the existing entry untouched and do not transfer the source. */
    Skip,

    /** Overwrite the existing entry with the source. */
    Replace,

    /** Transfer the source under a freshly generated, non-colliding name. */
    KeepBoth,
}

/** What happened to a single source in a copy/move batch. */
sealed interface TransferOutcome {
    /** The data now lives at [destination] (copied, or moved after verification). */
    data class Transferred(
        val destination: FileItem
    ) : TransferOutcome

    /** A [ConflictStrategy.Skip] collision: the source was intentionally not transferred. */
    data object Skipped : TransferOutcome

    /** The transfer failed; the source is left intact. */
    data class Failed(
        val error: OperationError
    ) : TransferOutcome
}

/**
 * Per-source result of a copy/move batch. The batch as a whole succeeds (the use
 * case returns [com.appblish.filora.core.common.result.Result.Success]) once the
 * destination is readable; individual failures are reported here so a partial
 * batch surfaces exactly which items did and did not move.
 */
data class TransferResult(
    val source: FileItem,
    val outcome: TransferOutcome,
)
