package io.github.mrlarkyy.http4k.openapi.tests.golden

import io.github.mrlarkyy.http4k.openapi.http4k.KotlinxApiRenderer
import org.http4k.format.KotlinxSerialization
import kotlin.test.Test
import kotlin.test.assertEquals

class SchemaDedupTest {
    private val itemKey = Regex("\"Item\"\\s*:")

    @Test
    fun `a type used by several routes is defined once as openapi 3_0`() {
        assertSingleDefinition(OpenApiSpec.V3_0)
    }

    @Test
    fun `a type used by several routes is defined once as openapi 3_1`() {
        assertSingleDefinition(OpenApiSpec.V3_1)
    }

    // The raw text is counted because parsing the document collapses duplicate keys.
    private fun assertSingleDefinition(spec: OpenApiSpec) {
        val body = ContractFixtures.render(spec, SerializableContract.serializableRoutes(), KotlinxApiRenderer(KotlinxSerialization))

        assertEquals(1, itemKey.findAll(body).count(), "definitions of Item in $spec")
    }
}
