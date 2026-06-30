package com.appblish.filora.core.data.storage

import android.net.Uri

/**
 * Persists and reports Storage Access Framework (SAF) tree grants.
 *
 * SAF is Filora's permission-free browsing path (architecture §5): the user picks
 * a directory through the system document picker
 * ([android.content.Intent.ACTION_OPEN_DOCUMENT_TREE]) and Filora takes a
 * *persistable* read/write grant on the returned tree URI. Unlike a runtime
 * permission, a persisted SAF grant survives process death and reboot — the
 * platform keeps it until the user revokes it or the app releases it — so
 * [persistedTreeUris] is the source of truth for "which trees can I still read
 * after a restart" (FR-1.1, T1.3 AC).
 *
 * The interface keeps the picker UI (which needs an Activity result launcher)
 * decoupled from the `ContentResolver` bookkeeping, and lets the persistence
 * logic be exercised without a device.
 */
interface SafTreeAccess {
    /**
     * Take a persistable read/write grant on [treeUri] — the URI returned by
     * [androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree].
     * The platform deduplicates, so re-persisting an already-held grant is a no-op.
     */
    fun persist(treeUri: Uri)

    /** Tree URIs Filora currently holds a persisted read grant for. */
    fun persistedTreeUris(): List<Uri>

    /** True once at least one tree grant is held, so the gate can skip the picker. */
    fun hasPersistedTree(): Boolean = persistedTreeUris().isNotEmpty()

    /** Drop the persisted grant on [treeUri] (e.g. the user removes a location). */
    fun release(treeUri: Uri)
}
