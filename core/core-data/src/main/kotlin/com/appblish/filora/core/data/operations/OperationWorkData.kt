package com.appblish.filora.core.data.operations

import androidx.work.Data
import androidx.work.workDataOf
import com.appblish.filora.core.common.result.OperationError
import com.appblish.filora.core.domain.model.ConflictStrategy
import com.appblish.filora.core.domain.model.FileOperationKind
import com.appblish.filora.core.domain.model.OperationProgress

/**
 * Translation between the strongly-typed operation contract and the loosely-typed
 * [Data] bags that WorkManager passes into and out of a worker. Kept in one place
 * (and free of Android framework calls) so the encoding is unit-testable and the
 * worker/scheduler never sprinkle raw string keys around.
 */
internal object OperationWorkData {
    // ---- input keys ----
    private const val KEY_KIND = "op_kind"
    private const val KEY_DEST = "op_dest"
    private const val KEY_TO_TRASH = "op_to_trash"
    private const val KEY_STRATEGY = "op_strategy"

    /** Inline source paths (used when the list is small enough for the Data cap). */
    private const val KEY_SOURCES = "op_sources"

    /** Key into [WorkRequestStore] when the list is too large to inline. */
    private const val KEY_SOURCES_REF = "op_sources_ref"

    // ---- progress keys ----
    private const val KEY_ITEM_INDEX = "pg_index"
    private const val KEY_ITEM_COUNT = "pg_count"
    private const val KEY_CURRENT_NAME = "pg_name"
    private const val KEY_COMPLETED_BYTES = "pg_done_bytes"
    private const val KEY_TOTAL_BYTES = "pg_total_bytes"

    // ---- terminal output keys ----
    private const val KEY_PROCESSED = "out_processed"
    private const val KEY_ERROR_TAG = "out_error"

    /**
     * Inline at most this many sources directly in [Data]; larger lists are handed
     * off through [WorkRequestStore] to dodge WorkManager's ~10 KB Data limit.
     */
    const val INLINE_SOURCE_LIMIT: Int = 32

    fun encodeInput(args: OperationArgs): Data {
        val base = mutableMapOf<String, Any?>(
            KEY_KIND to args.kind.name,
            KEY_DEST to args.destinationDir,
            KEY_TO_TRASH to args.toTrash,
            KEY_STRATEGY to args.conflictStrategy.name,
        )
        if (args.sources.size <= INLINE_SOURCE_LIMIT) {
            base[KEY_SOURCES] = args.sources.toTypedArray()
        } else {
            base[KEY_SOURCES_REF] = args.sourcesRefKey
        }
        return workDataOf(*base.entries.map { it.key to it.value }.toTypedArray())
    }

    /**
     * Rebuilds [OperationArgs] from worker input. [resolveRef] supplies the source
     * list for an out-of-band reference (typically `store::get`); returns `null`
     * when a referenced list is gone (cold restart after process death), which the
     * worker treats as nothing-to-do.
     */
    fun decodeInput(
        data: Data,
        resolveRef: (String) -> List<String>?
    ): OperationArgs? {
        val kind = data.getString(KEY_KIND)?.let { runCatching { FileOperationKind.valueOf(it) }.getOrNull() }
            ?: return null
        val refKey = data.getString(KEY_SOURCES_REF)
        val sources = when {
            refKey != null -> resolveRef(refKey) ?: return null
            else -> data.getStringArray(KEY_SOURCES)?.toList() ?: emptyList()
        }
        val strategy = data
            .getString(KEY_STRATEGY)
            ?.let { runCatching { ConflictStrategy.valueOf(it) }.getOrNull() }
            ?: ConflictStrategy.KeepBoth
        return OperationArgs(
            kind = kind,
            sources = sources,
            destinationDir = data.getString(KEY_DEST),
            toTrash = data.getBoolean(KEY_TO_TRASH, true),
            sourcesRefKey = refKey,
            conflictStrategy = strategy,
        )
    }

    fun encodeProgress(progress: OperationProgress.Running): Data =
        workDataOf(
            KEY_ITEM_INDEX to progress.itemIndex,
            KEY_ITEM_COUNT to progress.itemCount,
            KEY_CURRENT_NAME to progress.currentName,
            KEY_COMPLETED_BYTES to progress.completedBytes,
            KEY_TOTAL_BYTES to progress.totalBytes,
        )

    /** Decodes a [Running] progress snapshot, or `null` if the bag has no progress yet. */
    fun decodeProgress(
        kind: FileOperationKind,
        data: Data
    ): OperationProgress.Running? {
        if (!data.keyValueMap.containsKey(KEY_ITEM_COUNT)) return null
        return OperationProgress.Running(
            kind = kind,
            itemIndex = data.getInt(KEY_ITEM_INDEX, 0),
            itemCount = data.getInt(KEY_ITEM_COUNT, 0),
            currentName = data.getString(KEY_CURRENT_NAME).orEmpty(),
            completedBytes = data.getLong(KEY_COMPLETED_BYTES, 0L),
            totalBytes = data.getLong(KEY_TOTAL_BYTES, 0L),
        )
    }

    fun kindOf(data: Data): FileOperationKind? =
        data.getString(KEY_KIND)?.let { runCatching { FileOperationKind.valueOf(it) }.getOrNull() }

    fun encodeSuccessOutput(processedCount: Int): Data = workDataOf(KEY_PROCESSED to processedCount)

    fun encodeFailureOutput(error: OperationError): Data = workDataOf(KEY_ERROR_TAG to error.wireTag())

    /**
     * Maps a *terminal* WorkInfo's payload to a finished [OperationProgress].
     * [succeeded] reflects WorkManager's own state, [cancelled] its cancelled
     * state; otherwise the error tag in [output] drives a [OperationProgress.Failed].
     */
    fun decodeTerminal(
        kind: FileOperationKind,
        succeeded: Boolean,
        cancelled: Boolean,
        output: Data,
    ): OperationProgress =
        when {
            cancelled -> OperationProgress.Cancelled(kind)
            succeeded -> OperationProgress.Succeeded(kind, output.getInt(KEY_PROCESSED, 0))
            else -> OperationProgress.Failed(kind, operationErrorFromTag(output.getString(KEY_ERROR_TAG)))
        }
}

/**
 * The decoded, validated arguments a file-operation worker runs against. [sources]
 * is the resolved path list (inline or rehydrated from [WorkRequestStore]);
 * [sourcesRefKey] is retained so the worker can evict the stashed list when done.
 */
internal data class OperationArgs(
    val kind: FileOperationKind,
    val sources: List<String>,
    val destinationDir: String?,
    val toTrash: Boolean,
    val sourcesRefKey: String?,
    val conflictStrategy: ConflictStrategy = ConflictStrategy.KeepBoth,
)

/** Maps a domain [OperationError] to a stable wire tag so the UI can re-inflate it from terminal work output. */
internal fun OperationError.wireTag(): String =
    when (this) {
        is OperationError.PermissionDenied -> "permission_denied"
        is OperationError.NotFound -> "not_found"
        is OperationError.Conflict -> "conflict"
        is OperationError.InvalidName -> "invalid_name"
        is OperationError.OutOfSpace -> "out_of_space"
        is OperationError.Cancelled -> "cancelled"
        is OperationError.Io -> "io"
        is OperationError.Unknown -> "unknown"
    }

/** Inverse of [wireTag]; defaults to [OperationError.Unknown] for an unrecognised tag. */
internal fun operationErrorFromTag(tag: String?): OperationError =
    when (tag) {
        "permission_denied" -> OperationError.PermissionDenied()
        "not_found" -> OperationError.NotFound()
        "conflict" -> OperationError.Conflict()
        "invalid_name" -> OperationError.InvalidName()
        "out_of_space" -> OperationError.OutOfSpace()
        "cancelled" -> OperationError.Cancelled()
        "io" -> OperationError.Io()
        else -> OperationError.Unknown()
    }
