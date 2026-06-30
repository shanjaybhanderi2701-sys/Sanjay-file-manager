package com.appblish.filora.core.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DirectoryOrderingTest {
    private fun entry(
        name: String,
        isDirectory: Boolean = false,
        size: Long = 0,
        modified: Long = 0,
    ) = FileItem(
        name = name,
        path = "/root/$name",
        isDirectory = isDirectory,
        sizeBytes = size,
        lastModifiedEpochMillis = modified,
        extension = name.substringAfterLast('.', ""),
        isHidden = name.startsWith("."),
    )

    private val sample =
        listOf(
            entry("banana.txt", size = 30, modified = 300),
            entry("Apple", isDirectory = true, modified = 100),
            entry("cherry.mp3", size = 10, modified = 200),
            entry("delta", isDirectory = true, modified = 400),
            entry("avocado.txt", size = 20, modified = 500),
        )

    @Test
    fun `folders come before files by default, each alphabetical`() {
        val ordered = sample.ordered(SortOrder.Default)

        assertThat(ordered.map { it.name })
            .containsExactly("Apple", "delta", "avocado.txt", "banana.txt", "cherry.mp3")
            .inOrder()
    }

    @Test
    fun `name sort is case-insensitive`() {
        val items = listOf(entry("Zebra.txt"), entry("apple.txt"), entry("Banana.txt"))

        val ordered = items.ordered(SortOrder(by = SortOrder.By.Name))

        assertThat(ordered.map { it.name }).containsExactly("apple.txt", "Banana.txt", "Zebra.txt").inOrder()
    }

    @Test
    fun `descending name keeps folders first but reverses within each group`() {
        val ordered = sample.ordered(SortOrder(by = SortOrder.By.Name, ascending = false))

        assertThat(ordered.map { it.name })
            .containsExactly("delta", "Apple", "cherry.mp3", "banana.txt", "avocado.txt")
            .inOrder()
    }

    @Test
    fun `size sort orders files by bytes ascending`() {
        val ordered =
            sample
                .filterNot { it.isDirectory }
                .ordered(SortOrder(by = SortOrder.By.Size, foldersFirst = false))

        assertThat(ordered.map { it.name }).containsExactly("cherry.mp3", "avocado.txt", "banana.txt").inOrder()
    }

    @Test
    fun `date sort orders by last modified descending`() {
        val ordered =
            sample.ordered(SortOrder(by = SortOrder.By.DateModified, ascending = false, foldersFirst = false))

        assertThat(ordered.map { it.name })
            .containsExactly("avocado.txt", "delta", "banana.txt", "cherry.mp3", "Apple")
            .inOrder()
    }

    @Test
    fun `type sort groups by extension then name`() {
        val items =
            listOf(
                entry("song.mp3"),
                entry("notes.txt"),
                entry("photo.jpg"),
                entry("archive.txt"),
            )

        val ordered = items.ordered(SortOrder(by = SortOrder.By.Type, foldersFirst = false))

        // jpg < mp3 < txt; within txt, archive before notes by name.
        assertThat(ordered.map { it.name })
            .containsExactly("photo.jpg", "song.mp3", "archive.txt", "notes.txt")
            .inOrder()
    }

    @Test
    fun `foldersFirst disabled mixes directories into the key order`() {
        val ordered = sample.ordered(SortOrder(by = SortOrder.By.Name, foldersFirst = false))

        assertThat(ordered.first().name).isEqualTo("Apple")
        assertThat(ordered.map { it.name })
            .containsExactly("Apple", "avocado.txt", "banana.txt", "cherry.mp3", "delta")
            .inOrder()
    }
}
