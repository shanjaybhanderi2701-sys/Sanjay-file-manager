package com.appblish.filora.core.common.dispatcher

import javax.inject.Qualifier

/**
 * Coroutine dispatcher qualifiers. These are pure `javax.inject` annotations so
 * `core-common` stays Android/Hilt-free (load-bearing pure-JVM rule). The Hilt
 * provider that binds them lives in `:app` (`DispatcherModule`).
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class DefaultDispatcher

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class MainDispatcher

/** Enumerates the dispatchers Filora injects, for table-driven provider wiring. */
enum class FiloraDispatcher { Io, Default, Main }
