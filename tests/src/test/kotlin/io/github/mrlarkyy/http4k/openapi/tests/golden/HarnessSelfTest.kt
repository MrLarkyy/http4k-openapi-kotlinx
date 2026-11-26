package io.github.mrlarkyy.http4k.openapi.tests.golden

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import org.junit.jupiter.api.io.TempDir
import org.opentest4j.AssertionFailedError
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.listDirectoryEntries
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Proves the golden harness fails where it must; every test passes by asserting the failure. */
class HarnessSelfTest {
    @TempDir
    lateinit var temp: Path

    private fun settings(mode: ApprovalMode): GoldenSettings =
        GoldenSettings(temp.resolve("golden"), temp.resolve("received"), MatrixEntry.LOWEST, mode)

    private fun valid(spec: OpenApiSpec): String = ContractFixtures.render(spec, emptyList())

    private fun mutated(
        spec: OpenApiSpec,
        change: (MutableMap<String, JsonElement>) -> Unit,
    ): String {
        val root = Json.parseToJsonElement(valid(spec)) as JsonObject
        val fields = root.toMutableMap()
        change(fields)
        return JsonObject(fields).toString()
    }

    @Test
    fun `a changed approved file fails with a diff and the received path`() {
        val actual = CanonicalJson.canonical(valid(OpenApiSpec.V3_0), "self-test")
        val broken = actual.replace("\"title\": \"golden\"", "\"title\": \"changed\"")
        assertTrue(broken != actual, "the fixture title must be present in the canonical document")
        val settings = settings(ApprovalMode.Verify)
        GoldenFiles(settings.dir, settings.receivedDir, settings.entry).approve("broken", broken)

        val failure =
            assertFailsWith<AssertionFailedError> {
                GoldenOpenApi.verify("broken", OpenApiSpec.V3_0, { valid(OpenApiSpec.V3_0) }, settings)
            }

        val message = failure.message.orEmpty()
        assertTrue(message.lines().any { it.startsWith("-") && it.contains("\"changed\"") }, message)
        assertTrue(message.lines().any { it.startsWith("+") && it.contains("\"golden\"") }, message)
        assertTrue(message.contains(settings.receivedDir.toString()), message)
        assertEquals(1, settings.receivedDir.listDirectoryEntries().size)
    }

    @Test
    fun `a document without info fails naming info`() {
        for (spec in OpenApiSpec.entries) {
            val failure =
                assertFailsWith<AssertionFailedError>("$spec") {
                    GoldenOpenApi.verify("no-info", spec, { mutated(spec) { it.remove("info") } }, settings(ApprovalMode.Verify))
                }
            assertTrue(failure.message.orEmpty().contains("info"), failure.message)
        }
    }

    @Test
    fun `paths as an array fails naming paths`() {
        for (spec in OpenApiSpec.entries) {
            val failure =
                assertFailsWith<AssertionFailedError>("$spec") {
                    GoldenOpenApi.verify(
                        "array-paths",
                        spec,
                        { mutated(spec) { it["paths"] = JsonArray(emptyList()) } },
                        settings(ApprovalMode.Verify),
                    )
                }
            assertTrue(failure.message.orEmpty().contains("/paths"), failure.message)
        }
    }

    @Test
    fun `a document of another openapi version fails naming both versions`() {
        val failure =
            assertFailsWith<AssertionFailedError> {
                GoldenOpenApi.verify("wrong-version", OpenApiSpec.V3_1, { valid(OpenApiSpec.V3_0) }, settings(ApprovalMode.Verify))
            }

        val message = failure.message.orEmpty()
        assertTrue(message.contains("3.1") && message.contains("3.0"), message)
    }

    @Test
    fun `approving on ci is refused and writes nothing`() {
        val failure = assertFailsWith<IllegalStateException> { ApprovalMode.resolve("true", "true") }

        assertTrue(failure.message.orEmpty().contains("CI"), failure.message)
        assertTrue(Files.notExists(temp.resolve("golden")))
    }

    @Test
    fun `approving an invalid document writes no file`() {
        val settings = settings(ApprovalMode.Approve)

        assertFailsWith<AssertionFailedError> {
            GoldenOpenApi.verify("invalid", OpenApiSpec.V3_0, { mutated(OpenApiSpec.V3_0) { it.remove("info") } }, settings)
        }

        val written = if (Files.exists(settings.dir)) settings.dir.listDirectoryEntries() else emptyList()
        assertEquals(emptyList(), written)
    }

    @Test
    fun `approving a nondeterministic document writes no file`() {
        val settings = settings(ApprovalMode.Approve)
        var calls = 0

        assertFailsWith<AssertionFailedError> {
            GoldenOpenApi.verify("flaky", OpenApiSpec.V3_0, { valid(OpenApiSpec.V3_0).replace("golden", "run${calls++}") }, settings)
        }

        val written = if (Files.exists(settings.dir)) settings.dir.listDirectoryEntries() else emptyList()
        assertEquals(emptyList(), written)
    }
}
