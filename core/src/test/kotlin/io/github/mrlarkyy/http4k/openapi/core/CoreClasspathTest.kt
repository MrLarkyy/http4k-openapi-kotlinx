package io.github.mrlarkyy.http4k.openapi.core

import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlin.test.Test
import kotlin.test.assertEquals

class CoreClasspathTest {
    @Test
    fun `kotlinx serialization descriptor api is on the classpath`() {
        assertEquals(PrimitiveKind.STRING, String.serializer().descriptor.kind)
    }
}
