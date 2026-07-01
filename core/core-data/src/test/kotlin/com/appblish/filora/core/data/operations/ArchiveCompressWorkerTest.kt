package com.appblish.filora.core.data.operations

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
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
import java.util.zip.ZipFile

/**
 * JVM execution tests for [ArchiveCompressWorker] (T167). Exercises the worker's
 * orchestration layer — input decode, dispatch to [com.appblish.filora.core.data.archive.ZipCompressor],
 * progress emission, cancel cleanup and the WorkManager result — on Robolectric with
 * no emulator. The archiving logic itself (entry naming, walk) is covered exhaustively
 * by `ZipCompressorTest`; this only asserts the worker wires it correctly.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ArchiveCompressWorkerTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val context: Context get() = RuntimeEnvironment.getApplication()

    private lateinit var source: File
    private lateinit var destination: File

    @Before
    fun setUp() {
        source = temp.newFolder("src")
        destination = File(temp.newFolder("out"), "archive.zip")
    }

    private fun buildWorker(
        sources: List<File>,
        dest: File = destination,
        progressUpdater: RecordingProgressUpdater = RecordingProgressUpdater(),
    ): ArchiveCompressWorker {
        val input = ArchiveCompressWorkData.encodeInput(
            ArchiveCompressArgs(sources.map(File::getPath), dest.path),
        )
        return TestListenableWorkerBuilder
            .from(context, ArchiveCompressWorker::class.java)
            .setInputData(input)
            .setProgressUpdater(progressUpdater)
            .build()
    }

    @Test
    fun `compresses the sources into a zip and succeeds`() {
        val files = (1..3).map { index ->
            File(source, "file$index.txt").apply { writeText("payload-$index") }
        }
        val worker = buildWorker(files)

        val result = runBlocking { worker.doWork() }

        assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
        assertThat(destination.exists()).isTrue()
        ZipFile(destination).use { zip -> assertThat(zip.size()).isEqualTo(3) }
    }

    @Test
    fun `publishes determinate progress with the total entry count`() {
        val files = (1..3).map { index ->
            File(source, "file$index.txt").apply { writeText("payload-$index") }
        }
        val recorder = RecordingProgressUpdater()
        val worker = buildWorker(files, progressUpdater = recorder)

        runBlocking { worker.doWork() }

        assertThat(recorder.updates).isNotEmpty()
        val progress = ArchiveCompressWorkData.decodeProgress(recorder.updates.last())
        assertThat(progress.totalEntries).isEqualTo(3)
        assertThat(progress.currentName).isNotEmpty()
    }

    @Test
    fun `cancel deletes the partial archive and propagates cancellation`() {
        val files = (1..3).map { index ->
            File(source, "file$index.txt").apply { writeText("payload-$index") }
        }
        val worker = buildWorker(files).apply { stoppedSignalOverride = { true } }

        assertThrows(CancellationException::class.java) {
            runBlocking { worker.doWork() }
        }
        assertThat(destination.exists()).isFalse()
    }

    @Test
    fun `missing input fails the worker`() {
        val worker = buildWorker(sources = emptyList())

        val result = runBlocking { worker.doWork() }

        assertThat(result).isInstanceOf(ListenableWorker.Result.Failure::class.java)
    }
}
