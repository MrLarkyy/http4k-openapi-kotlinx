package io.github.mrlarkyy.http4k.openapi.tests.golden

import org.http4k.contract.openapi.OpenApiVersion

/**
 * The OpenAPI versions the golden tests can render and validate. [schemaIri] is the schema that also checks Schema
 * Objects against the OpenAPI dialect; [anyDialectSchemaIri], when set, validates documents that declare a different
 * `jsonSchemaDialect`, whose Schema Objects the OpenAPI dialect cannot judge.
 */
enum class OpenApiSpec(
    val http4k: OpenApiVersion,
    val versionPrefix: String,
    val schemaIri: String,
    val anyDialectSchemaIri: String? = null,
) {
    V3_0(OpenApiVersion._3_0_0, "3.0.", "https://spec.openapis.org/oas/3.0/schema/2024-10-18"),
    V3_1(
        OpenApiVersion._3_1_0,
        "3.1.",
        "https://spec.openapis.org/oas/3.1/schema-base/2026-11-05",
        "https://spec.openapis.org/oas/3.1/schema/2026-11-05",
    ),
}
