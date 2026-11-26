package io.github.mrlarkyy.http4k.openapi.tests.golden

import io.github.mrlarkyy.http4k.openapi.http4k.KotlinxApiRenderer
import org.http4k.contract.ContractRoute
import org.http4k.contract.div
import org.http4k.contract.meta
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.CREATED
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.format.KotlinxSerialization
import org.http4k.lens.Header
import org.http4k.lens.Path
import org.http4k.lens.Query
import org.http4k.lens.int
import org.http4k.lens.string
import kotlin.test.Test

class ContractGoldenTest {
    @Test
    fun `basic contract as openapi 3_0`() {
        GoldenOpenApi.verify("contract-basic-3.0", OpenApiSpec.V3_0, { ContractFixtures.render(OpenApiSpec.V3_0, basicContract()) })
    }

    @Test
    fun `basic contract as openapi 3_1`() {
        GoldenOpenApi.verify("contract-basic-3.1", OpenApiSpec.V3_1, { ContractFixtures.render(OpenApiSpec.V3_1, basicContract()) })
    }

    @Test
    fun `serializable contract as openapi 3_0`() {
        verifySerializable("contract-serializable-3.0", OpenApiSpec.V3_0)
    }

    @Test
    fun `serializable contract as openapi 3_1`() {
        verifySerializable("contract-serializable-3.1", OpenApiSpec.V3_1)
    }

    private fun verifySerializable(
        name: String,
        spec: OpenApiSpec,
    ) {
        GoldenOpenApi.verify(
            name,
            spec,
            { ContractFixtures.render(spec, SerializableContract.serializableRoutes(), KotlinxApiRenderer(KotlinxSerialization)) },
        )
    }

    private fun basicContract(): List<ContractRoute> {
        val id = Path.string().of("id", "The item id")
        val limit = Query.int().optional("limit", "Maximum number of results")
        val trace = Header.optional("X-Trace", "Correlation id")
        val body = Body.string(ContentType.APPLICATION_JSON, "The item to create").toLens()

        val get =
            "/items" / id meta {
                summary = "Fetch an item"
                description = "Returns one item by id."
                queries += limit
                headers += trace
                returning(OK to "The item")
                returning(NOT_FOUND to "No such item")
            } bindContract GET to { _ -> { _: Request -> Response(OK) } }

        val create =
            "/items" meta {
                summary = "Create an item"
                description = "Creates an item from the request body."
                receiving(body)
                returning(CREATED to "The created item")
                returning(NOT_FOUND to "No such parent")
            } bindContract POST to { _: Request -> Response(CREATED) }

        return listOf(get, create)
    }
}
