plugins {
    base
    alias(libs.plugins.bcv)
}

apiValidation {
    ignoredProjects += "tests"
}

// Task paths into an included build do not resolve from the root, so the reference goes through the included build.
tasks.named("check") {
    dependsOn(gradle.includedBuild("build-logic").task(":check"))
}
