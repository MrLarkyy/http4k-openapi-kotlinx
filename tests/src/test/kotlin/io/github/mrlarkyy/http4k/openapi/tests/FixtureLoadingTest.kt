package io.github.mrlarkyy.http4k.openapi.tests

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class FixtureLoadingTest {
    @Test
    fun `classpath fixture loads and parses`() {
        val stream = assertNotNull(javaClass.getResourceAsStream("/fixtures/placeholder.json"), "fixture missing")
        val text = stream.use { it.readBytes().decodeToString() }

        val name =
            Json
                .parseToJsonElement(text)
                .jsonObject
                .getValue("name")
                .jsonPrimitive
                .content

        assertEquals("placeholder", name)
    }
}
