package com.appblish.filora.core.data.search

import com.appblish.filora.core.domain.model.FileTypeFilter
import com.appblish.filora.core.domain.model.SearchFilter
import com.appblish.filora.core.domain.model.SearchQuery
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.file.Files

class FileTreeWalkerTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val walker = FileTreeWalker()

    @Test
    fun `recurses subtrees and streams name matches case-insensitively`() =
        runTest {
            val root = temp.root
            file(root, "Report.pdf")
            file(root, "notes.txt")
            val docs = dir(root, "Docs")
            file(docs, "budget-REPORT.csv")
            dir(docs, "ReportsArchive") // a matching directory name

            val names = walk(root, "report").map { it.name }

            assertThat(names)
                .containsExactly("Report.pdf", "budget-REPORT.csv", "ReportsArchive")
        }

    @Test
    fun `excludes hidden entries unless includeHidden is set`() =
        runTest {
            val root = temp.root
            file(root, ".cache-data")
            file(root, "data-set.csv")

            assertThat(walk(root, "data").map { it.name }).containsExactly("data-set.csv")
            assertThat(walk(root, "data", includeHidden = true).map { it.name })
                .containsExactly(".cache-data", "data-set.csv")
        }

    @Test
    fun `AND-combines a type and size filter on top of the name match`() =
        runTest {
            val root = temp.root
            file(root, "holiday-photo.jpg", bytes = 5_000)
            file(root, "thumb-photo.jpg", bytes = 100) // right type, too small
            file(root, "photo-notes.txt", bytes = 5_000) // big enough, wrong type
            dir(root, "photo-dir") // a folder is excluded once any filter is active

            val filter =
                SearchFilter(types = setOf(FileTypeFilter.Image), minSizeBytes = 1_000)
            val names = walk(root, "photo", filter = filter).map { it.name }

            assertThat(names).containsExactly("holiday-photo.jpg")
        }

    @Test
    fun `blank needle with an active filter streams every matching file`() =
        runTest {
            val root = temp.root
            file(root, "a.jpg")
            file(root, "b.png")
            file(root, "c.txt")

            val filter = SearchFilter(types = setOf(FileTypeFilter.Image))
            val names = walk(root, "", filter = filter).map { it.name }

            assertThat(names).containsExactly("a.jpg", "b.png")
        }

    @Test
    fun `is bounded against a symlink loop back to an ancestor`() =
        runTest {
            val root = temp.root
            val a = dir(root, "a")
            file(a, "target.txt")
            // a/back -> root : a naive recursion would revisit forever.
            Files.createSymbolicLink(File(a, "back").toPath(), root.toPath())

            val names = walk(root, "target").map { it.name }

            // The match is found exactly once; the walk terminates instead of spinning.
            assertThat(names).containsExactly("target.txt")
        }

    @Test
    fun `a non-existent root yields nothing without crashing`() =
        runTest {
            val ghost = File(temp.root, "does-not-exist")

            assertThat(walk(ghost, "anything")).isEmpty()
        }

    @Test
    fun `streams the first match without draining the rest of the tree`() =
        runTest {
            val root = temp.root
            file(root, "match-me.txt")
            val deep = dir(root, "Deep")
            file(deep, "match-too.txt")

            val first =
                walker
                    .walk(root, SearchQuery(text = "match", rootPath = root.absolutePath))
                    .take(1)
                    .toList()

            assertThat(first).hasSize(1)
            assertThat(first.single().name).isEqualTo("match-me.txt")
        }

    private suspend fun walk(
        root: File,
        text: String,
        includeHidden: Boolean = false,
        filter: SearchFilter = SearchFilter(),
    ) = walker
        .walk(
            root,
            SearchQuery(
                text = text,
                rootPath = root.absolutePath,
                filter = filter,
                includeHidden = includeHidden,
            ),
        ).toList()

    private fun dir(
        parent: File,
        name: String,
    ): File = File(parent, name).apply { check(mkdir()) }

    private fun file(
        parent: File,
        name: String,
        bytes: Int = 1,
    ): File = File(parent, name).apply { writeBytes(ByteArray(bytes)) }
}
