package com.appblish.filora.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure JVM tests for [ViewIntentValidator] (security-impl-audit F1 / B3). Proves the
 * exported `filora://` deep link cannot browse app-private paths, traversal escapes, or
 * un-granted content URIs, while still allowing the legitimate public-storage and
 * granted-SAF-tree targets the feature depends on.
 */
class ViewIntentValidatorTest {
    private val grant = "content://com.android.externalstorage.documents/tree/primary%3ADownload"
    private val validator = ViewIntentValidator(grantedTreeUris = { setOf(grant) })

    // --- location whitelist ---------------------------------------------------------

    @Test
    fun `empty or absent location is the safe default browse root`() {
        assertTrue(validator.isLocationAllowed(""))
        assertTrue(validator.isLocationAllowed(null))
    }

    @Test
    fun `public shared storage paths are allowed`() {
        assertTrue(validator.isLocationAllowed("/storage/emulated/0/DCIM"))
        assertTrue(validator.isLocationAllowed("/storage/emulated/0"))
        assertTrue(validator.isLocationAllowed("/sdcard/Download"))
        assertTrue(validator.isLocationAllowed("/storage/1234-5678/Movies"))
    }

    @Test
    fun `app-private and system paths are rejected`() {
        assertFalse(validator.isLocationAllowed("/data/data/com.appblish.filora/databases/filora.db"))
        assertFalse(validator.isLocationAllowed("/data/user/0/com.appblish.filora/files"))
        assertFalse(validator.isLocationAllowed("/system/etc/hosts"))
        assertFalse(validator.isLocationAllowed("file:///data/data/com.appblish.filora/shared_prefs"))
    }

    @Test
    fun `scoped Android data and obb legs under shared storage are rejected`() {
        assertFalse(validator.isLocationAllowed("/storage/emulated/0/Android/data/com.evil.app/files"))
        assertFalse(validator.isLocationAllowed("/storage/emulated/0/Android/obb/com.appblish.filora"))
    }

    @Test
    fun `traversal segments are rejected even under an allowed root`() {
        assertFalse(validator.isLocationAllowed("/storage/emulated/0/../../data/data/com.appblish.filora"))
        assertFalse(validator.isLocationAllowed("/sdcard/../../etc"))
    }

    @Test
    fun `relative or opaque locations are rejected`() {
        assertFalse(validator.isLocationAllowed("DCIM"))
        assertFalse(validator.isLocationAllowed("../secret"))
    }

    // --- content:// grant matching --------------------------------------------------

    @Test
    fun `granted SAF tree and its descendants are allowed`() {
        assertTrue(validator.isLocationAllowed(grant))
        assertTrue(validator.isLocationAllowed("$grant/document/primary%3ADownload%2Fsub"))
    }

    @Test
    fun `un-granted content uri is rejected`() {
        assertFalse(validator.isLocationAllowed("content://com.android.externalstorage.documents/tree/primary%3ADCIM"))
        assertFalse(validator.isLocationAllowed("content://com.evil.provider/data"))
    }

    @Test
    fun `content uri prefix confusion does not leak past the grant`() {
        // "…Download-evil" must not be treated as nested under "…Download".
        assertFalse(validator.isLocationAllowed("${grant}-evil/document/x"))
    }

    // --- category mirror ------------------------------------------------------------

    @Test
    fun `known category names are allowed and unknown ones are rejected`() {
        assertTrue(validator.isCategoryAllowed("Images"))
        assertTrue(validator.isCategoryAllowed("Downloads"))
        assertFalse(validator.isCategoryAllowed("images")) // case-sensitive enum name
        assertFalse(validator.isCategoryAllowed("/data/data/x"))
        assertFalse(validator.isCategoryAllowed(null))
    }

    // --- host routing ---------------------------------------------------------------

    @Test
    fun `deep-link host routing dispatches to the right check`() {
        assertTrue(validator.isDeepLinkAllowed("browser", "/storage/emulated/0/DCIM", null))
        assertFalse(validator.isDeepLinkAllowed("browser", "/data/data/com.appblish.filora", null))
        assertTrue(validator.isDeepLinkAllowed("category", null, "Video"))
        assertFalse(validator.isDeepLinkAllowed("category", null, "../../etc"))
        assertTrue(validator.isDeepLinkAllowed("categories", null, null))
        assertFalse(validator.isDeepLinkAllowed("unknownhost", "/storage/emulated/0", null))
        assertFalse(validator.isDeepLinkAllowed(null, null, null))
    }
}
