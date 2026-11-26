package io.github.mrlarkyy.http4k.openapi.build

import org.gradle.testkit.runner.GradleRunner
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertTrue

class AbiValidationConventionTest {
    private lateinit var root: File

    @BeforeTest
    fun createProject() {
        root = Files.createTempDirectory("abi-validation").toFile()
        val catalog = File("../gradle/libs.versions.toml").canonicalFile.invariantSeparatorsPath
        File(root, "settings.gradle").writeText(
            """
            rootProject.name = 'probe'
            dependencyResolutionManagement {
                repositories { mavenCentral() }
                versionCatalogs { create('libs') { from(files('$catalog')) } }
            }
            """.trimIndent(),
        )
        File(root, "build.gradle").writeText(
            """
            plugins { id 'h4oa.kotlin-library' }
            h4oaLibrary { artifactId = 'probe' }
            """.trimIndent(),
        )
        File(root, "api").mkdirs()
        File(root, "api/probe.api").writeText("")
        val sources = File(root, "src/main/kotlin").apply { mkdirs() }
        File(sources, "Probe.kt").writeText("public fun probe(): Int = 1\n")
    }

    @AfterTest
    fun deleteProject() {
        root.deleteRecursively()
    }

    @Test
    fun `check depends on the API check and the Kotlin ABI check`() {
        val result = runner("check", "--dry-run").build()

        assertContains(result.output, ":apiCheck")
        assertContains(result.output, ":checkKotlinAbi")
    }

    @Test
    fun `API check fails for a public declaration missing from the dump`() {
        val failure = runner("apiCheck").buildAndFail()

        assertContains(failure.output, ":checkKotlinAbi")
        assertContains(failure.output, "probe")
    }

    @Test
    fun `API check passes after the dump is updated`() {
        runner("apiDump").build()
        val dump = File(root, "api/probe.api")

        val result = runner("apiCheck").build()

        assertTrue(dump.readText().contains("probe"), dump.readText())
        assertContains(result.output, "BUILD SUCCESSFUL")
    }

    private fun runner(vararg arguments: String) =
        GradleRunner
            .create()
            .withProjectDir(root)
            .withPluginClasspath()
            .withArguments(*arguments, "--stacktrace")
}
