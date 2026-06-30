package com.appblish.filora

import android.app.Application
import android.content.pm.ApplicationInfo
import android.os.StrictMode
import dagger.hilt.android.HiltAndroidApp

/** Application entry point. `@HiltAndroidApp` triggers Hilt's code generation and
 * creates the application-level dependency container.
 *
 * `onCreate` is intentionally lean (T147 cold-start): no eager initialization runs
 * here. Hilt builds the dependency graph lazily and `ProfileInstaller` (baseline
 * profiles, T144) handles startup AOT — so the only work added on this hot path is a
 * debug-only [StrictMode] install, which is a no-op in release builds. */
@HiltAndroidApp
class FiloraApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        if (isDebuggable()) {
            enableStrictMode()
        }
    }

    private fun isDebuggable(): Boolean =
        (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    /**
     * T149 — surface accidental main-thread disk/network I/O and leaked resources during
     * development. We use `penaltyLog` rather than `penaltyDeath`: violations are logged
     * (and visible to LeakCanary / logcat) without crashing debug sessions on benign
     * third-party hits. Never installed in release (guarded by [isDebuggable]).
     */
    private fun enableStrictMode() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .penaltyLog()
                .build(),
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedClosableObjects()
                .detectLeakedRegistrationObjects()
                .detectActivityLeaks()
                .penaltyLog()
                .build(),
        )
    }
}
