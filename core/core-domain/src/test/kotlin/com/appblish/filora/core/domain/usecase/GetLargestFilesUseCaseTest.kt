package com.appblish.filora.core.domain.usecase

import com.appblish.filora.core.common.result.OperationError
import com.appblish.filora.core.common.result.Result
import com.appblish.filora.core.common.result.asError
import com.appblish.filora.core.common.result.asSuccess
import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.domain.model.StorageVolume
import com.appblish.filora.core.domain.repository.StorageRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException

class GetLargestFilesUseCaseTest {
    private fun volume(
        id: String,
        isPrimary: Boolean,
        rootPath: String = "/storage/$id",
    ) = StorageVolume(
        id = id,
        label = id,
        rootPath = rootPath,
        totalBytes = 1_000,
        availableBytes = 400,
        isRemovable = !isPrimary,
        isPrimary = isPrimary,
    )

    private fun file(
        name: String,
        size: Long,
    ) = FileItem(
        name = name,
        path = "/storage/internal/$name",
        isDirectory = false,
        sizeBytes = size,
        lastModifiedEpochMillis = 0,
    )

    private class FakeStorage(
        private val volumesFlow: Flow<List<StorageVolume>>,
        private val onLargest: (rootPath: String, limit: Int) -> Result<List<FileItem>> = { _, _ ->
            emptyList<FileItem>().asSuccess()
        },
    ) : StorageRepository {
        var requestedRoot: String? = null
        var requestedLimit: Int? = null

        override fun observeVolumes(): Flow<List<StorageVolume>> = volumesFlow

        override suspend fun getVolume(id: String): Result<StorageVolume> = throw UnsupportedOperationException()

        override suspend fun largestFiles(
            rootPath: String,
            limit: Int,
        ): Result<List<FileItem>> {
            requestedRoot = rootPath
            requestedLimit = limit
            return onLargest(rootPath, limit)
        }
    }

    @Test
    fun `scans the primary volume by default and drops zero-byte files`() =
        runTest {
            val storage =
                FakeStorage(
                    flowOf(listOf(volume("sdcard", isPrimary = false), volume("internal", isPrimary = true))),
                    onLargest = { _, _ ->
                        listOf(file("big.mp4", 900), file("empty.tmp", 0), file("doc.pdf", 100)).asSuccess()
                    },
                )

            val result = GetLargestFilesUseCase(storage)()

            assertThat(result).isInstanceOf(Result.Success::class.java)
            val files = (result as Result.Success).data
            assertThat(files.map { it.name }).containsExactly("big.mp4", "doc.pdf").inOrder()
            // Defaulted to the primary volume's root and the top-N limit.
            assertThat(storage.requestedRoot).isEqualTo("/storage/internal")
            assertThat(storage.requestedLimit).isEqualTo(GetLargestFilesUseCase.DEFAULT_LIMIT)
        }

    @Test
    fun `scans a named volume when a volumeId is supplied`() =
        runTest {
            val storage =
                FakeStorage(
                    flowOf(
                        listOf(
                            volume("internal", isPrimary = true, rootPath = "/storage/internal"),
                            volume("sdcard", isPrimary = false, rootPath = "/storage/sdcard"),
                        ),
                    ),
                )

            GetLargestFilesUseCase(storage)(volumeId = "sdcard", limit = 10)

            assertThat(storage.requestedRoot).isEqualTo("/storage/sdcard")
            assertThat(storage.requestedLimit).isEqualTo(10)
        }

    @Test
    fun `falls back to the first volume when there is no primary`() =
        runTest {
            val storage = FakeStorage(flowOf(listOf(volume("usb", isPrimary = false, rootPath = "/storage/usb"))))

            GetLargestFilesUseCase(storage)()

            assertThat(storage.requestedRoot).isEqualTo("/storage/usb")
        }

    @Test
    fun `returns NotFound when no volume is mounted`() =
        runTest {
            val storage = FakeStorage(flowOf(emptyList()))

            val result = GetLargestFilesUseCase(storage)()

            assertThat(result).isInstanceOf(Result.Error::class.java)
            assertThat((result as Result.Error).error).isInstanceOf(OperationError.NotFound::class.java)
            assertThat(storage.requestedRoot).isNull()
        }

    @Test
    fun `surfaces a volume enumeration failure as an Io error`() =
        runTest {
            val storage = FakeStorage(flow { throw IOException("storage manager blew up") })

            val result = GetLargestFilesUseCase(storage)()

            assertThat(result).isInstanceOf(Result.Error::class.java)
            assertThat((result as Result.Error).error).isInstanceOf(OperationError.Io::class.java)
        }

    @Test
    fun `propagates a scan failure from the repository`() =
        runTest {
            val storage =
                FakeStorage(
                    flowOf(listOf(volume("internal", isPrimary = true))),
                    onLargest = { _, _ -> OperationError.PermissionDenied().asError() },
                )

            val result = GetLargestFilesUseCase(storage)()

            assertThat(result).isInstanceOf(Result.Error::class.java)
            assertThat((result as Result.Error).error).isInstanceOf(OperationError.PermissionDenied::class.java)
        }
}
