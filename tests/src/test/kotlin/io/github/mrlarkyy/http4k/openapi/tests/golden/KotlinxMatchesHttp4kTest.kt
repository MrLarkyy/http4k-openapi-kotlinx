package io.github.mrlarkyy.http4k.openapi.tests.golden

import io.github.mrlarkyy.http4k.openapi.http4k.KotlinxApiRenderer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.http4k.format.KotlinxSerialization
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Compares the document of `KotlinxApiRenderer` for a contract with `@Serializable` examples with the one the stock
 * `OpenApi3ApiRenderer` renders for the same contract. Only http4k's known deviations are normalised: it names
 * schemas `object<hash>`, adds an `example` to every property and always writes an empty `required`.
 */
class KotlinxMatchesHttp4kTest {
    @Test
    fun `serializable contract matches http4k as openapi 3_0`() {
        assertMatches(OpenApiSpec.V3_0)
    }

    @Test
    fun `serializable contract matches http4k as openapi 3_1`() {
        assertMatches(OpenApiSpec.V3_1)
    }

    private fun assertMatches(spec: OpenApiSpec) {
        val ours = renderOurs(spec)
        val http4k = parse(ContractFixtures.render(spec, SerializableContract.serializableRoutes()))

        assertEquals(canonical(parametersOf(http4k)), canonical(parametersOf(ours)), "parameters of $spec")
        assertEquals(canonical(withoutRequired(http4kNormalised(http4k))), canonical(withoutRequired(ours)), "document of $spec")
    }

    @Test
    fun `required is derived from the descriptor in declaration order`() {
        val ours = renderOurs(OpenApiSpec.V3_0)
        val schemas = schemasOf(ours)

        assertEquals(listOf("name", "count", "big", "price", "ok", "inner"), requiredOf(schemas, "Item"))
        assertEquals(listOf("id"), requiredOf(schemas, "Inner"))
    }

    private fun requiredOf(
        schemas: JsonObject,
        name: String,
    ): List<String> {
        val required = schemas.getValue(name).jsonObject.getValue("required")
        return required.jsonArray.map { it.jsonPrimitive.content }
    }

    private fun renderOurs(spec: OpenApiSpec): JsonObject {
        val renderer = KotlinxApiRenderer(KotlinxSerialization)
        return parse(ContractFixtures.render(spec, SerializableContract.serializableRoutes(), renderer))
    }

    private fun parametersOf(document: JsonObject): JsonElement {
        val paths = document.getValue("paths").jsonObject
        val path = paths.getValue("/items/{id}").jsonObject
        return path.getValue("get").jsonObject.getValue("parameters")
    }

    private fun parse(text: String): JsonObject = Json.parseToJsonElement(text).jsonObject

    private fun canonical(element: JsonElement): String = CanonicalJson.canonical(element.toString())

    private fun schemasOf(document: JsonObject): JsonObject {
        val components = document.getValue("components").jsonObject
        return components.getValue("schemas").jsonObject
    }

    /** http4k's document with serial-name schema names, without property examples and without its empty `required`. */
    private fun http4kNormalised(document: JsonObject): JsonObject {
        val renames = HashMap<String, String>()
        val schemas = LinkedHashMap<String, JsonElement>()
        for ((name, schema) in schemasOf(document)) {
            val body = schema.jsonObject
            val propertyNames = body["properties"]?.jsonObject?.keys.orEmpty()
            val serialName = SerializableContract.schemaNames[propertyNames]
            checkNotNull(serialName) { "Unexpected http4k schema '$name' with properties $propertyNames" }
            val required = body["required"] as? JsonArray
            check(required == null || required.isEmpty()) { "http4k derived required for '$name'" }
            renames[name] = serialName
            schemas[serialName] = withoutPropertyExamples(body)
        }
        val components = document.getValue("components").jsonObject
        val replaced = JsonObject(document + ("components" to JsonObject(components + ("schemas" to JsonObject(schemas)))))
        return renameRefs(replaced, renames).jsonObject
    }

    private fun withoutPropertyExamples(schema: JsonObject): JsonObject {
        val properties = schema["properties"]?.jsonObject ?: return schema
        val stripped = properties.mapValues { (_, property) -> JsonObject(property.jsonObject - "example") }
        return JsonObject(schema + ("properties" to JsonObject(stripped)))
    }

    private fun withoutRequired(document: JsonObject): JsonObject {
        val schemas = schemasOf(document).mapValues { (_, schema) -> JsonObject(schema.jsonObject - "required") }
        val components = document.getValue("components").jsonObject
        return JsonObject(document + ("components" to JsonObject(components + ("schemas" to JsonObject(schemas)))))
    }

    private fun renameRefs(
        element: JsonElement,
        renames: Map<String, String>,
    ): JsonElement =
        when (element) {
            is JsonObject -> {
                val renamed =
                    element.mapValues { (key, value) ->
                        if (key == "\$ref" && value is JsonPrimitive && value.isString) {
                            val name = value.content.substringAfterLast('/')
                            JsonPrimitive(value.content.removeSuffix(name) + renames.getValue(name))
                        } else {
                            renameRefs(value, renames)
                        }
                    }
                JsonObject(renamed)
            }

            is JsonArray -> {
                JsonArray(element.map { renameRefs(it, renames) })
            }

            else -> {
                element
            }
        }
}
