package com.appblish.filora.core.data.search

import android.content.Context
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.appblish.filora.core.common.util.FileExtensions
import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.domain.model.SearchQuery
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Breadth-first SAF tree walk that streams matching [FileItem]s (FR-5.1) for a root that
 * is a persisted tree-document URI rather than a filesystem path.
 *
 * Mirrors [FileTreeWalker]: cold, cancelable ([ensureActive] each descent), hidden-skip,
 * AND-combined filters via [SearchQuery.accepts]. The SAF tree is a DAG with no symlinks,
 * so the only loop guard needed is [SearchTraversal.MAX_DEPTH]. A child with no resolvable
 * name is skipped. The traversal walks lazily through [DocumentFile.listFiles] per node so
 * it never materializes the whole tree.
 */
internal class DocumentTreeWalker
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        fun walk(
            treeUri: String,
            query: SearchQuery,
        ): Flow<FileItem> =
            flow {
                val root = DocumentFile.fromTreeUri(context, treeUri.toUri()) ?: return@flow
                val needle = query.text.trim()
                val pending = ArrayDeque<Descent>()
                pending.addLast(Descent(root, depth = 0))

                while (pending.isNotEmpty()) {
                    currentCoroutineContext().ensureActive()
                    val (dir, depth) = pending.removeFirst()

                    for (child in dir.listFiles()) {
                        currentCoroutineContext().ensureActive()
                        val name = child.name
                        // A nameless or (unless requested) hidden entry is the single skip case.
                        if (name == null || (name.startsWith(SearchTraversal.HIDDEN_PREFIX) && !query.includeHidden)) {
                            continue
                        }

                        val isDirectory = child.isDirectory
                        val item = child.toFileItem(name, isDirectory)
                        if (query.accepts(item, needle)) emit(item)

                        if (isDirectory && depth < SearchTraversal.MAX_DEPTH) {
                            pending.addLast(Descent(child, depth + 1))
                        }
                    }
                }
            }

        private fun DocumentFile.toFileItem(
            name: String,
            isDirectory: Boolean,
        ): FileItem =
            FileItem(
                name = name,
                path = uri.toString(),
                isDirectory = isDirectory,
                sizeBytes = if (isDirectory) 0L else length(),
                lastModifiedEpochMillis = lastModified(),
                extension = FileExtensions.extensionOf(name),
                isHidden = name.startsWith(SearchTraversal.HIDDEN_PREFIX),
            )

        private data class Descent(
            val dir: DocumentFile,
            val depth: Int
        )
    }
