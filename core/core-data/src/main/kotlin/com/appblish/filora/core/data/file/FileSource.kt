package com.appblish.filora.core.data.file

import com.appblish.filora.core.domain.model.FileItem

/**
 * The listing + operation surface shared by the java.io ([FileSystemDataSource]) and SAF
 * ([SafDataSource]) backends, so [FileRepositoryImpl] routes to either by scheme without
 * per-call branching. Implementations throw on failure ([java.io.FileNotFoundException],
 * [java.nio.file.FileAlreadyExistsException], [SecurityException], [java.io.IOException]);
 * the repository maps those onto the domain error taxonomy.
 */
internal interface FileSource {
    /** Direct children of [path], unsorted — the repository applies ordering. */
    fun list(path: String): List<FileItem>

    fun getFile(path: String): FileItem

    fun createFolder(
        parentPath: String,
        name: String,
    ): FileItem

    fun rename(
        path: String,
        newName: String,
    ): FileItem

    fun delete(paths: List<String>)

    fun copy(
        sourcePath: String,
        destinationDir: String,
        destinationName: String,
        overwrite: Boolean,
    ): FileItem
}
