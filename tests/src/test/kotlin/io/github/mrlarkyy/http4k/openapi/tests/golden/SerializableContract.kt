package io.github.mrlarkyy.http4k.openapi.tests.golden

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.http4k.contract.ContractRoute
import org.http4k.contract.RouteMetaDsl
import org.http4k.contract.div
import org.http4k.contract.meta
import org.http4k.core.Body
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.format.KotlinxSerialization.auto
import org.http4k.lens.Header
import org.http4k.lens.Path
import org.http4k.lens.Query
import org.http4k.lens.int
import org.http4k.lens.string

/**
 * One contract in two forms: with `@Serializable` examples, and with the same examples as JSON objects, which is the
 * only form http4k renders correctly itself.
 */
object SerializableContract {
    @Serializable
    data class Inner(
        val id: String,
    )

    @Serializable
    data class Item(
        val name: String,
        val count: Int,
        val big: Long,
        val price: Double,
        val ok: Boolean,
        val inner: Inner,
    )

    val item = Item("pen", 3, 4_000_000_000L, 1.5, true, Inner("i-1"))

    /** Serial names of the schemas by the property names they have, to name http4k's hash-named schemas. */
    val schemaNames: Map<Set<String>, String> =
        mapOf(
            setOf("name", "count", "big", "price", "ok", "inner") to "Item",
            setOf("id") to "Inner",
        )

    /** POST and GET routes whose bodies are `@Serializable` [Item] examples, with path, query and header parameters. */
    fun serializableRoutes(): List<ContractRoute> {
        val lens = Body.auto<Item>().toLens()
        return routes(
            requestBody = { receiving(lens to item) },
            okResponse = { returning(OK, lens to item) },
        )
    }

    /** The same routes with the examples given as JSON objects. */
    fun jsonObjectRoutes(): List<ContractRoute> {
        val lens = Body.auto<JsonElement>().toLens()
        val example = Json.encodeToJsonElement(Item.serializer(), item)
        return routes(
            requestBody = { receiving(lens to example) },
            okResponse = { returning(OK, lens to example) },
        )
    }

    private fun routes(
        requestBody: RouteMetaDsl.() -> Unit,
        okResponse: RouteMetaDsl.() -> Unit,
    ): List<ContractRoute> {
        val id = Path.string().of("id", "The item id")
        val limit = Query.int().optional("limit", "Maximum number of results")
        val trace = Header.optional("X-Trace", "Correlation id")

        val create =
            "/items" meta {
                summary = "Create an item"
                description = "Creates an item from the request body."
                requestBody()
                okResponse()
            } bindContract POST to { _: Request -> Response(OK) }

        val fetch =
            "/items" / id meta {
                summary = "Fetch an item"
                description = "Returns one item by id."
                queries += limit
                headers += trace
                okResponse()
                returning(NOT_FOUND to "No such item")
            } bindContract GET to { _ -> { _: Request -> Response(OK) } }

        return listOf(create, fetch)
    }
}
