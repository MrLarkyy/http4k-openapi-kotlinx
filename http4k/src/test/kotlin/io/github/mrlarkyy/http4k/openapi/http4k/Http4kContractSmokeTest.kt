package io.github.mrlarkyy.http4k.openapi.http4k

import org.http4k.contract.contract
import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.v3.OpenApi3
import org.http4k.contract.openapi.v3.OpenApi3ApiRenderer
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.format.KotlinxSerialization
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Http4kContractSmokeTest {
    @Test
    fun `contract renders an openapi 3 document`() {
        val handler =
            contract {
                renderer =
                    OpenApi3(
                        apiInfo(),
                        KotlinxSerialization,
                        apiRenderer = OpenApi3ApiRenderer(KotlinxSerialization),
                    )
                descriptionPath = "/openapi.json"
            }

        val response = handler(Request(GET, "/openapi.json"))

        assertEquals(OK, response.status)
        assertTrue(Regex("\"openapi\"\\s*:\\s*\"3\\.").containsMatchIn(response.bodyString()), response.bodyString())
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
        arguments[0] = "smoke"
        arguments[1] = "v1"
        return constructor.newInstance(*arguments) as ApiInfo
    }
}
