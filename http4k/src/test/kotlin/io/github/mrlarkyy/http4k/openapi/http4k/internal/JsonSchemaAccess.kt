package io.github.mrlarkyy.http4k.openapi.http4k.internal

import kotlinx.serialization.json.JsonElement
import org.http4k.contract.jsonschema.JsonSchema

/**
 * Reads [JsonSchema] through its getters by reflection: `getDefinitions` returns a `Set<Pair>` before http4k 6.25.1.0
 * and a `Map` from then on, so no compiled call links on both.
 */
internal object JsonSchemaAccess {
    fun node(schema: JsonSchema<JsonElement>): JsonElement = schema.node

    fun definitions(schema: JsonSchema<JsonElement>): Map<String, JsonElement> {
        val value = JsonSchema::class.java.getMethod("getDefinitions").invoke(schema)
        return when (value) {
            is Map<*, *> -> value.entries.associate { (name, node) -> name as String to node as JsonElement }
            is Set<*> -> value.associate { pair -> (pair as Pair<*, *>).let { it.first as String to it.second as JsonElement } }
            else -> error("Unsupported definitions type ${value?.javaClass}")
        }
    }

    /** The definition names in the order http4k will see them. */
    fun definitionOrder(schema: JsonSchema<JsonElement>): List<String> {
        val value = JsonSchema::class.java.getMethod("getDefinitions").invoke(schema)
        return when (value) {
            is Map<*, *> -> value.keys.map { it as String }
            is Set<*> -> value.map { (it as Pair<*, *>).first as String }
            else -> error("Unsupported definitions type ${value?.javaClass}")
        }
    }
}
