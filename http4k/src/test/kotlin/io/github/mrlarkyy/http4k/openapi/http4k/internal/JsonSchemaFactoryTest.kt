package io.github.mrlarkyy.http4k.openapi.http4k.internal

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class JsonSchemaFactoryTest {
    private val factory = JsonSchemaFactory.resolve()

    @Test
    fun `the factory matches the loaded http4k and keeps node and definitions`() {
        val node = schemaRef("Item")
        val definitions: Map<String, JsonElement> =
            mapOf("Item" to JsonObject(mapOf("type" to JsonPrimitive("object"))), "Another" to JsonObject(emptyMap()))

        val schema = factory.create(node, definitions)

        assertEquals(node, JsonSchemaAccess.node(schema))
        assertEquals(definitions, JsonSchemaAccess.definitions(schema))
    }

    @Test
    fun `definitions are handed to http4k sorted by name`() {
        val definitions: Map<String, JsonElement> =
            linkedMapOf("b" to JsonObject(emptyMap()), "c" to JsonObject(emptyMap()), "a" to JsonObject(emptyMap()))

        val schema = factory.create(schemaRef("a"), definitions)

        assertEquals(listOf("a", "b", "c"), JsonSchemaAccess.definitionOrder(schema))
    }

    @Test
    fun `an empty definitions map is accepted`() {
        val schema = factory.create(JsonObject(emptyMap()), emptyMap())

        assertEquals(emptyMap(), JsonSchemaAccess.definitions(schema))
    }
}
