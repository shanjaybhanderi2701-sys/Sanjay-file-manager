package com.appblish.filora.core.domain.usecase

import com.appblish.filora.core.common.result.Result
import com.appblish.filora.core.domain.model.ConflictStrategy
import com.appblish.filora.core.domain.model.TransferOutcome
import com.appblish.filora.core.testing.FakeFileRepository
import com.appblish.filora.core.testing.fileTree
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Proves the shared `:core:core-testing` fixtures (T169) are consumable and
 * behaviourally correct by driving real domain use cases against the in-memory
 * [FakeFileRepository] built from the [fileTree] DSL. This is the acceptance proof
 * that the new module is wired into and used by the test source set.
 */
class FakeFileRepositoryFixtureTest {
    @Test
    fun fileTree_derivesDirectorySizeAndChildCount() {
        val tree = fileTree(root = "/sdcard") {
            dir("Documents") {
                file("notes.txt", sizeBytes = 12)
                file(".secret", sizeBytes = 4, hidden = true)
            }
            file("photo.jpg", sizeBytes = 2_048)
        }

        val documents = tree.item("/sdcard/Documents")!!
        assertThat(documents.isDirectory).isTrue()
        assertThat(documents.sizeBytes).isEqualTo(16) // recursive byte total
        assertThat(documents.childCount).isEqualTo(2)
        assertThat(tree.item("/sdcard/Documents/.secret")!!.isHidden).isTrue()
        assertThat(tree.childrenOf("/sdcard").map { it.name })
            .containsExactly("Documents", "photo.jpg")
    }

    @Test
    fun moveUseCase_overFakeRepo_copiesToDestinationThenDeletesSource() =
        runTest {
            val repo = FakeFileRepository(
                fileTree(root = "/sdcard") {
                    file("a.txt", sizeBytes = 10)
                    dir("dest")
                },
            )
            val source = repo.items.getValue("/sdcard/a.txt")
            val move = MoveUseCase(CopyUseCase(repo), repo)

            val result = move(listOf(source), "/sdcard/dest", ConflictStrategy.KeepBoth)

            val outcomes = (result as Result.Success).data
            assertThat(outcomes.single().outcome).isInstanceOf(TransferOutcome.Transferred::class.java)
            // Copy landed at the destination and the source was deleted (move safety).
            assertThat(repo.items).containsKey("/sdcard/dest/a.txt")
            assertThat(repo.items).doesNotContainKey("/sdcard/a.txt")
        }

    @Test
    fun copy_deepCopiesDirectorySubtreeAndReEmits() =
        runTest {
            val repo = FakeFileRepository(
                fileTree(root = "/sdcard") {
                    dir("src") {
                        file("inner.txt", sizeBytes = 3)
                    }
                    dir("dest")
                },
            )

            val copied = repo.copy(
                sourcePath = "/sdcard/src",
                destinationDir = "/sdcard/dest",
                destinationName = "src",
                overwrite = false,
            )

            assertThat(copied).isInstanceOf(Result.Success::class.java)
            assertThat(repo.items).containsKey("/sdcard/dest/src/inner.txt")
            // Source subtree is untouched (copy, not move) and listing re-emits the new child.
            assertThat(repo.items).containsKey("/sdcard/src/inner.txt")
            val destListing = repo.listDirectory("/sdcard/dest").first() as Result.Success
            assertThat(destListing.data.map { it.name }).containsExactly("src")
        }
}
