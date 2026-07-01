package com.appblish.filora.permission

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject

/**
 * Injectable seam over the ambient media-read permission check used by [MainActivity]'s
 * first-run gate (FR-1.1).
 *
 * The gate decision is otherwise a static call to [StoragePermissions.hasMediaAccess], which
 * reads process-global runtime-permission state. That is untestable in isolation: runtime
 * grants are package-scoped and persist for the whole instrumentation run, so a sibling test
 * that grants media access (e.g. `HomeSmokeTest` via `GrantPermissionRule`) leaks the granted
 * state into `PermissionFlowTest`, flipping the gate to Home — and revoking in-process crashes
 * the self-instrumented test process. Injecting the check lets a test bind a deterministic
 * fake instead of depending on (or mutating) ambient permission state.
 */
interface MediaAccessChecker {
    /** True once the user has granted media read access (see [StoragePermissions.hasMediaAccess]). */
    fun hasMediaAccess(): Boolean
}

/** Production [MediaAccessChecker] delegating to the real runtime-permission check. */
class DefaultMediaAccessChecker
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : MediaAccessChecker {
        override fun hasMediaAccess(): Boolean = StoragePermissions.hasMediaAccess(context)
    }

@Module
@InstallIn(SingletonComponent::class)
abstract class MediaAccessModule {
    @Binds
    abstract fun bindMediaAccessChecker(impl: DefaultMediaAccessChecker): MediaAccessChecker
}
