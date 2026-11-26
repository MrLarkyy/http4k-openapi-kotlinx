plugins {
    base
    // Puts the Kotlin plugin on the root classpath, which the ktlint plugin needs for its extension types.
    id("h4oa.kotlin-jvm") apply false
    alias(libs.plugins.ktlint)
}

ktlint {
    version.set(
        libs.versions.ktlint
            .asProvider()
            .get(),
    )
}

// Task paths into an included build do not resolve from the root, so the reference goes through the included build.
tasks.named("check") {
    dependsOn(gradle.includedBuild("build-logic").task(":check"))
}
