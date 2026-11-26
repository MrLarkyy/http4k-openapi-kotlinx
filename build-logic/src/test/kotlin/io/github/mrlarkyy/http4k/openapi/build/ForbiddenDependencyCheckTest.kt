package io.github.mrlarkyy.http4k.openapi.build

import org.gradle.testkit.runner.GradleRunner
import java.io.File
import java.nio.file.Files
import java.util.jar.JarOutputStream
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertTrue

class ForbiddenDependencyCheckTest {
    @Test
    fun `fails and names the coordinates when a forbidden module is on the classpath`() {
        val project = newProject(withForbidden = true)

        val result = runner(project).buildAndFail()

        assertContains(result.output, "core must not depend on org.http4k: org.http4k:fake:1.0 in compileClasspath")
        assertContains(result.output, "org.http4k:fake:1.0 in runtimeClasspath")
    }

    @Test
    fun `passes when no forbidden module is present`() {
        val project = newProject(withForbidden = false)

        val result = runner(project).build()

        assertTrue(File(project, "build/h4oa/ok.txt").isFile, result.output)
    }

    private fun runner(project: File) =
        GradleRunner
            .create()
            .withProjectDir(project)
            .withArguments("checkForbidden", "--stacktrace")

    private fun newProject(withForbidden: Boolean): File {
        val root = Files.createTempDirectory("forbidden-check").toFile().also { it.deleteOnExit() }
        val repo = File(root, "repo")
        publishFake(repo)
        File(root, "settings.gradle").writeText("rootProject.name = 'core'\n")
        val dependency = if (withForbidden) "implementation 'org.http4k:fake:1.0'" else ""
        File(root, "build.gradle").writeText(
            """
            buildscript { dependencies { classpath files('${taskClassesLocation()}') } }
            plugins { id 'java' }
            repositories { maven { url = uri('${repo.invariantSeparatorsPath}') } }
            dependencies { $dependency }
            tasks.register('checkForbidden', io.github.mrlarkyy.http4k.openapi.build.ForbiddenDependencyCheck) {
                subject = project.name
                forbiddenGroups.add('org.http4k')
                marker = layout.buildDirectory.file('h4oa/ok.txt')
                ['compileClasspath', 'runtimeClasspath'].each { inspect(configurations.getByName(it)) }
            }
            """.trimIndent(),
        )
        return root
    }

    private fun taskClassesLocation(): String {
        val location = ForbiddenDependencyCheck::class.java.protectionDomain.codeSource.location
        return File(location.toURI()).invariantSeparatorsPath
    }

    private fun publishFake(repo: File) {
        val dir = File(repo, "org/http4k/fake/1.0").apply { mkdirs() }
        JarOutputStream(File(dir, "fake-1.0.jar").outputStream()).close()
        File(dir, "fake-1.0.pom").writeText(
            """
            <project xmlns="http://maven.apache.org/POM/4.0.0">
              <modelVersion>4.0.0</modelVersion>
              <groupId>org.http4k</groupId>
              <artifactId>fake</artifactId>
              <version>1.0</version>
            </project>
            """.trimIndent(),
        )
    }
}
