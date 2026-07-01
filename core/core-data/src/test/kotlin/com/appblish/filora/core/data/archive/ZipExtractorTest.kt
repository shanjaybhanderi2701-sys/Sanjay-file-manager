package com.appblish.filora.core.data.archive

import com.appblish.filora.core.common.result.OperationError
import com.appblish.filora.core.common.result.Result
import com.appblish.filora.core.domain.model.ConflictStrategy
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.coroutines.cancellation.CancellationException

class ZipExtractorTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val extractor = ZipExtractor()

    @Test
    fun `reconstructs nested directory tree`() {
        val archive = zip(
            "top.txt" to "root",
            "a/" to null,
            "a/b/c.txt" to "deep",
            "a/d.txt" to "shallow",
        )
        val dest = temp.newFolder("out")

        val result = extractor.extract(archive, dest)

        assertThat(result).isInstanceOf(Result.Success::class.java)
        assertThat(File(dest, "top.txt").readText()).isEqualTo("root")
        assertThat(File(dest, "a/b/c.txt").readText()).isEqualTo("deep")
        assertThat(File(dest, "a/d.txt").readText()).isEqualTo("shallow")
        val summary = (result as Result.Success).data
        assertThat(summary.extractedFiles).isEqualTo(3)
    }

    @Test
    fun `creates implicit parent directories for entries without directory records`() {
        // No explicit "x/" or "x/y/" entries — parents must be created on the fly.
        val archive = zip("x/y/z.txt" to "nested")
        val dest = temp.newFolder("out")

        extractor.extract(archive, dest)

        assertThat(File(dest, "x/y/z.txt").readText()).isEqualTo("nested")
    }

    @Test
    fun `rejects path-traversal entry without writing outside the destination`() {
        val archive = zip("../escaped.txt" to "evil")
        val dest = temp.newFolder("out")

        val result = extractor.extract(archive, dest)

        assertThat(result).isInstanceOf(Result.Error::class.java)
        assertThat((result as Result.Error).error).isInstanceOf(OperationError.Io::class.java)
        assertThat(File(dest.parentFile, "escaped.txt").exists()).isFalse()
    }

    @Test
    fun `rejects absolute path entry`() {
        val archive = zip("/etc/passwd" to "evil")
        val dest = temp.newFolder("out")

        // Leading slash is stripped to a relative path, so it stays inside the target.
        val result = extractor.extract(archive, dest)

        assertThat(result).isInstanceOf(Result.Success::class.java)
        assertThat(File(dest, "etc/passwd").readText()).isEqualTo("evil")
    }

    @Test
    fun `skip strategy keeps the existing file untouched`() {
        val dest = temp.newFolder("out")
        File(dest, "note.txt").writeText("original")
        val archive = zip("note.txt" to "incoming")

        val result = extractor.extract(archive, dest, ConflictStrategy.Skip)

        assertThat(File(dest, "note.txt").readText()).isEqualTo("original")
        assertThat((result as Result.Success).data.skippedFiles).isEqualTo(1)
        assertThat(result.data.extractedFiles).isEqualTo(0)
    }

    @Test
    fun `replace strategy overwrites the existing file`() {
        val dest = temp.newFolder("out")
        File(dest, "note.txt").writeText("original")
        val archive = zip("note.txt" to "incoming")

        val result = extractor.extract(archive, dest, ConflictStrategy.Replace)

        assertThat(File(dest, "note.txt").readText()).isEqualTo("incoming")
        assertThat((result as Result.Success).data.extractedFiles).isEqualTo(1)
    }

    @Test
    fun `keep-both strategy writes a uniquely named copy alongside the original`() {
        val dest = temp.newFolder("out")
        File(dest, "note.txt").writeText("original")
        val archive = zip("note.txt" to "incoming")

        val result = extractor.extract(archive, dest, ConflictStrategy.KeepBoth)

        assertThat(File(dest, "note.txt").readText()).isEqualTo("original")
        assertThat(File(dest, "note (1).txt").readText()).isEqualTo("incoming")
        assertThat((result as Result.Success).data.extractedFiles).isEqualTo(1)
    }

    @Test
    fun `keep-both handles a nested conflict within a reconstructed subdirectory`() {
        val dest = temp.newFolder("out")
        File(dest, "a").mkdirs()
        File(dest, "a/c.txt").writeText("original")
        val archive = zip("a/c.txt" to "incoming")

        extractor.extract(archive, dest, ConflictStrategy.KeepBoth)

        assertThat(File(dest, "a/c.txt").readText()).isEqualTo("original")
        assertThat(File(dest, "a/c (1).txt").readText()).isEqualTo("incoming")
    }

    @Test
    fun `cancellation aborts mid-archive`() {
        val archive = zip("a.txt" to "1", "b.txt" to "2", "c.txt" to "3")
        val dest = temp.newFolder("out")

        assertThrows(CancellationException::class.java) {
            extractor.extract(
                source = ByteArrayInputStream(archive.readBytes()),
                destinationDir = dest,
                isActive = { false },
            )
        }
    }

    @Test
    fun `counts created directories from explicit directory entries only`() {
        val archive = zip(
            "dir1/" to null,
            "dir2/" to null,
            "dir2/file.txt" to "x",
        )
        val dest = temp.newFolder("out")

        val summary = (extractor.extract(archive, dest) as Result.Success).data

        assertThat(summary.createdDirectories).isEqualTo(2)
        assertThat(summary.extractedFiles).isEqualTo(1)
    }

    @Test
    fun `rejects an entry over the per-entry uncompressed cap (zip-bomb guard)`() {
        // Compresses to a few bytes but inflates to 50 KB — the classic bomb shape.
        val archive = zip("bomb.txt" to "A".repeat(50_000))
        val dest = temp.newFolder("out")
        val guarded = ZipExtractor(
            ExtractionLimits(
                maxTotalUncompressedBytes = 1_000_000,
                maxEntryUncompressedBytes = 1_024,
                maxCompressionRatio = 10_000,
            ),
        )

        val result = guarded.extract(archive, dest)

        assertThat(result).isInstanceOf(Result.Error::class.java)
        assertThat((result as Result.Error).error).isInstanceOf(OperationError.Io::class.java)
    }

    @Test
    fun `rejects an archive over the total uncompressed cap`() {
        val chunk = "B".repeat(2_000)
        val archive = zip("a.txt" to chunk, "b.txt" to chunk, "c.txt" to chunk)
        val dest = temp.newFolder("out")
        val guarded = ZipExtractor(
            ExtractionLimits(
                maxTotalUncompressedBytes = 5_000, // 3 * 2_000 = 6_000 crosses this
                maxEntryUncompressedBytes = 1_000_000,
                maxCompressionRatio = 10_000,
            ),
        )

        val result = guarded.extract(archive, dest)

        assertThat(result).isInstanceOf(Result.Error::class.java)
        assertThat((result as Result.Error).error).isInstanceOf(OperationError.Io::class.java)
    }

    @Test
    fun `default limits extract an ordinary archive without tripping the guard`() {
        val archive = zip("readme.txt" to "hello", "d/note.txt" to "world")
        val dest = temp.newFolder("out")

        val result = ZipExtractor().extract(archive, dest)

        assertThat(result).isInstanceOf(Result.Success::class.java)
        assertThat(File(dest, "readme.txt").readText()).isEqualTo("hello")
        assertThat(File(dest, "d/note.txt").readText()).isEqualTo("world")
    }

    private fun zip(vararg entries: Pair<String, String?>): File {
        val file = temp.newFile("archive-${entries.hashCode()}.zip")
        ZipOutputStream(file.outputStream()).use { zos ->
            entries.forEach { writeEntry(zos, it) }
        }
        return file
    }

    private fun writeEntry(
        zos: ZipOutputStream,
        entry: Pair<String, String?>,
    ) {
        zos.putNextEntry(ZipEntry(entry.first))
        entry.second?.let { zos.write(it.toByteArray()) }
        zos.closeEntry()
    }
}
