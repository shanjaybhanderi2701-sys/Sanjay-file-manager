package com.appblish.filora.core.common.util

/**
 * Text that may come from a literal or an Android string resource, resolved in the
 * UI layer. Kept here (pure JVM) so domain/data can produce user-facing messages
 * without importing Android — a resource id is just an `Int`.
 */
sealed interface UiText {
    data class Dynamic(
        val value: String
    ) : UiText

    data class Resource(
        val resId: Int,
        val args: List<Any> = emptyList()
    ) : UiText

    companion object {
        fun of(value: String): UiText = Dynamic(value)

        fun res(
            resId: Int,
            vararg args: Any
        ): UiText = Resource(resId, args.toList())
    }
}
