plugins {
    id("h4oa.kotlin-library")
}

h4oaLibrary {
    artifactId.set("http4k-openapi-kotlinx")
}

dependencies {
    api(project(":core"))
    api(libs.http4k.api.openapi)
    api(libs.http4k.format.kotlinx.serialization)
}
