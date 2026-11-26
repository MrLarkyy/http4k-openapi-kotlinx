package io.github.mrlarkyy.http4k.openapi.http4k.internal

/** Schema of one definition, free of http4k and kotlinx.serialization types. */
internal sealed interface SchemaModel {
    /** An object with [properties] in declaration order; every name in [required] is a key of [properties]. */
    data class Object(
        val properties: Map<String, SchemaModel> = emptyMap(),
        val required: List<String> = emptyList(),
    ) : SchemaModel

    /** A JSON primitive of the OpenAPI [type] and an optional [format]. */
    data class Primitive(
        val type: String,
        val format: String? = null,
    ) : SchemaModel

    /** A reference to the definition called [name]. */
    data class Ref(
        val name: String,
    ) : SchemaModel
}
