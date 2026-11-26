package io.github.mrlarkyy.http4k.openapi.http4k.internal

import kotlinx.serialization.json.JsonElement
import org.http4k.contract.jsonschema.JsonSchema

internal fun JsonSchemaAccess.node(schema: JsonSchema<JsonElement>): JsonElement = schema.node

internal fun JsonSchemaAccess.definitionOrder(schema: JsonSchema<JsonElement>): List<String> = definitions(schema).keys.toList()
