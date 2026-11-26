import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    kotlin("jvm")
    id("org.jlleitschuh.gradle.ktlint")
}

val catalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
val jdk = catalog.findVersion("jdk").get().requiredVersion
val kotlinApi = KotlinVersion.fromVersion(catalog.findVersion("kotlin-api").get().requiredVersion)

kotlin {
    jvmToolchain(jdk.toInt())
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(jdk))
        apiVersion.set(kotlinApi)
        languageVersion.set(kotlinApi)
        allWarningsAsErrors.set(true)
    }
}

ktlint {
    version.set(catalog.findVersion("ktlint").get().requiredVersion)
    ignoreFailures.set(false)
    reporters {
        reporter(ReporterType.PLAIN)
        reporter(ReporterType.CHECKSTYLE)
    }
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation(platform(catalog.findLibrary("junit-bom").get()))
    testRuntimeOnly(catalog.findLibrary("junit-platform-launcher").get())
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform {
        excludeTags("integration", "benchmark")
    }
}

configurations.configureEach {
    resolutionStrategy {
        failOnDynamicVersions()
        failOnChangingVersions()
    }
}
