package com.appblish.filora.core.domain.repository

/**
 * Reports whether Filora currently holds runtime read access to shared media.
 *
 * Kept as a pure contract in the domain layer so feature ViewModels (Home, Media)
 * can render permission-aware empty states and decide whether to query MediaStore
 * without depending on the Android permission APIs — those live behind the
 * core-data implementation. The single source of truth for *which* permissions
 * constitute access is the app's `StoragePermissions`; the implementation mirrors
 * that predicate (M4 T4.6, FR-6.1 / FR-1.1).
 */
interface MediaAccess {
    /** True once the user has granted media read access (full or partial). */
    fun hasReadAccess(): Boolean
}
