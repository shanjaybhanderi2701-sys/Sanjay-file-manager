package com.appblish.filora.permission

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.appblish.filora.core.data.storage.SafTreeAccess
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Backs the SAF tree-grant flow on the permission gate (T1.3).
 *
 * The picker UI itself lives in Compose because it needs an Activity result
 * launcher; this view model holds the inject-only [SafTreeAccess] dependency that
 * persists the chosen tree so the grant outlives the process and survives restart.
 */
@HiltViewModel
class PermissionViewModel
    @Inject
    constructor(
        private val safTreeAccess: SafTreeAccess,
    ) : ViewModel() {
        /**
         * Persist the tree the user just picked. Called with the URI returned by
         * [androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree];
         * after this the grant is queryable via [hasPersistedTree] on a cold start.
         */
        fun onTreeGranted(treeUri: Uri) = safTreeAccess.persist(treeUri)

        /** True if a tree grant already exists, so the gate can skip the picker. */
        fun hasPersistedTree(): Boolean = safTreeAccess.hasPersistedTree()
    }
