package io.github.mrlarkyy.http4k.openapi.http4k.linkage

import org.http4k.contract.openapi.ApiInfo

/**
 * Never invoked. Compiled against http4k 6.0.0.0, this call references the synthetic defaults constructor of
 * `ApiInfo`, which http4k removed in 6.54.0.0; Http4kLinkageTest uses the class to prove the checker sees that break.
 */
internal class LegacyApiInfoCaller {
    fun create(): ApiInfo = ApiInfo("t", "v")
}
