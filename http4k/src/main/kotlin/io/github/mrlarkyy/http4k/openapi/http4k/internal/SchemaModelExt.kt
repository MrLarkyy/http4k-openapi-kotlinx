package io.github.mrlarkyy.http4k.openapi.http4k.internal

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Prefix of every schema reference in an OpenAPI 3 document. */
internal const val SCHEMA_REF_PREFIX = "#/components/schemas/"

/** A reference to the schema called [name]. */
internal fun schemaRef(name: String): JsonObject = buildJsonObject { put("\$ref", SCHEMA_REF_PREFIX + name) }

/**
 * Renders this model as a JSON schema node. [refName] maps the model's definition names to the names used in the
 * document. An empty `required` list is left out, because OpenAPI 3.0 does not allow it.
 */
internal fun SchemaModel.toJson(refName: (String) -> String): JsonElement =
    when (this) {
        is SchemaModel.Primitive -> {
            buildJsonObject {
                put("type", type)
                format?.let { put("format", it) }
            }
        }

        is SchemaModel.Ref -> {
            schemaRef(refName(name))
        }

        is SchemaModel.Object -> {
            buildJsonObject {
                put("type", "object")
                put("properties", JsonObject(properties.mapValuesTo(LinkedHashMap()) { (_, property) -> property.toJson(refName) }))
                if (required.isNotEmpty()) {
                    put("required", buildJsonArray { required.forEach { add(JsonPrimitive(it)) } })
                }
            }
        }
    }
