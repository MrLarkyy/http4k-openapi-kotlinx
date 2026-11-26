package io.github.mrlarkyy.http4k.openapi.http4k.internal

import kotlinx.serialization.json.JsonElement
import org.http4k.contract.jsonschema.JsonSchema
import java.lang.reflect.Constructor

/**
 * Creates http4k [JsonSchema] values.
 *
 * http4k changed the constructor of [JsonSchema] from `(NODE, Set<Pair>)` to `(NODE, Map)` in 6.25.1.0, so a compiled
 * call would link on only one side of the supported range. [resolve] picks the shape the loaded http4k has.
 */
internal fun interface JsonSchemaFactory {
    /** Creates a schema for [node] with [definitions] by name. */
    fun create(
        node: JsonElement,
        definitions: Map<String, JsonElement>,
    ): JsonSchema<JsonElement>

    companion object {
        /** Selects the factory that matches the constructor of the loaded [JsonSchema]. */
        fun resolve(): JsonSchemaFactory {
            val type = JsonSchema::class.java
            constructorOrNull(type, Map::class.java)?.let { return MapJsonSchemaFactory(it) }
            constructorOrNull(type, Set::class.java)?.let { return SetJsonSchemaFactory(it) }
            throw IllegalStateException(
                "Unsupported http4k: ${type.name} has no (Object, Map) or (Object, Set) constructor, found ${describe(type)}",
            )
        }

        private fun describe(type: Class<*>): String =
            type.constructors.joinToString { constructor ->
                constructor.parameterTypes.joinToString(prefix = "(", postfix = ")") { parameter -> parameter.name }
            }

        private fun constructorOrNull(
            type: Class<JsonSchema<*>>,
            second: Class<*>,
        ): Constructor<JsonSchema<*>>? =
            try {
                type.getConstructor(Any::class.java, second)
            } catch (_: NoSuchMethodException) {
                null
            }
    }
}

@Suppress("UNCHECKED_CAST")
private class MapJsonSchemaFactory(
    private val constructor: Constructor<JsonSchema<*>>,
) : JsonSchemaFactory {
    override fun create(
        node: JsonElement,
        definitions: Map<String, JsonElement>,
    ): JsonSchema<JsonElement> = constructor.newInstance(node, LinkedHashMap(definitions.toSortedMap())) as JsonSchema<JsonElement>
}

@Suppress("UNCHECKED_CAST")
private class SetJsonSchemaFactory(
    private val constructor: Constructor<JsonSchema<*>>,
) : JsonSchemaFactory {
    override fun create(
        node: JsonElement,
        definitions: Map<String, JsonElement>,
    ): JsonSchema<JsonElement> {
        val pairs = definitions.toSortedMap().mapTo(LinkedHashSet()) { (name, schema) -> name to schema }
        return constructor.newInstance(node, pairs) as JsonSchema<JsonElement>
    }
}
