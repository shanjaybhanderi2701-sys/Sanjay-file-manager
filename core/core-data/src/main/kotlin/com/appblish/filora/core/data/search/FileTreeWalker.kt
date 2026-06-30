package com.appblish.filora.core.data.search

import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.domain.model.SearchQuery
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import javax.inject.Inject

/**
 * Breadth-first java.io tree walk that streams matching [FileItem]s (FR-5.1).
 *
 * The flow is cold and cooperative: [ensureActive] runs before every directory listing
 * and child scan, so a collector that stops (newer query, screen left) halts the walk
 * promptly. A directory that cannot be listed — permission denied, removed mid-walk —
 * yields `null` from [File.listFiles] and is skipped rather than aborting the search, so
 * every readable subtree still streams (NFR-2.2 partial progress).
 *
 * **Symlink-loop bounded:** each directory is entered at most once keyed by its canonical
 * path, so a symlink pointing back at an ancestor resolves to an already-visited path and
 * is dropped. [SearchTraversal.MAX_DEPTH] is a second guard for degenerate depth.
 */
internal class FileTreeWalker
    @Inject
    constructor() {
        fun walk(
            root: File,
            query: SearchQuery,
        ): Flow<FileItem> =
            flow {
                val needle = query.text.trim()
                val visited = HashSet<String>()
                val pending = ArrayDeque<Descent>()
                enqueue(pending, visited, root, depth = 0)

                while (pending.isNotEmpty()) {
                    currentCoroutineContext().ensureActive()
                    val (dir, depth) = pending.removeFirst()
                    val children = dir.listFiles() ?: continue // unreadable subtree: skip

                    for (child in children) {
                        currentCoroutineContext().ensureActive()
                        val name = child.name
                        if (name.startsWith(SearchTraversal.HIDDEN_PREFIX) && !query.includeHidden) continue

                        val isDirectory = child.isDirectory
                        val item = child.toFileItem(isDirectory)
                        if (query.accepts(item, needle)) emit(item)

                        if (isDirectory && depth < SearchTraversal.MAX_DEPTH) {
                            enqueue(pending, visited, child, depth + 1)
                        }
                    }
                }
            }

        private fun enqueue(
            pending: ArrayDeque<Descent>,
            visited: MutableSet<String>,
            dir: File,
            depth: Int,
        ) {
            val canonical = runCatching { dir.canonicalPath }.getOrDefault(dir.absolutePath)
            if (visited.add(canonical)) pending.addLast(Descent(dir, depth))
        }

        private data class Descent(
            val dir: File,
            val depth: Int
        )
    }
