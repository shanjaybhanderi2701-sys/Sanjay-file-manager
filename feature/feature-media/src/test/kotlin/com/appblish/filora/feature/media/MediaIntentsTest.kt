package com.appblish.filora.feature.media

import com.appblish.filora.core.domain.model.FileItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit coverage for [MediaIntents]' pure share-eligibility rule. Intent dispatch
 * itself needs the Android runtime (no Robolectric in the catalog), so this targets
 * the platform-free [MediaIntents.shareableItems] seam; MIME typing is covered by
 * `ShareIntentPlannerTest` in core-domain.
 */
class MediaIntentsTest {
    private fun item(
        name: String,
        path: String,
        isDirectory: Boolean = false,
    ) = FileItem(
        name = name,
        path = path,
        isDirectory = isDirectory,
        sizeBytes = 10,
        lastModifiedEpochMillis = 0,
    )

    @Test
    fun `keeps content-uri files`() {
        val items =
            listOf(
                item("a.jpg", "content://media/external/images/1"),
                item("b.mp4", "content://media/external/video/2"),
            )

        assertEquals(items, MediaIntents.shareableItems(items))
    }

    @Test
    fun `drops directories`() {
        val dir = item("folder", "content://media/external/dir/1", isDirectory = true)
        val file = item("a.jpg", "content://media/external/images/1")

        assertEquals(listOf(file), MediaIntents.shareableItems(listOf(dir, file)))
    }

    @Test
    fun `drops non-content locators`() {
        val raw = item("a.jpg", "/storage/emulated/0/a.jpg")
        val content = item("b.jpg", "content://media/external/images/2")

        assertEquals(listOf(content), MediaIntents.shareableItems(listOf(raw, content)))
    }

    @Test
    fun `empty when nothing shareable`() {
        val items =
            listOf(
                item("folder", "content://media/external/dir/1", isDirectory = true),
                item("a.jpg", "/storage/emulated/0/a.jpg"),
            )

        assertTrue(MediaIntents.shareableItems(items).isEmpty())
    }
}
