import io.github.mrlarkyy.http4k.openapi.build.LibraryPublishingExtension

plugins {
    id("h4oa.kotlin-jvm")
    id("h4oa.version-matrix")
    `maven-publish`
    signing
}

val catalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
val library = extensions.create<LibraryPublishingExtension>("h4oaLibrary")

kotlin {
    explicitApi()
    coreLibrariesVersion = catalog.findVersion("kotlin-stdlib-lowest").get().requiredVersion
}

java {
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            pom {
                description.set("OpenAPI documents for http4k contracts, generated from kotlinx.serialization descriptors.")
                url.set("https://github.com/MrLarkyy/http4k-openapi-kotlinx")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                scm {
                    url.set("https://github.com/MrLarkyy/http4k-openapi-kotlinx")
                    connection.set("scm:git:https://github.com/MrLarkyy/http4k-openapi-kotlinx.git")
                }
                developers {
                    developer {
                        id.set("MrLarkyy")
                        name.set("MrLarkyy")
                    }
                }
            }
        }
    }
}

// The module script sets the extension after this plugin is applied, so the artifact id is read once it is evaluated.
afterEvaluate {
    publishing.publications.named<MavenPublication>("maven") {
        artifactId = library.artifactId.get()
        pom.name.set(library.artifactId)
    }
}

signing {
    val key = providers.gradleProperty("signingInMemoryKey")
    isRequired = key.isPresent
    if (key.isPresent) {
        useInMemoryPgpKeys(key.get(), providers.gradleProperty("signingInMemoryKeyPassword").orNull)
        sign(publishing.publications)
    }
}
