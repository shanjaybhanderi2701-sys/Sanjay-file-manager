package com.appblish.filora.core.data.file

import com.appblish.filora.core.domain.model.FileItem
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import javax.inject.Inject

/**
 * java.io listing + metadata + file operations for local storage paths (T031, FR-2.1).
 * Pure java.io with no Android dependency so it is exercisable with temp directories in a
 * plain JVM test. Every method throws on failure ([FileNotFoundException],
 * [java.nio.file.FileAlreadyExistsException], [SecurityException], [IOException]); the
 * repository translates those into the domain error taxonomy.
 */
internal class FileSystemDataSource
    @Inject
    constructor() : FileSource {
        /** Lists [path]'s direct children, unsorted (the repository applies ordering). */
        override fun list(path: String): List<FileItem> {
            val dir = File(path)
            if (!dir.exists()) throw FileNotFoundException(path)
            if (!dir.isDirectory) throw IOException("Not a directory: $path")
            // listFiles() returns null on an I/O error or when the directory is unreadable.
            val children = dir.listFiles() ?: throw IOException("Cannot list: $path")
            return children.map(File::toFileItem)
        }

        override fun getFile(path: String): FileItem {
            val file = File(path)
            if (!file.exists()) throw FileNotFoundException(path)
            return file.toFileItem()
        }

        override fun createFolder(
            parentPath: String,
            name: String,
        ): FileItem {
            val target = File(parentPath, name)
            if (target.exists()) throw java.nio.file.FileAlreadyExistsException(target.path)
            if (!target.mkdirs()) throw IOException("Cannot create folder: ${target.path}")
            return target.toFileItem()
        }

        override fun rename(
            path: String,
            newName: String,
        ): FileItem {
            val source = File(path)
            if (!source.exists()) throw FileNotFoundException(path)
            val target = File(source.parentFile, newName)
            if (target.exists()) throw java.nio.file.FileAlreadyExistsException(target.path)
            if (!source.renameTo(target)) throw IOException("Cannot rename: $path")
            return target.toFileItem()
        }

        /** Removes each path, recursing into directories. Caller verified them up front. */
        override fun delete(paths: List<String>) {
            paths.forEach { path ->
                val file = File(path)
                if (file.exists() && !file.deleteRecursively()) {
                    throw IOException("Cannot delete: $path")
                }
            }
        }

        override fun copy(
            sourcePath: String,
            destinationDir: String,
            destinationName: String,
            overwrite: Boolean,
        ): FileItem {
            val source = File(sourcePath)
            if (!source.exists()) throw FileNotFoundException(sourcePath)
            val target = File(destinationDir, destinationName)
            if (target.exists() && !overwrite) throw java.nio.file.FileAlreadyExistsException(target.path)
            source.copyRecursively(target, overwrite = overwrite)
            return target.toFileItem()
        }
    }
