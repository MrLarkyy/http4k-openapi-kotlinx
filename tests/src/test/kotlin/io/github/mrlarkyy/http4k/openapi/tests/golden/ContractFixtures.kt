package io.github.mrlarkyy.http4k.openapi.tests.golden

import kotlinx.serialization.json.JsonElement
import org.http4k.contract.ContractRoute
import org.http4k.contract.contract
import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.ApiRenderer
import org.http4k.contract.openapi.v3.Api
import org.http4k.contract.openapi.v3.OpenApi3
import org.http4k.contract.openapi.v3.OpenApi3ApiRenderer
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.format.KotlinxSerialization

/** Renders real http4k `contract { }` definitions with the kotlinx.serialization renderer. */
object ContractFixtures {
    private const val DESCRIPTION_PATH = "/openapi.json"

    /** Renders the OpenAPI document of a contract with [routes] as [spec] and returns the raw response body. */
    fun render(
        spec: OpenApiSpec,
        routes: List<ContractRoute>,
    ): String = render(spec, routes, OpenApi3ApiRenderer(KotlinxSerialization))

    /** Renders the OpenAPI document of a contract with [routes] as [spec], building schemas with [apiRenderer]. */
    fun render(
        spec: OpenApiSpec,
        routes: List<ContractRoute>,
        apiRenderer: ApiRenderer<Api<JsonElement>, JsonElement>,
    ): String {
        val handler =
            contract {
                renderer =
                    OpenApi3(
                        apiInfo(),
                        KotlinxSerialization,
                        apiRenderer = apiRenderer,
                        version = spec.http4k,
                    )
                descriptionPath = DESCRIPTION_PATH
                this.routes += routes
            }
        val response = handler(Request(GET, DESCRIPTION_PATH))
        check(response.status == OK) { "Rendering $spec returned ${response.status}: ${response.bodyString()}" }
        return response.bodyString()
    }

    // ApiInfo gained constructor parameters between http4k 6.0.0.0 and 6.61.0.0, so no compiled call (including the
    // synthetic defaults one) links on both ends of the range; the shortest public constructor is picked reflectively.
    private fun apiInfo(): ApiInfo {
        val constructor =
            ApiInfo::class.java.constructors
                .filter { constructor -> constructor.parameterTypes.none { it.name.endsWith("DefaultConstructorMarker") } }
                .minByOrNull { it.parameterCount }
        checkNotNull(constructor) { "ApiInfo has no public constructor" }
        val arguments = arrayOfNulls<Any>(constructor.parameterCount)
        arguments[0] = "golden"
        arguments[1] = "v1"
        return constructor.newInstance(*arguments) as ApiInfo
    }
}
