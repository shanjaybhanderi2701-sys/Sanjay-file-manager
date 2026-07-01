package com.appblish.filora.navigation

import com.appblish.filora.core.domain.model.MediaCategory

/**
 * Security gate for **externally originated** `filora://` deep links (design §4 / B3;
 * security-impl-audit F1).
 *
 * The `VIEW` + `BROWSABLE` intent-filter on [MainActivity][com.appblish.filora.MainActivity]
 * is exported, so any web page can one-tap fire a link such as
 * `filora://browser?location=/data/data/com.appblish.filora/databases/filora.db` or a
 * `content://` the app never granted. Without validation the browser would open an
 * attacker-chosen location the user never picked — a confused-deputy / internal-path
 * disclosure.
 *
 * This validator is a **whitelist**: a location is allowed only when it is
 * - empty (the app's own default "Browse" entry point), or
 * - a filesystem path under public shared storage ([ALLOWED_ROOTS]) that contains no
 *   `..` traversal segment and no scoped app-private (`Android/data`, `Android/obb`) leg, or
 * - a `content://` URI that is exactly one of, or nested under, a SAF tree the app
 *   currently holds a persisted grant for.
 *
 * Everything else — app-private `/data/**`, `/system`, other apps' dirs, un-granted
 * content URIs, unknown hosts, malformed links — is rejected. Callers translate a
 * rejection into a fall-back to [Route.Home]; the validator itself never throws.
 *
 * The class holds no Android framework types so it is exercised by a plain JVM unit
 * test; [MainActivity][com.appblish.filora.MainActivity] parses the inbound `Uri` and
 * feeds the pieces in.
 *
 * @param grantedTreeUris supplies the string form of the SAF tree URIs the app holds a
 *   persisted read grant for (from
 *   [SafTreeAccess][com.appblish.filora.core.data.storage.SafTreeAccess]); evaluated
 *   lazily so validation always sees the current grant table.
 */
class ViewIntentValidator(
    private val grantedTreeUris: () -> Set<String>,
) {
    /**
     * Decide whether an inbound `filora://` VIEW deep link may open its target, or must
     * fall back to Home. [host] is the URI authority (`browser` / `category` /
     * `categories`); [locationArg] and [categoryArg] are the relevant decoded query
     * params (either may be `null` when absent).
     */
    fun isDeepLinkAllowed(
        host: String?,
        locationArg: String?,
        categoryArg: String?,
    ): Boolean =
        when (host?.lowercase()) {
            HOST_BROWSER -> isLocationAllowed(locationArg)
            HOST_CATEGORY -> isCategoryAllowed(categoryArg)
            HOST_CATEGORIES -> true // the hub grid carries no free-form args
            else -> false // unknown/absent host: never route an external intent into the graph
        }

    /**
     * True when [location] is safe to hand to the browser. `null`/empty means "no
     * explicit target" — the app's default browse root — which is always safe.
     */
    fun isLocationAllowed(location: String?): Boolean {
        val loc = location ?: return true
        if (loc.isEmpty()) return true

        if (loc.startsWith(CONTENT_SCHEME)) {
            if (hasTraversalSegment(loc)) return false
            return isWithinGrantedTree(loc)
        }

        // Anything else is treated as a filesystem path (an optional file:// scheme aside).
        val path = loc.removePrefix(FILE_SCHEME)
        if (!path.startsWith("/")) return false // relative / opaque input is untrusted
        if (hasTraversalSegment(path)) return false
        if (isScopedAppDir(path)) return false // Android/data|obb is not public shared storage
        return isUnderAllowedRoot(path)
    }

    /** True only for the exact name of a known [MediaCategory]; guards `valueOf` crashes. */
    fun isCategoryAllowed(category: String?): Boolean {
        val name = category ?: return false
        return MediaCategory.entries.any { it.name == name }
    }

    private fun isWithinGrantedTree(uri: String): Boolean =
        grantedTreeUris().any { grant -> uri == grant || uri.startsWith("$grant/") }

    private fun isUnderAllowedRoot(path: String): Boolean {
        val normalised = path.trimEnd('/')
        return ALLOWED_ROOTS.any { root -> normalised == root || normalised.startsWith("$root/") }
    }

    private fun isScopedAppDir(path: String): Boolean {
        val lower = path.lowercase().trimEnd('/')
        return lower.contains("/android/data/") ||
            lower.contains("/android/obb/") ||
            lower.endsWith("/android/data") ||
            lower.endsWith("/android/obb")
    }

    private fun hasTraversalSegment(value: String): Boolean =
        value.split('/', '\\').any { it == ".." }

    private companion object {
        const val HOST_BROWSER = "browser"
        const val HOST_CATEGORY = "category"
        const val HOST_CATEGORIES = "categories"
        const val CONTENT_SCHEME = "content://"
        const val FILE_SCHEME = "file://"

        /**
         * Public shared-storage roots the user can legitimately be pointed at. Everything
         * outside these (all of `/data`, `/system`, other apps) is rejected by omission;
         * granted SAF trees are allowed separately via [grantedTreeUris].
         */
        val ALLOWED_ROOTS = setOf("/storage", "/sdcard")
    }
}
