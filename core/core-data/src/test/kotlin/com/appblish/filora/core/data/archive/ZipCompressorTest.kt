package com.appblish.filora.core.data.archive

import com.appblish.filora.core.common.result.OperationError
import com.appblish.filora.core.common.result.Result
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.coroutines.cancellation.CancellationException

class ZipCompressorTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val compressor = ZipCompressor()
    private val extractor = ZipExtractor()

    @Test
    fun `compresses a single file into an archive at the chosen destination`() {
        val source = temp.newFile("note.txt").apply { writeText("hello") }
        val dest = File(temp.root, "out.zip")

        val result = compressor.compress(listOf(source), dest)

        assertThat(result).isInstanceOf(Result.Success::class.java)
        assertThat(dest.exists()).isTrue()
        assertThat((result as Result.Success).data.entryCount).isEqualTo(1)
    }

    @Test
    fun `round-trips a nested directory through extract`() {
        val srcDir = temp.newFolder("docs")
        File(srcDir, "top.txt").writeText("root")
        File(srcDir, "a/b").mkdirs()
        File(srcDir, "a/b/c.txt").writeText("deep")
        val dest = File(temp.root, "docs.zip")

        compressor.compress(listOf(srcDir), dest)
        val out = temp.newFolder("restored")
        extractor.extract(dest, out)

        assertThat(File(out, "docs/top.txt").readText()).isEqualTo("root")
        assertThat(File(out, "docs/a/b/c.txt").readText()).isEqualTo("deep")
    }

    @Test
    fun `reports progress once per file entry`() {
        val dir = temp.newFolder("set")
        File(dir, "1.txt").writeText("a")
        File(dir, "2.txt").writeText("b")
        File(dir, "3.txt").writeText("c")
        val dest = File(temp.root, "set.zip")
        val totals = mutableListOf<Int>()
        var ticks = 0

        compressor.compress(listOf(dir), dest, onProgress = { _, total, _ ->
            ticks++
            totals += total
        })

        assertThat(ticks).isEqualTo(3)
        assertThat(totals.all { it == 3 }).isTrue()
    }

    @Test
    fun `cancellation deletes the partial output`() {
        val dir = temp.newFolder("big")
        repeat(5) { File(dir, "f$it.txt").writeText("x".repeat(64)) }
        val dest = File(temp.root, "big.zip")

        assertThrows(CancellationException::class.java) {
            compressor.compress(
                sources = listOf(dir),
                destination = dest,
                isActive = { false }, // stopped before the first entry
            )
        }

        assertThat(dest.exists()).isFalse()
    }

    @Test
    fun `missing source yields NotFound`() {
        val dest = File(temp.root, "x.zip")

        val result = compressor.compress(listOf(File(temp.root, "ghost.txt")), dest)

        assertThat(result).isInstanceOf(Result.Error::class.java)
        assertThat((result as Result.Error).error).isInstanceOf(OperationError.NotFound::class.java)
        assertThat(dest.exists()).isFalse()
    }

    @Test
    fun `empty source list is rejected`() {
        val result = compressor.compress(emptyList(), File(temp.root, "x.zip"))

        assertThat(result).isInstanceOf(Result.Error::class.java)
        assertThat((result as Result.Error).error).isInstanceOf(OperationError.InvalidName::class.java)
    }
}
