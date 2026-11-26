package io.github.mrlarkyy.http4k.openapi.tests.golden

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Canonical text form of a JSON document: object keys sorted recursively, array order kept, two-space indent, LF line
 * endings and a trailing newline. Leaves are printed through [JsonPrimitive.toString], which is identical across the
 * supported kotlinx.serialization versions.
 */
object CanonicalJson {
    fun canonical(
        text: String,
        source: String = "document",
    ): String {
        val element =
            try {
                Json.parseToJsonElement(text)
            } catch (e: SerializationException) {
                throw IllegalArgumentException("Invalid JSON in $source: ${e.message}", e)
            }
        return buildString {
            write(element, 0)
            append('\n')
        }
    }

    private fun StringBuilder.write(
        element: JsonElement,
        depth: Int,
    ) {
        when (element) {
            is JsonObject -> writeObject(element, depth)
            is JsonArray -> writeArray(element, depth)
            is JsonPrimitive -> append(element.toString())
        }
    }

    private fun StringBuilder.writeObject(
        obj: JsonObject,
        depth: Int,
    ) {
        if (obj.isEmpty()) {
            append("{}")
            return
        }
        append("{\n")
        obj.entries.sortedBy { it.key }.forEachIndexed { index, (key, value) ->
            if (index > 0) append(",\n")
            indent(depth + 1)
            append(JsonPrimitive(key).toString()).append(": ")
            write(value, depth + 1)
        }
        append('\n')
        indent(depth)
        append('}')
    }

    private fun StringBuilder.writeArray(
        array: JsonArray,
        depth: Int,
    ) {
        if (array.isEmpty()) {
            append("[]")
            return
        }
        append("[\n")
        array.forEachIndexed { index, value ->
            if (index > 0) append(",\n")
            indent(depth + 1)
            write(value, depth + 1)
        }
        append('\n')
        indent(depth)
        append(']')
    }

    private fun StringBuilder.indent(depth: Int) {
        repeat(depth) { append("  ") }
    }
}
