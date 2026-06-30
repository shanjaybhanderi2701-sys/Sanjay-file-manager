package com.appblish.filora.navigation

import com.appblish.filora.core.domain.model.FileItem
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Verifies the Home recents/favorites tap routing (APP-101 / T6.5a). Pure JVM test —
 * [homeItemRoute] is a plain mapping over [FileItem], so no Compose/Navigation runtime
 * is needed to prove a tap lands on the right [Route].
 */
class HomeItemRouteTest {
    private fun fileItem(
        path: String,
        isDirectory: Boolean,
    ) = FileItem(
        name = path.substringAfterLast('/'),
        path = path,
        isDirectory = isDirectory,
        sizeBytes = 0L,
        lastModifiedEpochMillis = 0L,
    )

    @Test
    fun `directory opens the browser at its own location`() {
        val dir = fileItem("/storage/emulated/0/DCIM", isDirectory = true)

        assertEquals(Route.Browser(location = "/storage/emulated/0/DCIM"), homeItemRoute(dir))
    }

    @Test
    fun `file opens the browser at its containing folder`() {
        val file = fileItem("/storage/emulated/0/DCIM/photo.jpg", isDirectory = false)

        assertEquals(Route.Browser(location = "/storage/emulated/0/DCIM"), homeItemRoute(file))
    }

    @Test
    fun `tree-document uri file resolves to its parent segment`() {
        val file =
            fileItem(
                "content://com.android.externalstorage.documents/tree/primary%3ADownload/song.mp3",
                isDirectory = false,
            )

        assertEquals(
            Route.Browser(
                location = "content://com.android.externalstorage.documents/tree/primary%3ADownload",
            ),
            homeItemRoute(file),
        )
    }

    @Test
    fun `root-level file falls back to a non-empty location`() {
        val file = fileItem("/photo.jpg", isDirectory = false)

        // No real parent above root: route to the path itself rather than an empty Browser location.
        assertEquals(Route.Browser(location = "/photo.jpg"), homeItemRoute(file))
    }
}
