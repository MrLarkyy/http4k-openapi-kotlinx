package io.github.mrlarkyy.http4k.openapi.http4k

import io.github.mrlarkyy.http4k.openapi.http4k.internal.JsonSchemaAccess
import io.github.mrlarkyy.http4k.openapi.http4k.internal.SchemaFixtures
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNamingStrategy
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.http4k.contract.contract
import org.http4k.contract.jsonschema.JsonSchema
import org.http4k.contract.meta
import org.http4k.contract.openapi.OpenApiVersion
import org.http4k.contract.openapi.v3.OpenApi3
import org.http4k.contract.openapi.v3.OpenApi3ApiRenderer
import org.http4k.core.Body
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.format.ConfigurableKotlinxSerialization
import org.http4k.format.KotlinxSerialization
import org.http4k.format.KotlinxSerialization.auto
import org.http4k.routing.bind
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class KotlinxApiRendererTest {
    private val subject = KotlinxApiRenderer(KotlinxSerialization)
    private val http4k = OpenApi3ApiRenderer(KotlinxSerialization)
    private val item = SchemaFixtures.Item("a", 1, 2L, 1.5, true, SchemaFixtures.Inner("x"))

    @Test
    fun `a serializable example becomes a reference with its definitions`() {
        val schema = subject.toSchema(item, null, null)

        assertEquals(ref("Item"), JsonSchemaAccess.node(schema))
        assertEquals(listOf("Inner", "Item"), JsonSchemaAccess.definitionOrder(schema))
        val definitions = JsonSchemaAccess.definitions(schema)
        assertEquals(
            parse("""{"type":"object","properties":{"id":{"type":"string"}},"required":["id"]}"""),
            definitions.getValue("Inner"),
        )
        assertEquals(
            parse(
                """
                {"type":"object","properties":{
                  "name":{"type":"string"},"count":{"type":"integer"},"big":{"type":"integer"},
                  "price":{"type":"number"},"ok":{"type":"boolean"},"inner":{"${'$'}ref":"#/components/schemas/Inner"}},
                 "required":["name","count","big","price","ok","inner"]}
                """.trimIndent(),
            ),
            definitions.getValue("Item"),
        )
    }

    @Test
    fun `an object without properties has no required list`() {
        val schema = subject.toSchema(SchemaFixtures.Marker, null, null)

        assertEquals(
            mapOf("Marker" to parse("""{"type":"object","properties":{}}""")),
            JsonSchemaAccess.definitions(schema),
        )
    }

    @Test
    fun `the same type again gives the same output and a shared type is accepted`() {
        val first = subject.toSchema(item, null, null)
        val again = subject.toSchema(item, null, null)
        val sharing = subject.toSchema(SchemaFixtures.Wrapper(SchemaFixtures.Inner("y"), "l"), null, null)

        assertEquals(first, again)
        assertEquals(listOf("Inner", "Wrapper"), JsonSchemaAccess.definitionOrder(sharing))
        assertEquals(JsonSchemaAccess.definitions(first).getValue("Inner"), JsonSchemaAccess.definitions(sharing).getValue("Inner"))
    }

    @Test
    fun `an override id names the root and a prefix names every schema`() {
        val overridden = subject.toSchema(item, "Custom", null)
        val prefixed = KotlinxApiRenderer(KotlinxSerialization).toSchema(item, null, "api_")

        assertEquals(ref("Custom"), JsonSchemaAccess.node(overridden))
        assertEquals(listOf("Custom", "Inner"), JsonSchemaAccess.definitionOrder(overridden))
        assertEquals(ref("api_Item"), JsonSchemaAccess.node(prefixed))
        assertEquals(listOf("api_Inner", "api_Item"), JsonSchemaAccess.definitionOrder(prefixed))
        assertEquals(
            ref("api_Inner"),
            JsonSchemaAccess
                .definitions(prefixed)
                .getValue("api_Item")
                .jsonObject
                .getValue("properties")
                .jsonObject
                .getValue("inner"),
        )
    }

    @Test
    fun `an override id that another schema already uses fails`() {
        val failure = assertFailsWith<IllegalStateException> { subject.toSchema(item, "Inner", null) }

        assertTrue(failure.message.orEmpty().contains("Inner"), failure.message)
    }

    @Test
    fun `json elements are rendered as http4k renders them`() {
        val example: JsonElement = parse("""{"name":"a","count":1,"nested":{"id":"x"}}""")

        assertEquals(http4k.toSchema(example, null, null), subject.toSchema(example, null, null))
        assertEquals(http4k.toSchema(JsonPrimitive("a"), "Id", "p"), subject.toSchema(JsonPrimitive("a"), "Id", "p"))
    }

    @Test
    fun `examples that are not classes or objects are rendered as http4k renders them`() {
        assertEquals(outcome { http4k.toSchema("text", null, null) }, outcome { subject.toSchema("text", null, null) })
        assertEquals(outcome { http4k.toSchema(42, null, null) }, outcome { subject.toSchema(42, null, null) })
    }

    @Test
    fun `a class without a serializer is rendered as http4k renders it`() {
        val example = SchemaFixtures.NotSerializable("a")

        assertEquals(outcome { http4k.toSchema(example, null, null) }, outcome { subject.toSchema(example, null, null) })
    }

    @Test
    fun `an anonymous object, which http4k uses as a probe, is answered with an empty object schema`() {
        val probe = object {}

        assertEquals(http4k.toSchema(JsonObject(emptyMap()), null, "p"), subject.toSchema(probe, null, "p"))
    }

    @Test
    fun `two types with one schema name fail naming both serial names`() {
        subject.toSchema(SchemaFixtures.DupFirst("a"), null, null)

        val failure = assertFailsWith<IllegalStateException> { subject.toSchema(SchemaFixtures.DupSecond("b"), null, null) }

        val message = failure.message.orEmpty()
        assertTrue(message.contains("first.Dup") && message.contains("second.Dup"), message)
    }

    @Test
    fun `two classes with one serial name but different properties fail instead of sharing a schema`() {
        subject.toSchema(SchemaFixtures.SameV1("a"), null, null)

        val failure = assertFailsWith<IllegalStateException> { subject.toSchema(SchemaFixtures.SameV2("b"), null, null) }

        assertTrue(failure.message.orEmpty().contains("Schema name 'Same' is used by two different schemas"), failure.message)
    }

    @Test
    fun `a schema that http4k renders under a name this renderer uses fails`() {
        subject.toSchema(item, null, null)
        val example: JsonElement = parse("""{"other":1}""")

        val failure = assertFailsWith<IllegalStateException> { subject.toSchema(example, "Item", null) }

        assertTrue(failure.message.orEmpty().contains("Schema name 'Item' is used by two different schemas"), failure.message)
    }

    @Test
    fun `a value class example fails because its JSON is a primitive`() {
        val failure = assertFailsWith<IllegalArgumentException> { subject.toSchema(SchemaFixtures.Id("a"), null, null) }

        assertTrue(failure.message.orEmpty().contains("inline CLASS not supported yet"), failure.message)
    }

    @Test
    fun `a long schema name is shortened in the message`() {
        val long = "x".repeat(300)
        subject.toSchema(item, long, null)

        val failure =
            assertFailsWith<IllegalStateException> {
                subject.toSchema(SchemaFixtures.Wrapper(SchemaFixtures.Inner("y"), "l"), long, null)
            }

        val message = failure.message.orEmpty()
        assertTrue(message.contains("...") && !message.contains("x".repeat(250)), message)
    }

    @Test
    fun `an unsupported property fails naming the type and the property`() {
        val failure = assertFailsWith<IllegalArgumentException> { subject.toSchema(SchemaFixtures.Nullable("a"), null, null) }

        assertTrue(failure.message.orEmpty().contains("Nullable.value: nullable STRING not supported yet"), failure.message)
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `a json with a naming strategy is rejected`() {
        val snakeCase = ConfigurableKotlinxSerialization(json = { namingStrategy = JsonNamingStrategy.SnakeCase })

        assertFailsWith<IllegalArgumentException> { KotlinxApiRenderer(snakeCase) }
    }

    @Test
    fun `eight threads rendering at once produce the same output as one`() {
        val wrapper = SchemaFixtures.Wrapper(SchemaFixtures.Inner("y"), "l")
        val expected = KotlinxApiRenderer().let { snapshot(it.toSchema(item, null, null)) to snapshot(it.toSchema(wrapper, null, null)) }
        val shared = KotlinxApiRenderer()
        val barrier = CyclicBarrier(THREADS)
        val pool = Executors.newFixedThreadPool(THREADS)
        try {
            val futures =
                (1..THREADS).map { index ->
                    pool.submit<Pair<Snapshot, Snapshot>> {
                        barrier.await(30, TimeUnit.SECONDS)
                        if (index % 2 == 0) {
                            snapshot(shared.toSchema(item, null, null)) to snapshot(shared.toSchema(wrapper, null, null))
                        } else {
                            val second = snapshot(shared.toSchema(wrapper, null, null))
                            snapshot(shared.toSchema(item, null, null)) to second
                        }
                    }
                }

            futures.forEach { assertEquals(expected, it.get(30, TimeUnit.SECONDS)) }
        } finally {
            pool.shutdownNow()
        }
    }

    @Test
    fun `a contract renders request and response schemas once under components`() {
        val lens = Body.auto<SchemaFixtures.Item>().toLens()
        for (version in listOf(OpenApiVersion._3_0_0, OpenApiVersion._3_1_0)) {
            val handler =
                contract {
                    renderer =
                        OpenApi3(
                            TestApiInfo.create("kotlinx", "v1"),
                            KotlinxSerialization,
                            apiRenderer = KotlinxApiRenderer(),
                            version = version,
                        )
                    descriptionPath = "/openapi.json"
                    routes +=
                        "/items" meta {
                            receiving(lens to item)
                            returning(OK, lens to item)
                        } bindContract POST to { _: Request -> Response(OK) }
                    routes +=
                        "/items" meta {
                            returning(OK, lens to item)
                        } bindContract GET to { _: Request -> Response(OK) }
                }

            val document = Json.parseToJsonElement(handler(Request(GET, "/openapi.json")).bodyString()).jsonObject

            val components = document.getValue("components").jsonObject
            val schemas = components.getValue("schemas").jsonObject
            assertEquals(listOf("Inner", "Item"), schemas.keys.toList(), version.toString())
            val paths = document.getValue("paths").jsonObject
            val operations = paths.getValue("/items").jsonObject
            val post = operations.getValue("post").jsonObject
            val responses = post.getValue("responses").jsonObject
            assertEquals(ref("Item"), content(post.getValue("requestBody")))
            assertEquals(ref("Item"), content(responses.getValue("200")))
        }
    }

    private fun content(message: JsonElement): JsonElement =
        message.jsonObject
            .getValue("content")
            .jsonObject
            .getValue("application/json")
            .jsonObject
            .getValue("schema")

    private fun ref(name: String): JsonElement = parse("""{"${'$'}ref":"#/components/schemas/$name"}""")

    private fun parse(text: String): JsonElement = Json.parseToJsonElement(text)

    private fun outcome(block: () -> JsonSchema<JsonElement>): Any = runCatching(block).fold({ it }, { it.javaClass })

    private fun snapshot(schema: JsonSchema<JsonElement>): Snapshot =
        Snapshot(JsonSchemaAccess.node(schema), JsonSchemaAccess.definitionOrder(schema), JsonSchemaAccess.definitions(schema))

    private data class Snapshot(
        val node: JsonElement,
        val order: List<String>,
        val definitions: Map<String, JsonElement>,
    )

    private companion object {
        const val THREADS = 8
    }
}
