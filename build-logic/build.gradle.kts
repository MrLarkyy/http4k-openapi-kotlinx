plugins {
    `kotlin-dsl`
    alias(libs.plugins.ktlint)
}

ktlint {
    version.set(
        libs.versions.ktlint
            .asProvider()
            .get(),
    )

    // kotlin-dsl generates accessors and plugin classes under build/ that do not follow the style.
    filter {
        exclude { element -> element.file.toString().contains("generated-sources") }
    }
}

dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.ktlint.gradle.plugin)

    testImplementation(gradleTestKit())
    testImplementation(kotlin("test"))
    testRuntimeOnly(platform(libs.junit.bom))
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}
