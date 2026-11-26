package io.github.mrlarkyy.http4k.openapi.build

import org.gradle.api.provider.Property

/** Publication settings a library module sets in its own build script. */
abstract class LibraryPublishingExtension {
    /** Maven artifact id; set explicitly so it stays stable when a module moves. */
    abstract val artifactId: Property<String>
}
