package com.appblish.filora.core.data.search

import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.domain.model.SearchProgress
import com.appblish.filora.core.domain.model.SearchQuery
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class FileSystemSearchRepositoryTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val documentTreeWalker = mockk<DocumentTreeWalker>()
    private val repository =
        FileSystemSearchRepository(
            fileTreeWalker = FileTreeWalker(),
            documentTreeWalker = documentTreeWalker,
            ioDispatcher = UnconfinedTestDispatcher(),
        )

    @Test
    fun `blank query short-circuits to a lone Completed without walking`() =
        runTest {
            file(temp.root, "anything.txt")

            val progress =
                repository.search(SearchQuery(text = "   ", rootPath = temp.root.absolutePath)).toList()

            assertThat(progress).containsExactly(SearchProgress.Completed(matchCount = 0))
            verify(exactly = 0) { documentTreeWalker.walk(any(), any()) }
        }

    @Test
    fun `a null root short-circuits to Completed`() =
        runTest {
            val progress = repository.search(SearchQuery(text = "report", rootPath = null)).toList()

            assertThat(progress).containsExactly(SearchProgress.Completed(matchCount = 0))
        }

    @Test
    fun `wraps file-walk matches with a running count and a terminal Completed`() =
        runTest {
            file(temp.root, "report-q1.pdf")
            file(temp.root, "report-q2.pdf")
            file(temp.root, "unrelated.txt")

            val progress =
                repository.search(SearchQuery(text = "report", rootPath = temp.root.absolutePath)).toList()

            val matches = progress.filterIsInstance<SearchProgress.Match>()
            assertThat(matches.map { it.item.name })
                .containsExactly("report-q1.pdf", "report-q2.pdf")
            // matchCount increments monotonically as results stream in.
            assertThat(matches.map { it.matchCount }).isEqualTo(listOf(1, 2))
            assertThat(progress.last()).isEqualTo(SearchProgress.Completed(matchCount = 2))
        }

    @Test
    fun `routes a content tree-uri root through the SAF walker`() =
        runTest {
            val treeUri = "content://com.android.externalstorage.documents/tree/primary%3ADownload"
            every { documentTreeWalker.walk(any(), any()) } returns
                flowOf(fileItem("doc.pdf"))

            val progress = repository.search(SearchQuery(text = "doc", rootPath = treeUri)).toList()

            verify(exactly = 1) { documentTreeWalker.walk(treeUri, any()) }
            assertThat(progress.filterIsInstance<SearchProgress.Match>().map { it.item.name })
                .containsExactly("doc.pdf")
            assertThat(progress.last()).isEqualTo(SearchProgress.Completed(matchCount = 1))
        }

    private fun fileItem(name: String): FileItem =
        FileItem(
            name = name,
            path = "uri/$name",
            isDirectory = false,
            sizeBytes = 1L,
            lastModifiedEpochMillis = 0L,
        )

    private fun file(
        parent: File,
        name: String,
    ): File = File(parent, name).apply { writeBytes(ByteArray(1)) }
}
