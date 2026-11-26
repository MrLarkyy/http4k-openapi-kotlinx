package io.github.mrlarkyy.http4k.openapi.http4k.linkage

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class Http4kLinkageTest {
    private val prefixes = listOf("org/http4k/", "kotlinx/serialization/")
    private val classLoader = Http4kLinkageTest::class.java.classLoader

    private val runtimeHttp4k: List<Int> =
        requireNotNull(System.getProperty("h4oa.http4k.version")) {
            "System property h4oa.http4k.version is not set; the version-matrix build wiring is lost"
        }.split('.').map { it.toInt() }

    @Test
    fun `main classes only reference http4k and serialization members the runtime has`() {
        val roots =
            requireNotNull(System.getProperty("h4oa.linkage.classes")) {
                "System property h4oa.linkage.classes is not set; the http4k build wiring is lost"
            }.split(File.pathSeparator).filter { it.isNotEmpty() }.map(::File)

        val missing =
            roots
                .filter { it.isDirectory }
                .flatMap { root -> root.walkTopDown().filter { it.isFile && it.extension == "class" }.toList() }
                .sortedBy { it.path }
                .flatMap { LinkageCheck.references(it.readBytes(), prefixes) }
                .toSet()
                .let { LinkageCheck.missing(it, classLoader) }

        val version = runtimeHttp4k.joinToString(".")
        assertTrue(missing.isEmpty(), "Unresolved references against http4k $version:\n${missing.joinToString("\n")}")
    }

    @Test
    fun `fixture compiled against the lowest http4k shows the ApiInfo constructor break`() {
        val bytes =
            assertNotNull(
                classLoader.getResourceAsStream("io/github/mrlarkyy/http4k/openapi/http4k/linkage/LegacyApiInfoCaller.class"),
                "fixture class file not found",
            ).use { it.readBytes() }

        val missing = LinkageCheck.missing(LinkageCheck.references(bytes, prefixes), classLoader)

        val changesAt = listOf(6, 54, 0, 0)
        val expected =
            if (compare(runtimeHttp4k, changesAt) < 0) {
                emptyList()
            } else {
                listOf(
                    "constructor org/http4k/contract/openapi/ApiInfo.<init>" +
                        "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ILkotlin/jvm/internal/DefaultConstructorMarker;)V",
                )
            }
        assertEquals(expected, missing, "http4k ${runtimeHttp4k.joinToString(".")}")
    }

    private fun compare(
        left: List<Int>,
        right: List<Int>,
    ): Int {
        for (index in 0 until maxOf(left.size, right.size)) {
            val order = left.getOrElse(index) { 0 }.compareTo(right.getOrElse(index) { 0 })
            if (order != 0) return order
        }
        return 0
    }
}
