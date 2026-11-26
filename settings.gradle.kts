pluginManagement {
    includeBuild("build-logic")
}

plugins {
    // The version catalog is not available in settings plugins, so the version is literal here.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
    }
}

rootProject.name = "http4k-openapi-kotlinx"
