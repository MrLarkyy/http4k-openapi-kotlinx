package io.github.mrlarkyy.http4k.openapi.http4k

import org.http4k.contract.openapi.ApiInfo
import java.lang.reflect.Constructor

/**
 * Builds an [ApiInfo] for tests on every supported http4k version.
 *
 * The constructor changed in http4k 6.54.0.0, and test code is compiled against 6.0.0.0 but also runs against newer
 * versions, so no compiled call links on both shapes. This is a deliberate guard: only the two known shapes are
 * accepted and anything else fails naming the constructors found. Library code never constructs [ApiInfo];
 * `Http4kLinkageTest` enforces that.
 */
internal object TestApiInfo {
    fun create(
        title: String,
        version: String,
    ): ApiInfo {
        val constructors = ApiInfo::class.java.constructors.toList()
        val string = String::class.java
        val threeStrings = constructors.firstOrNull { it.parameterTypes.toList() == listOf(string, string, string) }
        if (threeStrings != null) return threeStrings.newInstance(title, version, null) as ApiInfo

        val license = runCatching { Class.forName("org.http4k.contract.openapi.ApiLicense") }.getOrNull()
        val withLicense =
            license?.let { type ->
                constructors.firstOrNull { it.parameterTypes.toList() == listOf(string, string, string, string, type) }
            }
        if (withLicense != null) return withLicense.newInstance(title, version, null, null, null) as ApiInfo

        error("Unsupported ApiInfo constructors: ${constructors.joinToString { describe(it) }}")
    }

    private fun describe(constructor: Constructor<*>): String =
        constructor.parameterTypes.joinToString(prefix = "(", postfix = ")") { it.name }
}
