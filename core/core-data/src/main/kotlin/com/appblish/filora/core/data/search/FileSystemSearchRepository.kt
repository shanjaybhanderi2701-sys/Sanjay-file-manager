package com.appblish.filora.core.data.search

import com.appblish.filora.core.common.dispatcher.IoDispatcher
import com.appblish.filora.core.domain.model.FileItem
import com.appblish.filora.core.domain.model.SearchProgress
import com.appblish.filora.core.domain.model.SearchQuery
import com.appblish.filora.core.domain.repository.SearchRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import javax.inject.Inject

/**
 * [SearchRepository] over the device tree. Routes by root scheme — a `content://`
 * tree-document URI walks via SAF ([DocumentTreeWalker]); anything else is a java.io path
 * ([FileTreeWalker]) — then folds each raw match into a [SearchProgress.Match] carrying a
 * running count and closes with [SearchProgress.Completed].
 *
 * The whole walk runs on [ioDispatcher] via [flowOn] (FR-5.1 "on @IoDispatcher"): the
 * blocking `listFiles`/SAF I/O never touches the collector's thread. A blank query
 * short-circuits to a lone [SearchProgress.Completed] without listing anything, and the
 * underlying walkers' [kotlinx.coroutines.ensureActive] checks make abandonment cancel the
 * walk promptly.
 */
internal class FileSystemSearchRepository
    @Inject
    constructor(
        private val fileTreeWalker: FileTreeWalker,
        private val documentTreeWalker: DocumentTreeWalker,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : SearchRepository {
        override fun search(query: SearchQuery): Flow<SearchProgress> =
            flow {
                val root = query.rootPath
                if (query.isBlank || root.isNullOrBlank()) {
                    emit(SearchProgress.Completed(matchCount = 0))
                    return@flow
                }

                val matches: Flow<FileItem> =
                    if (root.startsWith(CONTENT_SCHEME)) {
                        documentTreeWalker.walk(root, query)
                    } else {
                        fileTreeWalker.walk(File(root), query)
                    }

                var count = 0
                matches.collect { item ->
                    count += 1
                    emit(SearchProgress.Match(item, matchCount = count))
                }
                emit(SearchProgress.Completed(matchCount = count))
            }.flowOn(ioDispatcher)

        private companion object {
            const val CONTENT_SCHEME = "content://"
        }
    }
