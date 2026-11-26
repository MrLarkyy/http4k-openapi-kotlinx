package io.github.mrlarkyy.http4k.openapi.tests.golden

import com.networknt.schema.InputFormat
import com.networknt.schema.Schema
import com.networknt.schema.SchemaLocation
import com.networknt.schema.SchemaRegistry
import com.networknt.schema.SchemaRegistryConfig
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.opentest4j.AssertionFailedError
import java.util.Locale

/**
 * Validates rendered documents against the official OpenAPI JSON Schemas bundled under `openapi-schemas/`. Every
 * schema is read from the classpath; a reference to any other `http(s)` IRI fails instead of being fetched, except
 * the `json-schema.org` meta-schemas, which ship inside the validator jar.
 */
object OpenApiValidator {
    const val MAX_ERRORS = 20

    private const val IRI_PREFIX = "https://spec.openapis.org/oas/"
    private const val RESOURCE_PREFIX = "openapi-schemas/oas/"
    private const val OAS_DIALECT = "https://spec.openapis.org/oas/3.1/dialect/2024-11-10"
    private const val OAS_DIALECT_BASE = "https://spec.openapis.org/oas/3.1/dialect/base"
    private const val JSON_SCHEMA_ORG_HTTP = "http://json-schema.org/"
    private const val JSON_SCHEMA_ORG_HTTPS = "https://json-schema.org/"

    private val schemas: Map<OpenApiSpec, Lazy<Schema>> =
        OpenApiSpec.entries.associateWith { spec -> lazy { load(spec.schemaIri) } }

    /** Loads the root schema of every [OpenApiSpec]; fails when a bundled schema or a reference is missing. */
    fun loadAll() {
        OpenApiSpec.entries.forEach { schema(it) }
    }

    /** Loads the schema registered under [iri] from the classpath only. */
    fun load(iri: String): Schema = registry().getSchema(SchemaLocation.of(iri))

    /**
     * Validates [document] against [spec]: first the `openapi` version prefix, then the JSON Schema. Fails with an
     * [AssertionFailedError] listing at most [MAX_ERRORS] errors by instance location and the total count.
     */
    fun validate(
        spec: OpenApiSpec,
        document: String,
    ) {
        val root = checkVersion(spec, document)
        val anyDialect = spec.anyDialectSchemaIri?.takeIf { declaresForeignDialect(root) }
        val schemaIri = anyDialect ?: spec.schemaIri
        val checked = if (anyDialect == null && declaresDefaultDialect(root)) withoutDialect(root) else document
        val raw = (if (anyDialect != null) load(anyDialect) else schema(spec)).validate(checked, InputFormat.JSON)
        val errors = raw.sortedWith(compareBy({ it.instanceLocation.toString() }, { it.message }))
        if (errors.isEmpty()) return
        val listed = errors.take(MAX_ERRORS).joinToString("\n") { "  ${it.instanceLocation}: ${it.message}" }
        val omitted = if (errors.size > MAX_ERRORS) "\n  ... ${errors.size - MAX_ERRORS} more" else ""
        throw AssertionFailedError(
            "Document is not valid ${spec.name} ($schemaIri): ${errors.size} error(s)\n$listed$omitted",
        )
    }

    private fun isRemote(iri: String) = iri.startsWith("http://") || iri.startsWith("https://")

    private fun schema(spec: OpenApiSpec): Schema = checkNotNull(schemas[spec]).value

    private fun declaredDialect(root: JsonObject): String? = (root["jsonSchemaDialect"] as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun declaresForeignDialect(root: JsonObject): Boolean {
        val dialect = declaredDialect(root)
        return dialect != null && dialect != OAS_DIALECT && dialect != OAS_DIALECT_BASE
    }

    // schema-base pins jsonSchemaDialect to the dated dialect only, while the undated "base" IRI names the same dialect.
    private fun declaresDefaultDialect(root: JsonObject): Boolean = declaredDialect(root) == OAS_DIALECT_BASE

    private fun withoutDialect(root: JsonObject): String = JsonObject(root - "jsonSchemaDialect").toString()

    private fun checkVersion(
        spec: OpenApiSpec,
        document: String,
    ): JsonObject {
        val root =
            try {
                Json.parseToJsonElement(document)
            } catch (e: SerializationException) {
                throw AssertionFailedError("Document is not valid JSON: ${e.message}")
            }
        val declared = ((root as? JsonObject)?.get("openapi") as? JsonPrimitive)?.takeIf { it.isString }?.content
        if (declared == null || !declared.startsWith(spec.versionPrefix)) {
            throw AssertionFailedError(
                "Document declares openapi ${declared?.let { "\"$it\"" } ?: "nothing"} " +
                    "but ${spec.name} expects ${spec.versionPrefix}x",
            )
        }
        return root as JsonObject
    }

    private fun registry(): SchemaRegistry {
        val config = SchemaRegistryConfig.builder().locale(Locale.ENGLISH).build()
        return SchemaRegistry
            .builder()
            .schemaRegistryConfig(config)
            .schemaLoader { loader -> loader.block { iri -> isForeignRemote(iri.toString()) } }
            .schemas { iri -> bundled(iri) }
            .build()
    }

    private fun isForeignRemote(iri: String): Boolean =
        isRemote(iri) &&
            !iri.startsWith(IRI_PREFIX) &&
            !iri.startsWith(JSON_SCHEMA_ORG_HTTP) &&
            !iri.startsWith(JSON_SCHEMA_ORG_HTTPS)

    private fun bundled(iri: String): String? {
        if (!iri.startsWith(IRI_PREFIX)) return null
        val resource = "$RESOURCE_PREFIX${iri.removePrefix(IRI_PREFIX).substringBefore('#')}.json"
        val stream =
            OpenApiValidator::class.java.classLoader.getResourceAsStream(resource)
                ?: throw IllegalStateException("Bundled OpenAPI schema $resource for $iri is missing")
        return stream.use { String(it.readAllBytes(), Charsets.UTF_8) }
    }
}
