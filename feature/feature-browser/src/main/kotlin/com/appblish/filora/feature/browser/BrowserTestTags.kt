package com.appblish.filora.feature.browser

/**
 * Compose `testTag`s for the Browser's scroll containers. `MainActivity` maps test tags to
 * resource-ids ([androidx.compose.ui.semantics.testTagsAsResourceId]), so UiAutomator-driven
 * macrobenchmarks can address these lists via `By.res(packageName, tag)` — used by the
 * large-directory frame-timing benchmark that validates NFR-1.2/1.3 (T2.6).
 */
internal object BrowserTestTags {
    const val LIST = "browser_list"
    const val GRID = "browser_grid"
}
