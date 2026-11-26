package io.github.mrlarkyy.http4k.openapi.http4k.internal

import kotlinx.serialization.descriptors.SerialDescriptor

/**
 * The result of walking one root descriptor: the [root] definition name and every reachable definition, sorted by name.
 */
internal data class WalkedSchema(
    val root: String,
    val definitions: Map<String, Definition>,
) {
    /** One named schema together with the descriptor it came from. */
    data class Definition(
        val descriptor: SerialDescriptor,
        val model: SchemaModel.Object,
    )
}
