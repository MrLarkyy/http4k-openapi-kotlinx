package io.github.mrlarkyy.http4k.openapi.http4k.internal

import kotlinx.serialization.json.JsonElement
import java.util.concurrent.ConcurrentHashMap

/**
 * Remembers which schema owns each schema name of one renderer, so two different schemas can never share a name.
 *
 * A schema is identified by its source (a serial name, or [DELEGATED] for schemas that http4k rendered) and its JSON,
 * because kotlinx descriptors of two types with one serial name compare equal even when their elements differ.
 *
 * Safe for concurrent use without locking: the first schema registered under a name wins and every other thread
 * compares against it.
 */
internal class DefinitionRegistry(
    private val maxDefinitions: Int = DescriptorWalker.MAX_DEFINITIONS,
) {
    private data class Owner(
        val source: String,
        val schema: JsonElement,
    ) {
        /** Schemas that http4k rendered may share a name with different JSON: http4k itself keeps the last one. */
        fun sameAs(other: Owner): Boolean = source == other.source && (source == DELEGATED || schema == other.schema)
    }

    private val owners = ConcurrentHashMap<String, Owner>()

    /**
     * Registers the [schema] that [source] renders under [name]; fails when a different schema owns [name] or the
     * registry is full.
     */
    fun register(
        name: String,
        source: String,
        schema: JsonElement,
    ) {
        val candidate = Owner(source, schema)
        val owner = owners[name] ?: addOrGetOwner(name, candidate)
        check(owner == null || owner.sameAs(candidate)) {
            "Schema name '${SchemaNames.shorten(name)}' is used by two different schemas: " +
                "'${SchemaNames.shorten(owner?.source.orEmpty())}' and '${SchemaNames.shorten(source)}'"
        }
    }

    private fun addOrGetOwner(
        name: String,
        candidate: Owner,
    ): Owner? {
        check(owners.size < maxDefinitions) {
            "${SchemaNames.shorten(candidate.source)}: more than $maxDefinitions schema definitions are not supported"
        }
        return owners.putIfAbsent(name, candidate)
    }

    internal companion object {
        /** The source of schemas that http4k rendered instead of this library. */
        const val DELEGATED = "http4k"
    }
}
