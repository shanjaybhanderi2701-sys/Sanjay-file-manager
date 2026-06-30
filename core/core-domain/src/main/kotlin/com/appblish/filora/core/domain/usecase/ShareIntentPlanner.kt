package com.appblish.filora.core.domain.usecase

import com.appblish.filora.core.common.util.MimeTypes
import com.appblish.filora.core.domain.model.FileItem

/**
 * The MIME type to advertise and whether a multi-file share sheet is required for
 * a set of files (FR-10.2). Pure data; the Android layer turns it into the actual
 * `ACTION_SEND` / `ACTION_SEND_MULTIPLE` intent.
 */
data class SharePlan(
    val mimeType: String,
    val isMultiple: Boolean,
)

/**
 * Decides, from domain [FileItem]s alone, how an open/share intent should be typed
 * (FR-10.1, FR-10.2). Kept free of Android types so the type-negotiation rules stay
 * unit-testable; URI resolution and intent dispatch live in the feature layer.
 */
object ShareIntentPlanner {
    /** Best MIME type for opening a single file with `ACTION_VIEW` (FR-10.1). */
    fun openType(item: FileItem): String = mimeOf(item)

    /**
     * Plan for sharing [items] via the system share sheet. Directories are not
     * shareable via `ACTION_SEND`, so callers should filter them out beforehand;
     * an empty list yields a wildcard single-file plan.
     */
    fun plan(items: List<FileItem>): SharePlan =
        SharePlan(
            mimeType = MimeTypes.commonType(items.map(::mimeOf)),
            isMultiple = items.size > 1,
        )

    private fun mimeOf(item: FileItem): String = MimeTypes.resolve(item.mimeType, item.extension)
}
