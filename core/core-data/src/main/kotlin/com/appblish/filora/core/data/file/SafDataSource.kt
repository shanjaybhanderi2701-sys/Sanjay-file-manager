package com.appblish.filora.core.data.file

import android.content.Context
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.appblish.filora.core.domain.model.FileItem
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.FileNotFoundException
import java.io.IOException
import javax.inject.Inject

/**
 * Storage Access Framework listing + operations for tree-document URIs (T032, FR-2.1) —
 * the path the user reaches through the SAF "open tree" picker on volumes the app can't
 * touch with java.io. A document URI from a granted tree is itself a tree-based document
 * URI, so [DocumentFile.fromTreeUri] re-roots at any folder the browser navigates into.
 * Every `path` argument here is therefore a tree-document URI string.
 */
internal class SafDataSource
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : FileSource {
        override fun list(path: String): List<FileItem> = resolve(path).listFiles().mapNotNull(DocumentFile::toFileItem)

        override fun getFile(path: String): FileItem = resolve(path).toFileItem() ?: throw FileNotFoundException(path)

        override fun createFolder(
            parentPath: String,
            name: String,
        ): FileItem {
            val parent = resolve(parentPath)
            if (parent.findFile(name) != null) throw java.nio.file.FileAlreadyExistsException(name)
            val created = parent.createDirectory(name) ?: throw IOException("Cannot create folder: $name")
            return created.toFileItem() ?: throw IOException("Cannot read created folder: $name")
        }

        override fun rename(
            path: String,
            newName: String,
        ): FileItem {
            val doc = resolve(path)
            if (!doc.renameTo(newName)) throw IOException("Cannot rename: $path")
            return doc.toFileItem() ?: throw IOException("Cannot read renamed entry: $path")
        }

        override fun delete(paths: List<String>) {
            paths.forEach { uri ->
                if (!resolve(uri).delete()) throw IOException("Cannot delete: $uri")
            }
        }

        override fun copy(
            sourcePath: String,
            destinationDir: String,
            destinationName: String,
            overwrite: Boolean,
        ): FileItem {
            val source = resolve(sourcePath)
            if (source.isDirectory) throw IOException("SAF directory copy is not supported yet: $sourcePath")
            val destDir = resolve(destinationDir)
            destDir.findFile(destinationName)?.let { existing ->
                if (!overwrite) throw java.nio.file.FileAlreadyExistsException(destinationName)
                existing.delete()
            }
            val mime = source.type ?: OCTET_STREAM
            val created =
                destDir.createFile(mime, destinationName)
                    ?: throw IOException("Cannot create: $destinationName")
            val resolver = context.contentResolver
            resolver.openInputStream(source.uri).use { input ->
                resolver.openOutputStream(created.uri).use { output ->
                    if (input == null || output == null) throw IOException("Cannot copy: $sourcePath")
                    input.copyTo(output)
                }
            }
            return created.toFileItem() ?: throw IOException("Cannot read copied entry: $destinationName")
        }

        private fun resolve(uri: String): DocumentFile =
            DocumentFile.fromTreeUri(context, uri.toUri()) ?: throw FileNotFoundException(uri)

        private companion object {
            const val OCTET_STREAM = "application/octet-stream"
        }
    }
