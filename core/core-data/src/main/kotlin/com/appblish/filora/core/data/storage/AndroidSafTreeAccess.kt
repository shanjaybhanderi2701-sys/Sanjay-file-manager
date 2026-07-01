package com.appblish.filora.core.data.storage

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * [SafTreeAccess] backed by the platform [ContentResolver]'s persisted URI grants.
 *
 * [persist] takes both read and write flags — the document picker hands back a
 * read/write grant, and taking both lets later file-operation milestones reuse
 * the same tree without re-prompting. Because the system stores persisted grants
 * itself, a fresh process observes them through
 * [ContentResolver.getPersistedUriPermissions] with no extra bookkeeping on our
 * side; that is what makes a granted tree survive restart.
 */
class AndroidSafTreeAccess
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : SafTreeAccess {
        private val resolver: ContentResolver get() = context.contentResolver

        override fun persist(treeUri: Uri) {
            resolver.takePersistableUriPermission(treeUri, PERSIST_FLAGS)
            pruneTowardCap(keep = treeUri)
        }

        /**
         * Opportunistically prune the oldest grants when the persisted-URI table
         * approaches the platform's ~512-entry cap (§5.7 / security-impl-audit F3).
         * Left unbounded, a user who keeps adding trees would eventually hit the cap and
         * `takePersistableUriPermission` would start throwing. We only ever act once we
         * cross [PRUNE_THRESHOLD], drop the least-recently-persisted grants down to
         * [PRUNE_TARGET], and never touch [keep] (the grant we just took). Releasing a
         * grant is best-effort — a concurrent revoke is harmless.
         */
        private fun pruneTowardCap(keep: Uri) {
            val grants = resolver.persistedUriPermissions
            if (grants.size < PRUNE_THRESHOLD) return
            grants
                .asSequence()
                .filter { it.uri != keep }
                .sortedBy { it.persistedTime } // oldest first
                .take((grants.size - PRUNE_TARGET).coerceAtLeast(0))
                .forEach { grant ->
                    runCatching {
                        resolver.releasePersistableUriPermission(grant.uri, PERSIST_FLAGS)
                    }
                }
        }

        override fun persistedTreeUris(): List<Uri> =
            resolver.persistedUriPermissions
                .filter { it.isReadPermission }
                .map { it.uri }

        override fun release(treeUri: Uri) {
            // Releasing a URI we never persisted throws SecurityException, so only
            // release grants we actually hold — keeps removal idempotent.
            if (resolver.persistedUriPermissions.any { it.uri == treeUri }) {
                resolver.releasePersistableUriPermission(treeUri, PERSIST_FLAGS)
            }
        }

        private companion object {
            const val PERSIST_FLAGS =
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION

            /**
             * The platform persists at most ~512 URI grants per app. We start pruning a
             * little under that and trim back down to leave comfortable headroom, so a
             * burst of new picks never races the hard cap.
             */
            const val PRUNE_THRESHOLD = 496
            const val PRUNE_TARGET = 448
        }
    }
