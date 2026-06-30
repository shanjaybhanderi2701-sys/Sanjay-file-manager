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
        }
    }
