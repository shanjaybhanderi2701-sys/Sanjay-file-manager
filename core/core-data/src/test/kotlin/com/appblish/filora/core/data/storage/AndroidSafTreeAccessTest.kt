package com.appblish.filora.core.data.storage

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.UriPermission
import android.net.Uri
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

/**
 * Unit tests for [AndroidSafTreeAccess]. The `ContentResolver` and its URI-grant
 * bookkeeping are mocked, so these run on the JVM with no device — they pin the
 * grant flags, the read-permission filtering, and the idempotent-release guard
 * that back T1.3's "persisted permission survives restart" guarantee.
 */
class AndroidSafTreeAccessTest {
    private val resolver = mockk<ContentResolver>()
    private val context = mockk<Context> { every { contentResolver } returns resolver }
    private val safTreeAccess = AndroidSafTreeAccess(context)

    private val treeUri = mockk<Uri>()

    @Test
    fun `persist takes a persistable read and write grant`() {
        justRun { resolver.takePersistableUriPermission(any(), any()) }

        safTreeAccess.persist(treeUri)

        val expectedFlags =
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        verify { resolver.takePersistableUriPermission(treeUri, expectedFlags) }
    }

    @Test
    fun `persistedTreeUris returns only read-grant uris`() {
        val readableUri = mockk<Uri>()
        val writeOnlyUri = mockk<Uri>()
        every { resolver.persistedUriPermissions } returns
            listOf(
                permission(readableUri, isRead = true),
                permission(writeOnlyUri, isRead = false),
            )

        assertThat(safTreeAccess.persistedTreeUris()).containsExactly(readableUri)
    }

    @Test
    fun `hasPersistedTree reflects whether any read grant is held`() {
        every { resolver.persistedUriPermissions } returns emptyList()
        assertThat(safTreeAccess.hasPersistedTree()).isFalse()

        every { resolver.persistedUriPermissions } returns
            listOf(permission(treeUri, isRead = true))
        assertThat(safTreeAccess.hasPersistedTree()).isTrue()
    }

    @Test
    fun `release drops a grant we hold`() {
        every { resolver.persistedUriPermissions } returns
            listOf(permission(treeUri, isRead = true))
        justRun { resolver.releasePersistableUriPermission(any(), any()) }

        safTreeAccess.release(treeUri)

        verify { resolver.releasePersistableUriPermission(treeUri, any()) }
    }

    @Test
    fun `release is a no-op for a grant we never held`() {
        every { resolver.persistedUriPermissions } returns emptyList()

        safTreeAccess.release(treeUri)

        verify(exactly = 0) { resolver.releasePersistableUriPermission(any(), any()) }
    }

    private fun permission(
        uri: Uri,
        isRead: Boolean,
    ): UriPermission =
        mockk {
            every { this@mockk.uri } returns uri
            every { isReadPermission } returns isRead
        }
}
