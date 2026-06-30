package com.appblish.filora.core.common.util

import org.junit.Assert.assertEquals
import org.junit.Test

class MimeTypesTest {
    @Test
    fun `fromExtension maps known types case-insensitively`() {
        assertEquals("image/jpeg", MimeTypes.fromExtension("JPG"))
        assertEquals("application/pdf", MimeTypes.fromExtension("pdf"))
        assertEquals("video/mp4", MimeTypes.fromExtension("m4v"))
    }

    @Test
    fun `fromExtension returns null for unknown extension`() {
        assertEquals(null, MimeTypes.fromExtension("xyz"))
        assertEquals(null, MimeTypes.fromExtension(""))
    }

    @Test
    fun `resolve prefers a concrete declared type`() {
        assertEquals("image/png", MimeTypes.resolve("image/png", "txt"))
    }

    @Test
    fun `resolve falls back to extension when declared type is missing or wildcard`() {
        assertEquals("application/pdf", MimeTypes.resolve(null, "pdf"))
        assertEquals("application/pdf", MimeTypes.resolve("", "pdf"))
        assertEquals("image/jpeg", MimeTypes.resolve("*/*", "jpg"))
        assertEquals("audio/mpeg", MimeTypes.resolve("audio/*", "mp3"))
    }

    @Test
    fun `resolve degrades to wildcard for fully unknown entries`() {
        assertEquals(MimeTypes.WILDCARD, MimeTypes.resolve(null, "unknownext"))
        assertEquals(MimeTypes.WILDCARD, MimeTypes.resolve("", ""))
    }

    @Test
    fun `resolve keeps a category wildcard when no extension match exists`() {
        assertEquals("image/*", MimeTypes.resolve("image/*", "noext"))
    }

    @Test
    fun `commonType returns the exact type when all match`() {
        assertEquals("image/jpeg", MimeTypes.commonType(listOf("image/jpeg", "image/jpeg")))
    }

    @Test
    fun `commonType collapses a shared top-level to a wildcard`() {
        assertEquals("image/*", MimeTypes.commonType(listOf("image/jpeg", "image/png")))
    }

    @Test
    fun `commonType falls back to wildcard for mixed top-levels`() {
        assertEquals(MimeTypes.WILDCARD, MimeTypes.commonType(listOf("image/png", "application/pdf")))
    }

    @Test
    fun `commonType handles empty and single-item inputs`() {
        assertEquals(MimeTypes.WILDCARD, MimeTypes.commonType(emptyList()))
        assertEquals("application/pdf", MimeTypes.commonType(listOf("application/pdf")))
    }
}
