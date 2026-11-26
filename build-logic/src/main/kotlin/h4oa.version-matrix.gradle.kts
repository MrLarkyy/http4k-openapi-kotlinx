import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.AttributeContainer

plugins {
    id("h4oa.kotlin-jvm")
}

val catalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
val sourceSets = extensions.getByType<SourceSetContainer>()
val testRuntime = configurations.getByName("testRuntimeClasspath")

fun AttributeContainer.copyFrom(source: AttributeContainer) {
    for (key in source.keySet()) {
        @Suppress("UNCHECKED_CAST")
        attribute(key as Attribute<Any>, requireNotNull(source.getAttribute(key)))
    }
}

tasks.named<Test>("test") {
    systemProperty("h4oa.http4k.version", catalog.findVersion("http4k-lowest").get().requiredVersion)
}

for (entry in listOf("current", "newest")) {
    val http4kVersion = catalog.findVersion("http4k-$entry").get().requiredVersion
    val serializationVersion = catalog.findVersion("kotlinx-serialization-$entry").get().requiredVersion
    val title = entry.replaceFirstChar { it.uppercase() }

    val matrixClasspath =
        configurations.create("matrix${title}RuntimeClasspath") {
            isCanBeConsumed = false
            isCanBeResolved = true
            extendsFrom(testRuntime)
            attributes { copyFrom(testRuntime.attributes) }
            resolutionStrategy.eachDependency {
                when {
                    requested.group == "org.http4k" -> {
                        useVersion(http4kVersion)
                    }

                    requested.group == "org.jetbrains.kotlinx" && requested.name.startsWith("kotlinx-serialization-") -> {
                        useVersion(serializationVersion)
                    }
                }
            }
        }

    val matrixTest =
        tasks.register<Test>("test$title") {
            description = "Runs the tests against http4k $http4kVersion and kotlinx.serialization $serializationVersion."
            group = "verification"
            systemProperty("h4oa.http4k.version", http4kVersion)
            testClassesDirs = sourceSets.getByName("test").output.classesDirs
            classpath = sourceSets.getByName("test").output + sourceSets.getByName("main").output + matrixClasspath
        }

    tasks.named("check") { dependsOn(matrixTest) }
}
