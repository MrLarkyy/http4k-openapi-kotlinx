package io.github.mrlarkyy.http4k.openapi.http4k.internal

import kotlinx.serialization.json.JsonElement
import org.http4k.contract.jsonschema.JsonSchema
import java.lang.reflect.Method

/**
 * Reads [JsonSchema] through its getters by reflection: `getDefinitions` returns a `Set<Pair>` before http4k 6.25.1.0
 * and a `Map` from then on, so no compiled call links on both.
 */
internal object JsonSchemaAccess {
    private val getDefinitions: Method = JsonSchema::class.java.getMethod("getDefinitions")

    fun node(schema: JsonSchema<JsonElement>): JsonElement = schema.node

    /** The definitions of [schema] by name, in the order http4k will see them. */
    fun definitions(schema: JsonSchema<JsonElement>): Map<String, JsonElement> {
        val result = LinkedHashMap<String, JsonElement>()
        val value = getDefinitions.invoke(schema)
        if (value is Map<*, *>) {
            value.forEach { (name, node) -> result[name as String] = node as JsonElement }
        } else if (value is Set<*>) {
            value.forEach { pair ->
                val (name, node) = pair as Pair<*, *>
                result[name as String] = node as JsonElement
            }
        } else {
            error("Unsupported definitions type ${value?.javaClass}")
        }
        return result
    }

    /** The definition names in the order http4k will see them. */
    fun definitionOrder(schema: JsonSchema<JsonElement>): List<String> = definitions(schema).keys.toList()
}
