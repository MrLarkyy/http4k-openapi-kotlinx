package io.github.mrlarkyy.http4k.openapi.tests.golden

import org.http4k.contract.openapi.OpenApiVersion

/** The OpenAPI versions the golden tests can render and validate. */
enum class OpenApiSpec(
    val http4k: OpenApiVersion,
    val versionPrefix: String,
    val schemaIri: String,
) {
    V3_0(OpenApiVersion._3_0_0, "3.0.", "https://spec.openapis.org/oas/3.0/schema/2024-10-18"),
    V3_1(OpenApiVersion._3_1_0, "3.1.", "https://spec.openapis.org/oas/3.1/schema-base/2026-11-05"),
}
