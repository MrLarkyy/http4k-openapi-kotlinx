plugins {
    id("h4oa.kotlin-library")
    id("org.jetbrains.kotlin.plugin.serialization")
}

h4oaLibrary {
    artifactId.set("http4k-openapi-kotlinx")
}

dependencies {
    api(project(":core"))
    api(libs.http4k.api.openapi)
    api(libs.http4k.format.kotlinx.serialization)
}

dependencies {
    testImplementation(libs.asm)
}

tasks.withType<Test>().configureEach {
    val mainOutput = sourceSets.main.get().output
    val mainClasses = mainOutput.classesDirs
    inputs
        .files(mainClasses)
        .withPathSensitivity(PathSensitivity.RELATIVE)
    jvmArgumentProviders.add(
        CommandLineArgumentProvider {
            listOf("-Dh4oa.linkage.classes=" + mainClasses.files.joinToString(File.pathSeparator) { it.absolutePath })
        },
    )
}
