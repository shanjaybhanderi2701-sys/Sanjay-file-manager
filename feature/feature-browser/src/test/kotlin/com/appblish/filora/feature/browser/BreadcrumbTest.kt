package com.appblish.filora.feature.browser

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Verifies the breadcrumb ancestor trail (FR-2.2 / T048-T049). [breadcrumbSegments] is the
 * pure core of tappable ancestor navigation, so back-stack-up behaviour is provable without
 * a Compose/Navigation runtime.
 */
class BreadcrumbTest {
    private val root = "Internal storage"

    @Test
    fun `filesystem path fans out into cumulative ancestor crumbs`() {
        val crumbs = breadcrumbSegments("/storage/emulated/0/DCIM", root)

        assertThat(crumbs)
            .containsExactly(
                BreadcrumbSegment(label = root, location = "/"),
                BreadcrumbSegment(label = "storage", location = "/storage"),
                BreadcrumbSegment(label = "emulated", location = "/storage/emulated"),
                BreadcrumbSegment(label = "0", location = "/storage/emulated/0"),
                BreadcrumbSegment(label = "DCIM", location = "/storage/emulated/0/DCIM"),
            ).inOrder()
    }

    @Test
    fun `each crumb location is a real prefix a tap can navigate up to`() {
        val crumbs = breadcrumbSegments("/a/b/c", root)

        // Tapping the second-to-last crumb walks up exactly one level.
        val parent = crumbs[crumbs.lastIndex - 1]
        assertThat(parent.location).isEqualTo("/a/b")
    }

    @Test
    fun `blank location collapses to a single root crumb`() {
        val crumbs = breadcrumbSegments("", root)

        assertThat(crumbs).containsExactly(BreadcrumbSegment(label = root, location = ""))
    }

    @Test
    fun `pure root path is a single root crumb`() {
        val crumbs = breadcrumbSegments("/", root)

        assertThat(crumbs).containsExactly(BreadcrumbSegment(label = root, location = "/"))
    }

    @Test
    fun `trailing and doubled separators do not produce empty crumbs`() {
        val crumbs = breadcrumbSegments("/a//b/", root)

        assertThat(crumbs.map { it.label }).containsExactly(root, "a", "b").inOrder()
        assertThat(crumbs.last().location).isEqualTo("/a/b")
    }

    @Test
    fun `saf tree uri is one opaque crumb labelled by its last segment`() {
        val uri = "content://com.android.externalstorage.documents/tree/primary%3ADownload"

        val crumbs = breadcrumbSegments(uri, root)

        assertThat(crumbs).hasSize(1)
        assertThat(crumbs.single().label).isEqualTo("Download")
        // The crumb keeps the full opaque locator so a tap is a valid Browser location.
        assertThat(crumbs.single().location).isEqualTo(uri)
    }
}
