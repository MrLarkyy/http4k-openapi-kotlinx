plugins {
    id("h4oa.kotlin-library")
    id("h4oa.http4k-free")
}

h4oaLibrary {
    artifactId.set("http4k-openapi-kotlinx-core")
}

dependencies {
    api(libs.kotlinx.serialization.core)
}
