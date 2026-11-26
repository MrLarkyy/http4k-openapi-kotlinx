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
        val body = response.bodyString()
        assertTrue(Regex("\"openapi\"\\s*:\\s*\"3\\.").containsMatchIn(body), body)
        assertTrue(Regex("\"title\"\\s*:\\s*\"smoke\"").containsMatchIn(body), body)
        assertTrue(Regex("\"version\"\\s*:\\s*\"v1\"").containsMatchIn(body), body)
    }

    private fun apiInfo(): ApiInfo = TestApiInfo.create("smoke", "v1")
}
