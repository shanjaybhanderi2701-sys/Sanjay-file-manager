package com.appblish.filora.core.data.media

import app.cash.turbine.test
import com.appblish.filora.core.common.result.OperationError
import com.appblish.filora.core.common.result.Result
import com.appblish.filora.core.domain.model.MediaCategory
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException

class MediaRepositoryImplTest {
    private val dispatcher = StandardTestDispatcher()

    private fun repository(source: MediaStoreSource) = MediaRepositoryImpl(source, dispatcher)

    private fun entry(
        name: String,
        mediaType: Int = MediaClassifier.MEDIA_TYPE_NONE,
        mimeType: String? = null,
        relativePath: String? = null,
    ) = RawMediaEntry(
        contentUri = "content://media/external/file/$name",
        displayName = name,
        mimeType = mimeType,
        mediaType = mediaType,
        sizeBytes = 100,
        dateModifiedEpochMillis = 1_700_000_000_000,
        relativePath = relativePath,
        filePath = "/storage/emulated/0/$name",
    )

    /** A source that buckets a fixed entry set through the real classifier. */
    private fun sourceOf(vararg entries: RawMediaEntry) =
        object : MediaStoreSource {
            override fun countByCategory(): Map<MediaCategory, Int> = entries.groupingBy { it.category }.eachCount()

            override fun sizeByCategory(): Map<MediaCategory, Long> =
                entries.groupingBy { it.category }.fold(0L) { acc, e -> acc + e.sizeBytes }

            override fun entriesIn(category: MediaCategory): List<RawMediaEntry> =
                entries.filter { it.category == category }
        }

    @Test
    fun `categoryCounts returns a dense map with zeros for empty categories`() =
        runTest(dispatcher) {
            val source =
                sourceOf(
                    entry("a.jpg", mediaType = MediaClassifier.MEDIA_TYPE_IMAGE),
                    entry("b.png", mediaType = MediaClassifier.MEDIA_TYPE_IMAGE),
                    entry("song.mp3", mediaType = MediaClassifier.MEDIA_TYPE_AUDIO),
                    entry("doc.pdf"),
                )

            val result = repository(source).categoryCounts()

            assertThat(result).isInstanceOf(Result.Success::class.java)
            val counts = (result as Result.Success).data
            // Every category present (dense), even those with no items.
            assertThat(counts.keys).containsExactlyElementsIn(MediaCategory.entries)
            assertThat(counts[MediaCategory.Images]).isEqualTo(2)
            assertThat(counts[MediaCategory.Audio]).isEqualTo(1)
            assertThat(counts[MediaCategory.Documents]).isEqualTo(1)
            assertThat(counts[MediaCategory.Video]).isEqualTo(0)
            assertThat(counts[MediaCategory.Apps]).isEqualTo(0)
        }

    @Test
    fun `categorySizes returns a dense map summing bytes per category`() =
        runTest(dispatcher) {
            // entry() fixes sizeBytes = 100 each.
            val source =
                sourceOf(
                    entry("a.jpg", mediaType = MediaClassifier.MEDIA_TYPE_IMAGE),
                    entry("b.png", mediaType = MediaClassifier.MEDIA_TYPE_IMAGE),
                    entry("song.mp3", mediaType = MediaClassifier.MEDIA_TYPE_AUDIO),
                )

            val result = repository(source).categorySizes()

            assertThat(result).isInstanceOf(Result.Success::class.java)
            val sizes = (result as Result.Success).data
            assertThat(sizes.keys).containsExactlyElementsIn(MediaCategory.entries)
            assertThat(sizes[MediaCategory.Images]).isEqualTo(200L)
            assertThat(sizes[MediaCategory.Audio]).isEqualTo(100L)
            assertThat(sizes[MediaCategory.Video]).isEqualTo(0L)
        }

    @Test
    fun `categorySizes surfaces source failures as an Io error`() =
        runTest(dispatcher) {
            val source =
                object : MediaStoreSource {
                    override fun countByCategory() = emptyMap<MediaCategory, Int>()

                    override fun sizeByCategory(): Map<MediaCategory, Long> = throw IOException("cursor blew up")

                    override fun entriesIn(category: MediaCategory) = emptyList<RawMediaEntry>()
                }

            val result = repository(source).categorySizes()

            assertThat(result).isInstanceOf(Result.Error::class.java)
            assertThat((result as Result.Error).error).isInstanceOf(OperationError.Io::class.java)
        }

    @Test
    fun `categoryCounts surfaces source failures as an Io error`() =
        runTest(dispatcher) {
            val source =
                object : MediaStoreSource {
                    override fun countByCategory(): Map<MediaCategory, Int> = throw IOException("cursor blew up")

                    override fun sizeByCategory(): Map<MediaCategory, Long> = throw IOException("cursor blew up")

                    override fun entriesIn(category: MediaCategory) = emptyList<RawMediaEntry>()
                }

            val result = repository(source).categoryCounts()

            assertThat(result).isInstanceOf(Result.Error::class.java)
            assertThat((result as Result.Error).error).isInstanceOf(OperationError.Io::class.java)
        }

    @Test
    fun `observeCategory emits the entries mapped to FileItems`() =
        runTest(dispatcher) {
            val source =
                sourceOf(
                    entry("a.jpg", mediaType = MediaClassifier.MEDIA_TYPE_IMAGE),
                    entry("clip.mp4", mediaType = MediaClassifier.MEDIA_TYPE_VIDEO),
                )

            repository(source).observeCategory(MediaCategory.Images).test {
                val result = awaitItem()
                assertThat(result).isInstanceOf(Result.Success::class.java)
                val items = (result as Result.Success).data
                assertThat(items).hasSize(1)
                val item = items.single()
                assertThat(item.name).isEqualTo("a.jpg")
                assertThat(item.path).isEqualTo("content://media/external/file/a.jpg")
                assertThat(item.isDirectory).isFalse()
                assertThat(item.extension).isEqualTo("jpg")
                awaitComplete()
            }
        }

    @Test
    fun `observeCategory surfaces source failures as an Io error`() =
        runTest(dispatcher) {
            val source =
                object : MediaStoreSource {
                    override fun countByCategory() = emptyMap<MediaCategory, Int>()

                    override fun sizeByCategory() = emptyMap<MediaCategory, Long>()

                    override fun entriesIn(category: MediaCategory): List<RawMediaEntry> =
                        throw IOException("query failed")
                }

            repository(source).observeCategory(MediaCategory.Video).test {
                val result = awaitItem()
                assertThat(result).isInstanceOf(Result.Error::class.java)
                assertThat((result as Result.Error).error).isInstanceOf(OperationError.Io::class.java)
                awaitComplete()
            }
        }
}
