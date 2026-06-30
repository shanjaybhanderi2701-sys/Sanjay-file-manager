package com.appblish.filora.core.common.result

/**
 * Domain-level error taxonomy. Data/domain layers translate platform exceptions
 * into one of these; the UI maps them to user-facing messages. Raw exceptions
 * never cross layer boundaries.
 */
sealed interface OperationError {
    /** Optional underlying cause for logging — never shown to users. */
    val cause: Throwable?

    data class PermissionDenied(
        override val cause: Throwable? = null
    ) : OperationError

    data class NotFound(
        val path: String? = null,
        override val cause: Throwable? = null
    ) : OperationError

    /** A file/folder already exists at the destination. */
    data class Conflict(
        val path: String? = null,
        override val cause: Throwable? = null
    ) : OperationError

    /**
     * The supplied name is not a valid file/folder name (blank, reserved, too
     * long, or contains illegal characters). The specific reason for inline
     * display is produced by the validator in the domain layer; this coarse
     * variant is the final gate use cases return when an invalid name slips
     * past the UI.
     */
    data class InvalidName(
        override val cause: Throwable? = null
    ) : OperationError

    data class OutOfSpace(
        override val cause: Throwable? = null
    ) : OperationError

    /** Operation was cancelled cooperatively (e.g. user-cancelled worker). */
    data class Cancelled(
        override val cause: Throwable? = null
    ) : OperationError

    data class Io(
        override val cause: Throwable? = null
    ) : OperationError

    data class Unknown(
        override val cause: Throwable? = null
    ) : OperationError
}
