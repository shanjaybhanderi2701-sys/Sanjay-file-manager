package com.appblish.filora.core.data.storage

import app.cash.turbine.test
import com.appblish.filora.core.common.result.OperationError
import com.appblish.filora.core.common.result.Result
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class StorageRepositoryImplTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private val dispatcher = StandardTestDispatcher()

    private fun repository(enumerator: VolumeEnumerator) = StorageRepositoryImpl(enumerator, dispatcher)

    private fun rawVolume(
        id: String,
        root: File,
        isPrimary: Boolean = true,
        isRemovable: Boolean = false,
    ) = RawVolume(
        id = id,
        label = "Volume $id",
        rootPath = root.absolutePath,
        isRemovable = isRemovable,
        isPrimary = isPrimary,
    )

    @Test
    fun `observeVolumes maps platform volumes and sizes them from the mount directory`() =
        runTest(dispatcher) {
            val dir = tempFolder.newFolder("internal")
            val enumerator = VolumeEnumerator { listOf(rawVolume("primary", dir)) }

            repository(enumerator).observeVolumes().test {
                val volumes = awaitItem()
                assertThat(volumes).hasSize(1)
                val volume = volumes.single()
                assertThat(volume.id).isEqualTo("primary")
                assertThat(volume.label).isEqualTo("Volume primary")
                assertThat(volume.rootPath).isEqualTo(dir.absolutePath)
                assertThat(volume.isPrimary).isTrue()
                assertThat(volume.isRemovable).isFalse()
                // Backed by a real filesystem, so totals are positive and consistent.
                assertThat(volume.totalBytes).isEqualTo(dir.totalSpace)
                assertThat(volume.availableBytes).isEqualTo(dir.usableSpace)
                assertThat(volume.usedBytes).isAtLeast(0L)
                assertThat(volume.usedBytes).isEqualTo(volume.totalBytes - volume.availableBytes)
                awaitComplete()
            }
        }

    @Test
    fun `getVolume returns the matching volume`() =
        runTest(dispatcher) {
            val dir = tempFolder.newFolder("sd")
            val enumerator =
                VolumeEnumerator {
                    listOf(
                        rawVolume("primary", tempFolder.newFolder("internal")),
                        rawVolume("ABCD-1234", dir, isPrimary = false, isRemovable = true),
                    )
                }

            val result = repository(enumerator).getVolume("ABCD-1234")

            assertThat(result).isInstanceOf(Result.Success::class.java)
            val volume = (result as Result.Success).data
            assertThat(volume.id).isEqualTo("ABCD-1234")
            assertThat(volume.isRemovable).isTrue()
        }

    @Test
    fun `getVolume returns NotFound for an unknown id`() =
        runTest(dispatcher) {
            val enumerator =
                VolumeEnumerator { listOf(rawVolume("primary", tempFolder.newFolder("internal"))) }

            val result = repository(enumerator).getVolume("missing")

            assertThat(result).isInstanceOf(Result.Error::class.java)
            assertThat((result as Result.Error).error).isInstanceOf(OperationError.NotFound::class.java)
        }

    @Test
    fun `getVolume surfaces enumeration failures as an Io error`() =
        runTest(dispatcher) {
            val enumerator = VolumeEnumerator { throw RuntimeException("mount table read failed") }

            val result = repository(enumerator).getVolume("primary")

            assertThat(result).isInstanceOf(Result.Error::class.java)
            assertThat((result as Result.Error).error).isInstanceOf(OperationError.Io::class.java)
        }

    @Test
    fun `largestFiles returns files sorted by size descending and capped at limit`() =
        runTest(dispatcher) {
            val root = tempFolder.newFolder("tree")
            writeFile(root, "small.txt", 10)
            writeFile(root, "big.bin", 5_000)
            val nested = File(root, "sub").apply { mkdirs() }
            writeFile(nested, "medium.dat", 1_000)

            val enumerator = VolumeEnumerator { emptyList() }
            val result = repository(enumerator).largestFiles(root.absolutePath, limit = 2)

            assertThat(result).isInstanceOf(Result.Success::class.java)
            val files = (result as Result.Success).data
            assertThat(files.map { it.name }).containsExactly("big.bin", "medium.dat").inOrder()
            assertThat(files.first().sizeBytes).isEqualTo(5_000L)
            assertThat(files.all { !it.isDirectory }).isTrue()
        }

    @Test
    fun `largestFiles returns empty for a non-positive limit`() =
        runTest(dispatcher) {
            val root = tempFolder.newFolder("tree")
            writeFile(root, "a.txt", 10)

            val result =
                repository(VolumeEnumerator { emptyList() }).largestFiles(root.absolutePath, limit = 0)

            assertThat(result).isInstanceOf(Result.Success::class.java)
            assertThat((result as Result.Success).data).isEmpty()
        }

    @Test
    fun `largestFiles returns NotFound when the root does not exist`() =
        runTest(dispatcher) {
            val missing = File(tempFolder.root, "does-not-exist")

            val result =
                repository(VolumeEnumerator { emptyList() }).largestFiles(missing.absolutePath)

            assertThat(result).isInstanceOf(Result.Error::class.java)
            assertThat((result as Result.Error).error).isInstanceOf(OperationError.NotFound::class.java)
        }

    private fun writeFile(
        parent: File,
        name: String,
        sizeBytes: Int,
    ) = File(parent, name).apply { writeBytes(ByteArray(sizeBytes)) }
}
