package com.appblish.filora.core.domain.model

/** How a directory listing is ordered. */
data class SortOrder(
    val by: By = By.Name,
    val ascending: Boolean = true,
    /** Directories grouped before files regardless of [by]. */
    val foldersFirst: Boolean = true,
) {
    enum class By { Name, Size, DateModified, Type }

    companion object {
        val Default = SortOrder()
    }
}
