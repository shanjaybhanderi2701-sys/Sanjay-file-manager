package com.appblish.filora.core.data.media

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.os.Build
import android.provider.MediaStore
import com.appblish.filora.core.domain.model.MediaCategory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * [MediaStoreSource] backed by [MediaStore] over the external "files" collection.
 *
 * A single projection (id, name, MIME, media type, size, date, relative path) is
 * read once and every row is bucketed by [MediaClassifier], so counting and
 * category listing share one query path. Rows are addressed by content URI rather
 * than the deprecated `_data` column to stay correct under scoped storage.
 *
 * Requires read access to media (granted by the storage permission flow); without
 * it the query yields an empty cursor, which surfaces here as zero counts rather
 * than a crash.
 */
class AndroidMediaStoreSource
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : MediaStoreSource {
        private val contentResolver: ContentResolver get() = context.contentResolver

        override fun countByCategory(): Map<MediaCategory, Int> {
            val counts = mutableMapOf<MediaCategory, Int>()
            forEachEntry { entry -> counts.merge(entry.category, 1, Int::plus) }
            return counts
        }

        override fun sizeByCategory(): Map<MediaCategory, Long> {
            val sizes = mutableMapOf<MediaCategory, Long>()
            forEachEntry { entry -> sizes.merge(entry.category, entry.sizeBytes, Long::plus) }
            return sizes
        }

        override fun entriesIn(category: MediaCategory): List<RawMediaEntry> {
            val entries = mutableListOf<RawMediaEntry>()
            forEachEntry { entry -> if (entry.category == category) entries += entry }
            return entries
        }

        /**
         * Bridges the platform [ContentObserver] callback into a cold [Flow]. The
         * observer is registered for the whole external collection (with
         * descendants) on collection and torn down on cancellation via
         * [awaitClose], so a category screen leaves no observer behind when it
         * stops collecting. A null handler delivers callbacks on the resolver's
         * worker thread, which is fine since we only signal.
         */
        override fun changes(): Flow<Unit> =
            callbackFlow {
                val observer =
                    object : ContentObserver(null) {
                        override fun onChange(selfChange: Boolean) {
                            trySend(Unit)
                        }
                    }
                contentResolver.registerContentObserver(COLLECTION, true, observer)
                awaitClose { contentResolver.unregisterContentObserver(observer) }
            }

        private inline fun forEachEntry(action: (RawMediaEntry) -> Unit) {
            contentResolver
                .query(COLLECTION, PROJECTION, null, null, null)
                ?.use { cursor ->
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                    val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                    val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
                    val typeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
                    val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                    val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
                    val pathColumn = cursor.relativePathColumn()
                    val dataColumn = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)

                    while (cursor.moveToNext()) {
                        action(
                            cursor.toEntry(
                                idColumn,
                                nameColumn,
                                mimeColumn,
                                typeColumn,
                                sizeColumn,
                                dateColumn,
                                pathColumn,
                                dataColumn
                            )
                        )
                    }
                }
        }

        @Suppress("LongParameterList")
        private fun Cursor.toEntry(
            idColumn: Int,
            nameColumn: Int,
            mimeColumn: Int,
            typeColumn: Int,
            sizeColumn: Int,
            dateColumn: Int,
            pathColumn: Int,
            dataColumn: Int,
        ): RawMediaEntry {
            val id = getLong(idColumn)
            return RawMediaEntry(
                contentUri = ContentUris.withAppendedId(COLLECTION, id).toString(),
                displayName = getStringOrNull(nameColumn).orEmpty(),
                mimeType = getStringOrNull(mimeColumn),
                mediaType = if (isNull(typeColumn)) MediaClassifier.MEDIA_TYPE_NONE else getInt(typeColumn),
                sizeBytes = getLong(sizeColumn),
                // MediaStore reports DATE_MODIFIED in seconds; normalise to millis.
                dateModifiedEpochMillis = TimeUnit.SECONDS.toMillis(getLong(dateColumn)),
                relativePath = if (pathColumn >= 0) getStringOrNull(pathColumn) else null,
                filePath = if (dataColumn >= 0) getStringOrNull(dataColumn) else null,
            )
        }

        private fun Cursor.relativePathColumn(): Int =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                getColumnIndex(MediaStore.Files.FileColumns.RELATIVE_PATH)
            } else {
                -1
            }

        private fun Cursor.getStringOrNull(column: Int): String? = if (isNull(column)) null else getString(column)

        private companion object {
            val COLLECTION =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Files.getContentUri("external")
                }

            val PROJECTION =
                buildList {
                    add(MediaStore.Files.FileColumns._ID)
                    add(MediaStore.Files.FileColumns.DISPLAY_NAME)
                    add(MediaStore.Files.FileColumns.MIME_TYPE)
                    add(MediaStore.Files.FileColumns.MEDIA_TYPE)
                    add(MediaStore.Files.FileColumns.SIZE)
                    add(MediaStore.Files.FileColumns.DATE_MODIFIED)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        add(MediaStore.Files.FileColumns.RELATIVE_PATH)
                    }
                    @Suppress("DEPRECATION")
                    add(MediaStore.Files.FileColumns.DATA)
                }.toTypedArray()
        }
    }
