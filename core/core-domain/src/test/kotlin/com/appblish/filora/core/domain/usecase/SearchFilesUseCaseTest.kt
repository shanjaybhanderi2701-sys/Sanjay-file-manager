package com.appblish.filora.core.domain.usecase

import com.appblish.filora.core.common.result.OperationError
import com.appblish.filora.core.common.result.Result
import com.appblish.filora.core.common.result.asError
import com.appblish.filora.core.common.result.asSuccess
import com.appblish.filora.core.domain.model.DeleteOutcome
import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.domain.model.SearchQuery
import com.appblish.filora.core.domain.model.SortOrder
import com.appblish.filora.core.domain.repository.FileRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Test

private const val ROOT = "/storage/emulated/0"

class SearchFilesUseCaseTest {
    @Test
    fun `emits files whose name contains the query, case-insensitively`() =
        runTest {
            val repo =
                TreeFileRepository(
                    ROOT to listOf(file("Report.pdf"), file("notes.txt"), file("budget-REPORT.csv")),
                )

            val matches = search(repo, "report")

            assertThat(matches.map { it.name }).containsExactly("Report.pdf", "budget-REPORT.csv")
        }

    @Test
    fun `recurses into subdirectories and streams matches from every readable subtree`() =
        runTest {
            val repo =
                TreeFileRepository(
                    ROOT to listOf(dir("Docs"), file("songa.mp3")),
                    "$ROOT/Docs" to listOf(file("song-lyrics.txt"), dir("Old")),
                    "$ROOT/Docs/Old" to listOf(file("songbook.pdf")),
                )

            val matches = search(repo, "song")

            assertThat(matches.map { it.name })
                .containsExactly("songa.mp3", "song-lyrics.txt", "songbook.pdf")
        }

    @Test
    fun `matches directory names too`() =
        runTest {
            val repo =
                TreeFileRepository(
                    ROOT to listOf(dir("Music"), file("readme.md")),
                    "$ROOT/Music" to emptyList(),
                )

            val matches = search(repo, "music")

            assertThat(matches.map { it.name }).containsExactly("Music")
        }

    @Test
    fun `excludes hidden entries unless includeHidden is set`() =
        runTest {
            val repo =
                TreeFileRepository(
                    ROOT to listOf(file(".cache-data"), file("data-set.csv")),
                )

            val visible = search(repo, "data")
            assertThat(visible.map { it.name }).containsExactly("data-set.csv")

            val all = search(repo, "data", includeHidden = true)
            assertThat(all.map { it.name }).containsExactly(".cache-data", "data-set.csv")
        }

    @Test
    fun `skips a directory it cannot list but keeps searching siblings`() =
        runTest {
            val repo =
                TreeFileRepository(
                    ROOT to listOf(dir("Locked"), dir("Open")),
                    "$ROOT/Open" to listOf(file("photo-1.jpg")),
                    // "$ROOT/Locked" intentionally absent -> listed as an error
                )

            val matches = search(repo, "photo")

            assertThat(matches.map { it.name }).containsExactly("photo-1.jpg")
        }

    @Test
    fun `emits nothing for a blank query`() =
        runTest {
            val repo = TreeFileRepository(ROOT to listOf(file("anything.txt")))

            assertThat(search(repo, "   ")).isEmpty()
            assertThat(repo.listedPaths).isEmpty()
        }

    @Test
    fun `emits nothing when no root is supplied`() =
        runTest {
            val repo = TreeFileRepository(ROOT to listOf(file("anything.txt")))

            val matches = SearchFilesUseCase(repo)(SearchQuery(text = "any", rootPath = null)).toList()

            assertThat(matches).isEmpty()
            assertThat(repo.listedPaths).isEmpty()
        }

    @Test
    fun `is cancelable - abandoning the flow stops the tree walk early`() =
        runTest {
            // Match lives at the root; a deep subtree exists but should never be walked
            // once the collector takes a single result and cancels the upstream.
            val repo =
                TreeFileRepository(
                    ROOT to listOf(file("match-me.txt"), dir("Deep")),
                    "$ROOT/Deep" to listOf(file("ignored.txt")),
                )

            val first =
                SearchFilesUseCase(repo)(SearchQuery(text = "match", rootPath = ROOT))
                    .take(1)
                    .toList()

            assertThat(first.map { it.name }).containsExactly("match-me.txt")
            assertThat(repo.listedPaths).containsExactly(ROOT)
        }

    private suspend fun search(
        repo: FileRepository,
        text: String,
        includeHidden: Boolean = false,
    ): List<FileItem> =
        SearchFilesUseCase(repo)(
            SearchQuery(text = text, rootPath = ROOT, includeHidden = includeHidden),
        ).toList()

    private fun file(name: String): FileItem =
        FileItem(
            name = name,
            path = "$ROOT/$name",
            isDirectory = false,
            sizeBytes = 1L,
            lastModifiedEpochMillis = 0L,
            isHidden = name.startsWith("."),
        )

    private fun dir(name: String): FileItem =
        FileItem(
            name = name,
            path = "$ROOT/$name",
            isDirectory = true,
            sizeBytes = 0L,
            lastModifiedEpochMillis = 0L,
            isHidden = name.startsWith("."),
        )
}

/**
 * In-memory directory tree keyed by path. A path absent from [tree] lists as a
 * [Result.Error] so tests can exercise the skip-unreadable-subtree branch. Child
 * [FileItem.path] values are taken verbatim, so wire parents and children with
 * matching paths.
 */
private class TreeFileRepository(
    vararg entries: Pair<String, List<FileItem>>,
) : FileRepository {
    private val tree: Map<String, List<FileItem>> =
        entries.associate { (path, items) -> path to items.map { it.copy(path = "$path/${it.name}") } }

    val listedPaths = mutableListOf<String>()

    override fun listDirectory(
        path: String,
        sortOrder: SortOrder,
    ): Flow<Result<List<FileItem>>> {
        listedPaths += path
        val listing = tree[path]
        return flowOf(listing?.asSuccess() ?: OperationError.PermissionDenied().asError())
    }

    override suspend fun getFile(path: String): Result<FileItem> = unsupported()

    override suspend fun createFolder(
        parentPath: String,
        name: String,
    ): Result<FileItem> = unsupported()

    override suspend fun rename(
        path: String,
        newName: String,
    ): Result<FileItem> = unsupported()

    override suspend fun delete(
        paths: List<String>,
        toTrash: Boolean,
    ): Result<DeleteOutcome> = unsupported()

    override suspend fun copy(
        sourcePath: String,
        destinationDir: String,
        destinationName: String,
        overwrite: Boolean,
    ): Result<FileItem> = unsupported()

    private fun unsupported(): Nothing = throw UnsupportedOperationException("not needed for search tests")
}
