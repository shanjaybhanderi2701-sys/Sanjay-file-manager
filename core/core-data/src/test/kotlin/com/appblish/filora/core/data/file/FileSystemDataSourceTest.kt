package com.appblish.filora.core.data.file

import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileNotFoundException

class FileSystemDataSourceTest {
    @get:Rule val temp = TemporaryFolder()

    private val source = FileSystemDataSource()

    @Test
    fun `list returns direct children including hidden, with metadata`() {
        val root = temp.newFolder("root")
        File(root, "a.txt").writeText("hello")
        File(root, ".hidden").writeText("x")
        File(root, "sub").mkdir()

        val items = source.list(root.absolutePath)

        assertThat(items.map { it.name }).containsExactly("a.txt", ".hidden", "sub")
        val hidden = items.single { it.name == ".hidden" }
        assertThat(hidden.isHidden).isTrue()
        val file = items.single { it.name == "a.txt" }
        assertThat(file.isDirectory).isFalse()
        assertThat(file.sizeBytes).isEqualTo(5)
        assertThat(file.extension).isEqualTo("txt")
        assertThat(items.single { it.name == "sub" }.isDirectory).isTrue()
    }

    @Test(expected = FileNotFoundException::class)
    fun `list of a missing path throws not found`() {
        source.list(File(temp.root, "nope").absolutePath)
    }

    @Test
    fun `createFolder creates and reports the directory`() {
        val root = temp.newFolder("root")

        val created = source.createFolder(root.absolutePath, "new")

        assertThat(created.isDirectory).isTrue()
        assertThat(File(root, "new").isDirectory).isTrue()
    }

    @Test(expected = java.nio.file.FileAlreadyExistsException::class)
    fun `createFolder rejects an existing name`() {
        val root = temp.newFolder("root")
        File(root, "dup").mkdir()
        source.createFolder(root.absolutePath, "dup")
    }

    @Test
    fun `rename moves the entry to the new name`() {
        val root = temp.newFolder("root")
        val original = File(root, "old.txt").apply { writeText("hi") }

        val renamed = source.rename(original.absolutePath, "new.txt")

        assertThat(renamed.name).isEqualTo("new.txt")
        assertThat(original.exists()).isFalse()
        assertThat(File(root, "new.txt").exists()).isTrue()
    }

    @Test
    fun `delete removes files and recurses into folders`() {
        val root = temp.newFolder("root")
        val file = File(root, "f.txt").apply { writeText("x") }
        val dir = File(root, "d").apply { mkdir() }
        File(dir, "nested.txt").writeText("y")

        source.delete(listOf(file.absolutePath, dir.absolutePath))

        assertThat(file.exists()).isFalse()
        assertThat(dir.exists()).isFalse()
    }

    @Test
    fun `copy duplicates a file into the destination`() {
        val root = temp.newFolder("root")
        val src = File(root, "src.txt").apply { writeText("payload") }
        val dest = temp.newFolder("dest")

        val copied = source.copy(src.absolutePath, dest.absolutePath, "copy.txt", overwrite = false)

        assertThat(copied.name).isEqualTo("copy.txt")
        assertThat(File(dest, "copy.txt").readText()).isEqualTo("payload")
        assertThat(src.exists()).isTrue() // copy, not move
    }

    @Test(expected = java.nio.file.FileAlreadyExistsException::class)
    fun `copy without overwrite rejects an existing destination`() {
        val root = temp.newFolder("root")
        File(root, "src.txt").writeText("a")
        val dest = temp.newFolder("dest")
        File(dest, "src.txt").writeText("b")

        source.copy(File(root, "src.txt").absolutePath, dest.absolutePath, "src.txt", overwrite = false)
    }
}
