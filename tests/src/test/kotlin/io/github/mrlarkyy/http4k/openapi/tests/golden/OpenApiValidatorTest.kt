package io.github.mrlarkyy.http4k.openapi.tests.golden

import org.opentest4j.AssertionFailedError
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class OpenApiValidatorTest {
    private fun minimal(version: String) = """{"openapi":"$version","info":{"title":"t","version":"1"},"paths":{}}"""

    @Test
    fun `minimal 3_0 document is valid`() {
        OpenApiValidator.validate(OpenApiSpec.V3_0, minimal("3.0.3"))
    }

    @Test
    fun `minimal 3_1 document is valid`() {
        OpenApiValidator.validate(OpenApiSpec.V3_1, minimal("3.1.0"))
    }

    @Test
    fun `3_1 document with another json schema dialect is validated without the openapi dialect`() {
        val other =
            minimal(
                "3.1.0",
            ).replace("{\"openapi\"", "{\"jsonSchemaDialect\":\"https://json-schema.org/draft/2020-12/schema\",\"openapi\"")

        OpenApiValidator.validate(OpenApiSpec.V3_1, other)
        assertFailsWith<AssertionFailedError> {
            OpenApiValidator.validate(OpenApiSpec.V3_1, other.replace("\"info\":{\"title\":\"t\",\"version\":\"1\"},", ""))
        }
    }

    @Test
    fun `3_1 document with the base dialect has its schema objects checked`() {
        fun document(schema: String) =
            """
            {"openapi":"3.1.0","jsonSchemaDialect":"https://spec.openapis.org/oas/3.1/dialect/base",
             "info":{"title":"t","version":"1"},"components":{"schemas":{"S":$schema}}}
            """.trimIndent()

        OpenApiValidator.validate(OpenApiSpec.V3_1, document("""{"type":"string"}"""))
        val failure =
            assertFailsWith<AssertionFailedError> { OpenApiValidator.validate(OpenApiSpec.V3_1, document("""{"type":5}""")) }
        assertContains(failure.message.orEmpty(), "/components/schemas/S/type")
    }

    @Test
    fun `3_1 document with the dated dialect has its schema objects checked`() {
        val document =
            """
            {"openapi":"3.1.0","jsonSchemaDialect":"https://spec.openapis.org/oas/3.1/dialect/2024-11-10",
             "info":{"title":"t","version":"1"},"components":{"schemas":{"S":{"type":5}}}}
            """.trimIndent()

        assertFailsWith<AssertionFailedError> { OpenApiValidator.validate(OpenApiSpec.V3_1, document) }
    }

    @Test
    fun `3_1 document with a foreign dialect does not have its schema objects checked`() {
        val document =
            """
            {"openapi":"3.1.0","jsonSchemaDialect":"https://json-schema.org/draft/2020-12/schema",
             "info":{"title":"t","version":"1"},"components":{"schemas":{"S":{"type":5}}}}
            """.trimIndent()

        OpenApiValidator.validate(OpenApiSpec.V3_1, document)
    }

    @Test
    fun `missing info fails for both versions`() {
        for ((spec, version) in listOf(OpenApiSpec.V3_0 to "3.0.3", OpenApiSpec.V3_1 to "3.1.0")) {
            val failure =
                assertFailsWith<AssertionFailedError> {
                    OpenApiValidator.validate(spec, """{"openapi":"$version","paths":{}}""")
                }
            assertContains(failure.message.orEmpty(), "info")
        }
    }

    @Test
    fun `version prefix mismatch names both versions`() {
        val failure =
            assertFailsWith<AssertionFailedError> {
                OpenApiValidator.validate(OpenApiSpec.V3_1, minimal("3.0.3"))
            }
        assertContains(failure.message.orEmpty(), "3.0.3")
        assertContains(failure.message.orEmpty(), "3.1.")
    }

    @Test
    fun `errors are capped at twenty with the total`() {
        val operations = (1..30).joinToString(",") { """"/p$it":{"get":{"responses":5}}""" }
        val document = """{"openapi":"3.0.3","info":{"title":"t","version":"1"},"paths":{$operations}}"""
        val failure = assertFailsWith<AssertionFailedError> { OpenApiValidator.validate(OpenApiSpec.V3_0, document) }
        val message = failure.message.orEmpty()
        assertContains(message, "more")
        assertContains(message, "error(s)")
        val listed = message.lines().count { it.startsWith("  /") || it.startsWith("  $") }
        assertEquals(OpenApiValidator.MAX_ERRORS, listed, message)
    }

    @Test
    fun `3_1 schema objects are checked against the dialect`() {
        val document =
            """
            {"openapi":"3.1.0","info":{"title":"t","version":"1"},
             "components":{"schemas":{"Broken":{"type":5}}}}
            """.trimIndent()
        val failure = assertFailsWith<AssertionFailedError> { OpenApiValidator.validate(OpenApiSpec.V3_1, document) }
        assertContains(failure.message.orEmpty(), "/components/schemas/Broken/type")
        assertContains(failure.message.orEmpty(), "integer")
    }

    @Test
    fun `bundled schemas load offline`() {
        OpenApiValidator.loadAll()
    }

    @Test
    fun `an unknown http iri fails instead of being fetched`() {
        val failure = assertFailsWith<RuntimeException> { OpenApiValidator.load("https://example.invalid/schema.json") }
        val messages = generateSequence<Throwable>(failure) { it.cause }.map { it.message.orEmpty() }.toList()
        assertTrue(messages.any { "https://example.invalid/schema.json" in it && "not allowed" in it }, messages.toString())
    }
}
