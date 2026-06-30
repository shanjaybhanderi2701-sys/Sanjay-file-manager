package com.appblish.filora

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * Test runner that substitutes [HiltTestApplication] for the production
 * `Application` so instrumented tests run against a Hilt component they can drive.
 * Wired via `testInstrumentationRunner` in the app's build script.
 */
class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?,
    ): Application = super.newApplication(cl, HiltTestApplication::class.java.name, context)
}
