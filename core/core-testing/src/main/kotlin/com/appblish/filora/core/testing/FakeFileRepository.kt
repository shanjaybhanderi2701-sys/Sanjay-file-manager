package com.appblish.filora.core.testing

import com.appblish.filora.core.common.result.OperationError
import com.appblish.filora.core.common.result.Result
import com.appblish.filora.core.common.result.asError
import com.appblish.filora.core.common.result.asSuccess
import com.appblish.filora.core.domain.model.DeleteOutcome
import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.domain.model.SortOrder
import com.appblish.filora.core.domain.repository.FileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * A behavioural in-memory [FileRepository] for unit/use-case/worker tests. Unlike a
 * scripted mock it actually mutates an in-memory [FakeFileTree], so create / rename /
 * delete / copy are observable through [listDirectory] (which re-emits on every
 * change) and a move (copy-verify-delete) leaves the tree in the right end state.
 *
 * Build one from the [fileTree] DSL:
 * ```
 * val repo = FakeFileRepository(fileTree("/sdcard") { file("a.txt", sizeBytes = 4) })
 * ```
 *
 * Failure paths can be injected per-operation to drive the error branches:
 * [failGetFilePaths] makes [getFile] return [OperationError.NotFound]; [failCopyPaths]
 * makes [copy] of a matching source return [OperationError.Io].
 */
class FakeFileRepository(
    tree: FakeFileTree = fileTree { },
    private val failGetFilePaths: Set<String> = emptySet(),
    private val failCopyPaths: Set<String> = emptySet(),
) : FileRepository {
    private val state = MutableStateFlow(tree.snapshot())

    /** Symlink edges declared in the source tree, exposed for walker loop-safety tests. */
    val symlinkTargets: Map<String, String> = tree.symlinkTargets

    /** Current items keyed by path — a live view for assertions after mutations. */
    val items: Map<String, FileItem> get() = state.value

    /** Records every [copy] call in order, for asserting resolved names / overwrite flags. */
    val copyCalls = mutableListOf<CopyCall>()

    override fun listDirectory(
        path: String,
        sortOrder: SortOrder,
    ): Flow<Result<List<FileItem>>> =
        // Declaration order is preserved; directory ordering itself is covered by the
        // domain's DirectoryOrdering tests, so the fake intentionally does not re-sort.
        state.map { snapshot -> childrenIn(snapshot, normalize(path)).asSuccess() }

    override suspend fun getFile(path: String): Result<FileItem> {
        val key = normalize(path)
        if (key in failGetFilePaths) return OperationError.NotFound(path).asError()
        return state.value[key]?.asSuccess() ?: OperationError.NotFound(path).asError()
    }

    override suspend fun createFolder(
        parentPath: String,
        name: String,
    ): Result<FileItem> {
        val parent = normalize(parentPath)
        val path = "$parent/$name"
        if (state.value.containsKey(path)) return OperationError.Conflict(path).asError()
        val folder = FileItem(
            name = name,
            path = path,
            isDirectory = true,
            sizeBytes = 0L,
            lastModifiedEpochMillis = 0L,
            childCount = 0,
        )
        mutate { it[path] = folder }
        return folder.asSuccess()
    }

    override suspend fun rename(
        path: String,
        newName: String,
    ): Result<FileItem> {
        val key = normalize(path)
        val existing = state.value[key] ?: return OperationError.NotFound(path).asError()
        val newPath = "${FakeFileTree.parentPathOf(key)}/$newName"
        if (state.value.containsKey(newPath)) return OperationError.Conflict(newPath).asError()
        val renamed = existing.copy(name = newName, path = newPath)
        mutate { snapshot ->
            // Re-key the entry and every descendant under the new prefix.
            val moved = reKey(snapshot, key, newPath)
            snapshot.clear()
            snapshot.putAll(moved)
            snapshot[newPath] = renamed
        }
        return renamed.asSuccess()
    }

    override suspend fun delete(
        paths: List<String>,
        toTrash: Boolean,
    ): Result<DeleteOutcome> {
        val targets = paths.map(::normalize)
        // All-or-nothing: every path must exist before we remove any (NFR-2.2).
        targets.firstOrNull { it !in state.value }?.let { return OperationError.NotFound(it).asError() }
        var removed = 0
        mutate { snapshot ->
            targets.forEach { target ->
                val gone = snapshot.keys.filter { it == target || it.startsWith("$target/") }
                gone.forEach { snapshot.remove(it) }
                if (gone.isNotEmpty()) removed++
            }
        }
        return DeleteOutcome(deletedCount = removed, movedToTrash = toTrash).asSuccess()
    }

    override suspend fun copy(
        sourcePath: String,
        destinationDir: String,
        destinationName: String,
        overwrite: Boolean,
    ): Result<FileItem> {
        val source = normalize(sourcePath)
        val dest = normalize(destinationDir)
        copyCalls += CopyCall(source, dest, destinationName, overwrite)
        if (source in failCopyPaths) return OperationError.Io().asError()
        val original = state.value[source] ?: return OperationError.NotFound(sourcePath).asError()
        val targetPath = "$dest/$destinationName"
        if (!overwrite && state.value.containsKey(targetPath)) return OperationError.Conflict(targetPath).asError()

        val created = original.copy(name = destinationName, path = targetPath)
        mutate { snapshot ->
            // Replace any existing subtree at the target when overwriting.
            snapshot.keys
                .filter { it == targetPath || it.startsWith("$targetPath/") }
                .toList()
                .forEach { snapshot.remove(it) }
            snapshot[targetPath] = created
            // Deep-copy descendants, re-rooting their paths under the new target (the
            // source subtree is left in place — this is a copy, not a move).
            snapshot.filterKeys { it.startsWith("$source/") }.forEach { (path, item) ->
                val newPath = targetPath + path.removePrefix(source)
                snapshot[newPath] = item.copy(path = newPath)
            }
        }
        return created.asSuccess()
    }

    private inline fun mutate(block: (LinkedHashMap<String, FileItem>) -> Unit) {
        val next = LinkedHashMap(state.value)
        block(next)
        state.value = next
    }

    /** Returns [snapshot] with [from] (and its descendants) re-pointed under [to]. */
    private fun reKey(
        snapshot: LinkedHashMap<String, FileItem>,
        from: String,
        to: String,
    ): LinkedHashMap<String, FileItem> {
        val result = LinkedHashMap<String, FileItem>()
        snapshot.forEach { (path, item) ->
            when {
                path == from -> result[to] = item.copy(path = to, name = to.substringAfterLast('/'))
                path.startsWith("$from/") -> {
                    val newPath = to + path.removePrefix(from)
                    result[newPath] = item.copy(path = newPath)
                }
                else -> result[path] = item
            }
        }
        return result
    }

    private fun childrenIn(
        snapshot: Map<String, FileItem>,
        parent: String,
    ): List<FileItem> = snapshot.values.filter { it.path != parent && FakeFileTree.parentPathOf(it.path) == parent }

    private fun normalize(path: String): String = path.trimEnd('/').ifEmpty { "/" }

    /** A single recorded [copy] invocation. */
    data class CopyCall(
        val sourcePath: String,
        val destinationDir: String,
        val destinationName: String,
        val overwrite: Boolean,
    )
}
