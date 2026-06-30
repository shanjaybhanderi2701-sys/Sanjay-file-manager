package com.appblish.filora.core.domain.model

/**
 * Pure directory ordering: folders-before-files default (FR-2.1, T041) plus sort by
 * name/size/date/type ascending or descending (FR-2.3, T040). Lives in core-domain so
 * the data layer and the browser ViewModel share a single ordering and it is
 * unit-testable without Android.
 *
 * When [SortOrder.foldersFirst] is set, directories are grouped ahead of files
 * regardless of the active key; within each group entries are compared by the chosen
 * [SortOrder.By] and the order is reversed for descending. Name and type comparisons are
 * case-insensitive; size/date ties fall back to case-insensitive name so the result is
 * stable rather than dependent on filesystem enumeration order.
 */
fun List<FileItem>.ordered(sortOrder: SortOrder): List<FileItem> {
    val byName = compareBy(String.CASE_INSENSITIVE_ORDER, FileItem::name)
    val key: Comparator<FileItem> =
        when (sortOrder.by) {
            SortOrder.By.Name -> byName
            SortOrder.By.Size -> compareBy(FileItem::sizeBytes).then(byName)
            SortOrder.By.DateModified -> compareBy(FileItem::lastModifiedEpochMillis).then(byName)
            SortOrder.By.Type -> compareBy(String.CASE_INSENSITIVE_ORDER, FileItem::extension).then(byName)
        }
    val directional = if (sortOrder.ascending) key else key.reversed()
    val comparator =
        if (sortOrder.foldersFirst) {
            compareByDescending(FileItem::isDirectory).then(directional)
        } else {
            directional
        }
    return sortedWith(comparator)
}
