package com.appblish.filora.core.common.dispatcher

import com.google.common.truth.Truth.assertThat
import javax.inject.Qualifier
import org.junit.Test

class DispatchersTest {
    @Test
    fun `dispatcher annotations are runtime-retained injection qualifiers`() {
        listOf(
            IoDispatcher::class.java,
            DefaultDispatcher::class.java,
            MainDispatcher::class.java,
        ).forEach { annotation ->
            assertThat(annotation.isAnnotationPresent(Qualifier::class.java)).isTrue()
            val retention = annotation.getAnnotation(Retention::class.java)
            assertThat(retention.value).isEqualTo(AnnotationRetention.RUNTIME)
        }
    }

    @Test
    fun `FiloraDispatcher enumerates the three injected dispatchers`() {
        assertThat(FiloraDispatcher.entries)
            .containsExactly(
                FiloraDispatcher.Io,
                FiloraDispatcher.Default,
                FiloraDispatcher.Main,
            )
            .inOrder()
    }
}
