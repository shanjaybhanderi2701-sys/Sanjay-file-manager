package com.appblish.filora.core.domain.usecase

import com.appblish.filora.core.common.util.MimeTypes
import com.appblish.filora.core.domain.model.FileItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareIntentPlannerTest {
    private fun file(
        name: String,
        mimeType: String? = null,
        extension: String = name.substringAfterLast('.', ""),
        isDirectory: Boolean = false,
    ) = FileItem(
        name = name,
        path = "/storage/emulated/0/$name",
        isDirectory = isDirectory,
        sizeBytes = 1,
        lastModifiedEpochMillis = 0,
        mimeType = mimeType,
        extension = extension,
    )

    @Test
    fun `openType uses declared mime when present`() {
        assertEquals("image/png", ShareIntentPlanner.openType(file("p.png", mimeType = "image/png")))
    }

    @Test
    fun `openType infers from extension when mime missing`() {
        assertEquals("application/pdf", ShareIntentPlanner.openType(file("doc.pdf")))
    }

    @Test
    fun `openType offers wildcard for an unknown type`() {
        assertEquals(MimeTypes.WILDCARD, ShareIntentPlanner.openType(file("data.unknownext")))
    }

    @Test
    fun `plan marks a single file as not multiple`() {
        val plan = ShareIntentPlanner.plan(listOf(file("a.jpg")))
        assertFalse(plan.isMultiple)
        assertEquals("image/jpeg", plan.mimeType)
    }

    @Test
    fun `plan collapses same-category files to a wildcard type`() {
        val plan = ShareIntentPlanner.plan(listOf(file("a.jpg"), file("b.png")))
        assertTrue(plan.isMultiple)
        assertEquals("image/*", plan.mimeType)
    }

    @Test
    fun `plan uses the top-level wildcard for a mixed batch`() {
        val plan = ShareIntentPlanner.plan(listOf(file("a.jpg"), file("b.pdf")))
        assertTrue(plan.isMultiple)
        assertEquals(MimeTypes.WILDCARD, plan.mimeType)
    }

    @Test
    fun `plan tolerates an empty list`() {
        val plan = ShareIntentPlanner.plan(emptyList())
        assertFalse(plan.isMultiple)
        assertEquals(MimeTypes.WILDCARD, plan.mimeType)
    }

    @Test
    fun `planOpen types a file by its open mime and marks it openable`() {
        val plan = ShareIntentPlanner.planOpen(file("clip.mp4", mimeType = "video/mp4"))
        assertTrue(plan.isOpenable)
        assertEquals("video/mp4", plan.mimeType)
    }

    @Test
    fun `planOpen infers the open mime from the extension when mime is missing`() {
        val plan = ShareIntentPlanner.planOpen(file("song.mp3"))
        assertTrue(plan.isOpenable)
        assertEquals("audio/mpeg", plan.mimeType)
    }

    @Test
    fun `planOpen marks a directory as not openable`() {
        val plan = ShareIntentPlanner.planOpen(file("Camera", isDirectory = true))
        assertFalse(plan.isOpenable)
    }
}
