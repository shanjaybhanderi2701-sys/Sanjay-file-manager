package com.appblish.filora.core.data.operations

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.appblish.filora.core.domain.model.ConflictStrategy
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * JVM execution tests for [ArchiveExtractWorker] (T167). Covers the worker's
 * orchestration — input decode, dispatch to
 * [com.appblish.filora.core.data.archive.ZipExtractor], conflict-strategy pass-through,
 * cancel and the WorkManager result — on Robolectric with no emulator. Zip-slip and
 * nested-path reconstruction are covered exhaustively by `ZipExtractorTest`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ArchiveExtractWorkerTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val context: Context get() = RuntimeEnvironment.getApplication()

    private lateinit var archive: File
    private lateinit var destination: File

    @Before
    fun setUp() {
        archive = File(temp.newFolder("archive"), "bundle.zip")
        destination = temp.newFolder("dest")
    }

    private fun writeZip(entries: List<Pair<String, String>>) {
        ZipOutputStream(archive.outputStream().buffered()).use { zip ->
            entries.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray())
                zip.closeEntry()
            }
        }
    }

    private fun buildWorker(strategy: ConflictStrategy): ArchiveExtractWorker {
        val input = ArchiveExtractWorkData.encodeInput(
            ArchiveExtractArgs(archive.path, destination.path, strategy),
        )
        return TestListenableWorkerBuilder
            .from(context, ArchiveExtractWorker::class.java)
            .setInputData(input)
            .build()
    }

    @Test
    fun `extracts nested entries and succeeds`() {
        writeZip(listOf("a.txt" to "AAA", "sub/b.txt" to "BBB"))
        val worker = buildWorker(ConflictStrategy.KeepBoth)

        val result = runBlocking { worker.doWork() }

        assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
        assertThat(File(destination, "a.txt").readText()).isEqualTo("AAA")
        assertThat(File(destination, "sub/b.txt").readText()).isEqualTo("BBB")
    }

    @Test
    fun `skip strategy leaves the existing file untouched`() {
        File(destination, "a.txt").writeText("OLD")
        writeZip(listOf("a.txt" to "NEW"))
        val worker = buildWorker(ConflictStrategy.Skip)

        val result = runBlocking { worker.doWork() }

        assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
        assertThat(File(destination, "a.txt").readText()).isEqualTo("OLD")
    }

    @Test
    fun `replace strategy overwrites the existing file`() {
        File(destination, "a.txt").writeText("OLD")
        writeZip(listOf("a.txt" to "NEW"))
        val worker = buildWorker(ConflictStrategy.Replace)

        runBlocking { worker.doWork() }

        assertThat(File(destination, "a.txt").readText()).isEqualTo("NEW")
    }

    @Test
    fun `keep-both strategy writes the entry under a fresh name`() {
        File(destination, "a.txt").writeText("OLD")
        writeZip(listOf("a.txt" to "NEW"))
        val worker = buildWorker(ConflictStrategy.KeepBoth)

        runBlocking { worker.doWork() }

        assertThat(File(destination, "a.txt").readText()).isEqualTo("OLD")
        assertThat(File(destination, "a (1).txt").readText()).isEqualTo("NEW")
    }

    @Test
    fun `cancel aborts extraction and propagates cancellation`() {
        writeZip(listOf("a.txt" to "AAA", "b.txt" to "BBB"))
        val worker = buildWorker(ConflictStrategy.KeepBoth).apply { stoppedSignalOverride = { true } }

        assertThrows(CancellationException::class.java) {
            runBlocking { worker.doWork() }
        }
    }

    @Test
    fun `missing archive fails the worker`() {
        // archive file was never written
        val worker = buildWorker(ConflictStrategy.KeepBoth)

        val result = runBlocking { worker.doWork() }

        assertThat(result).isInstanceOf(ListenableWorker.Result.Failure::class.java)
    }
}
