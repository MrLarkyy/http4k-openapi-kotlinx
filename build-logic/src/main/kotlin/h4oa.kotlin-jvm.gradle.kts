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
    testImplementation(platform("org.junit:junit-bom:${catalog.findVersion("junit").get().requiredVersion}"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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
