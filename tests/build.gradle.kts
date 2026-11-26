plugins {
    id("h4oa.kotlin-jvm")
    id("h4oa.version-matrix")
}

dependencies {
    testImplementation(project(":core"))
    testImplementation(project(":http4k"))
    testImplementation(libs.kotlinx.serialization.json)
}
