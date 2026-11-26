plugins {
    base
    // BCV 0.18.2 (the newest) calls Configuration.setVisible, which makes the build report Gradle 10 incompatibility.
    // Known deprecation; migration to the Kotlin plugin's built-in ABI validation is tracked.
    alias(libs.plugins.bcv)
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

apiValidation {
    ignoredProjects += "tests"
}

// Task paths into an included build do not resolve from the root, so the reference goes through the included build.
tasks.named("check") {
    dependsOn(gradle.includedBuild("build-logic").task(":check"))
}
