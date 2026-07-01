package com.appblish.filora.core.testing

import com.appblish.filora.core.domain.model.FileItem

/** Restricts the builder receivers so a nested `dir { }` can't accidentally call an outer scope. */
@DslMarker
annotation class FileTreeDsl

/**
 * An in-memory file tree produced by [fileTree]. Backed by a flat, insertion-ordered
 * map keyed by absolute path so lookups (`item`), directory listings (`childrenOf`)
 * and recursive walks (`descendantsOf`) are all cheap and a [FakeFileRepository] can
 * mutate it directly. Symlink edges are tracked separately (the domain [FileItem] has
 * no symlink field) so tree-walk/loop-safety tests can opt into them.
 */
class FakeFileTree internal constructor(
    val root: String,
    private val items: LinkedHashMap<String, FileItem>,
    /** Map of a symlink's path to the path it points at, for walker loop-safety fixtures. */
    val symlinkTargets: Map<String, String>,
) {
    /** Every entry in the tree (directories and files), in declaration order. */
    val allItems: List<FileItem> get() = items.values.toList()

    /** Every non-directory entry, in declaration order. */
    val files: List<FileItem> get() = items.values.filter { !it.isDirectory }

    fun item(path: String): FileItem? = items[normalize(path)]

    /** Direct children of [path] (one level), in declaration order. */
    fun childrenOf(path: String): List<FileItem> {
        val parent = normalize(path)
        return items.values.filter { it.path != parent && parentPathOf(it.path) == parent }
    }

    /** All entries beneath [path] at any depth, in declaration order. */
    fun descendantsOf(path: String): List<FileItem> {
        val prefix = normalize(path).trimEnd('/') + "/"
        return items.values.filter { it.path.startsWith(prefix) }
    }

    internal fun snapshot(): LinkedHashMap<String, FileItem> = LinkedHashMap(items)

    private fun normalize(path: String): String = path.trimEnd('/').ifEmpty { "/" }

    companion object {
        internal fun parentPathOf(path: String): String =
            path.trimEnd('/').substringBeforeLast('/', missingDelimiterValue = "")
    }
}

/**
 * Builds a [FakeFileTree] with a small DSL:
 *
 * ```
 * val tree = fileTree(root = "/sdcard") {
 *     dir("Documents") {
 *         file("notes.txt", sizeBytes = 12)
 *         file(".secret", sizeBytes = 4, hidden = true)
 *     }
 *     file("photo.jpg", sizeBytes = 2_048, mimeType = "image/jpeg")
 *     symlink("loop", target = "/sdcard")
 * }
 * ```
 *
 * Directory `sizeBytes`/`childCount` are derived from their contents (recursive byte
 * total, immediate-child count) so storage and largest-file fixtures need no manual
 * bookkeeping.
 */
fun fileTree(
    root: String = "/storage/emulated/0",
    build: DirScope.() -> Unit,
): FakeFileTree {
    val rootPath = root.trimEnd('/').ifEmpty { "/" }
    val scope = DirScope(rootPath).apply(build)

    val items = LinkedHashMap<String, FileItem>()
    val symlinks = mutableMapOf<String, String>()
    // Register the root directory itself so listings/lookups of the top level work.
    items[rootPath] = FileItem(
        name = rootPath.substringAfterLast('/').ifEmpty { rootPath },
        path = rootPath,
        isDirectory = true,
        sizeBytes = scope.totalSize(),
        lastModifiedEpochMillis = 0L,
        childCount = scope.nodes.size,
        isHidden = false,
    )
    scope.flattenInto(items, symlinks)
    return FakeFileTree(rootPath, items, symlinks)
}

/** Builder receiver for a single directory level. */
@FileTreeDsl
class DirScope internal constructor(
    private val path: String
) {
    internal val nodes = mutableListOf<Node>()

    /** Adds a regular file with an explicit [sizeBytes]. */
    fun file(
        name: String,
        sizeBytes: Long = 0L,
        lastModifiedEpochMillis: Long = 0L,
        mimeType: String? = null,
        hidden: Boolean = false,
    ) {
        nodes += Node.File(
            FileItem(
                name = name,
                path = "$path/$name",
                isDirectory = false,
                sizeBytes = sizeBytes,
                lastModifiedEpochMillis = lastModifiedEpochMillis,
                mimeType = mimeType,
                extension = name.substringAfterLast('.', missingDelimiterValue = ""),
                isHidden = hidden || name.startsWith('.'),
            ),
        )
    }

    /** Adds a subdirectory; its contents are described by [build]. */
    fun dir(
        name: String,
        hidden: Boolean = false,
        build: DirScope.() -> Unit = {},
    ) {
        val child = DirScope("$path/$name").apply(build)
        nodes += Node.Dir(name, "$path/$name", hidden || name.startsWith('.'), child)
    }

    /**
     * Adds a symlink entry named [name] pointing at [target]. Modelled as a plain
     * [FileItem] (the domain model has no symlink flag) with the edge recorded on the
     * resulting [FakeFileTree] for walker loop-safety tests.
     */
    fun symlink(
        name: String,
        target: String,
        hidden: Boolean = false,
    ) {
        nodes += Node.Symlink(
            FileItem(
                name = name,
                path = "$path/$name",
                isDirectory = false,
                sizeBytes = 0L,
                lastModifiedEpochMillis = 0L,
                isHidden = hidden || name.startsWith('.'),
            ),
            target = target.trimEnd('/').ifEmpty { "/" },
        )
    }

    internal fun totalSize(): Long = nodes.sumOf { it.size() }

    internal fun flattenInto(
        items: LinkedHashMap<String, FileItem>,
        symlinks: MutableMap<String, String>,
    ) {
        nodes.forEach { node ->
            when (node) {
                is Node.File -> items[node.item.path] = node.item
                is Node.Symlink -> {
                    items[node.item.path] = node.item
                    symlinks[node.item.path] = node.target
                }
                is Node.Dir -> {
                    items[node.fullPath] = FileItem(
                        name = node.name,
                        path = node.fullPath,
                        isDirectory = true,
                        sizeBytes = node.scope.totalSize(),
                        lastModifiedEpochMillis = 0L,
                        childCount = node.scope.nodes.size,
                        isHidden = node.hidden,
                    )
                    node.scope.flattenInto(items, symlinks)
                }
            }
        }
    }

    internal sealed class Node {
        abstract fun size(): Long

        data class File(
            val item: FileItem
        ) : Node() {
            override fun size(): Long = item.sizeBytes
        }

        data class Symlink(
            val item: FileItem,
            val target: String
        ) : Node() {
            override fun size(): Long = 0L
        }

        data class Dir(
            val name: String,
            val fullPath: String,
            val hidden: Boolean,
            val scope: DirScope,
        ) : Node() {
            override fun size(): Long = scope.totalSize()
        }
    }
}
