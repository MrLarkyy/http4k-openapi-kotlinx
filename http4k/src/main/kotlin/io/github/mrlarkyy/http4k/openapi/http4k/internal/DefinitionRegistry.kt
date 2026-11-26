package io.github.mrlarkyy.http4k.openapi.http4k.internal

import kotlinx.serialization.descriptors.SerialDescriptor
import java.util.concurrent.ConcurrentHashMap

/**
 * Remembers which descriptor owns each schema name of one renderer, so two types can never share a name.
 *
 * Safe for concurrent use without locking: the first descriptor registered under a name wins and every other thread
 * compares against it.
 */
internal class DefinitionRegistry(
    private val maxDefinitions: Int = DescriptorWalker.MAX_DEFINITIONS,
) {
    private val owners = ConcurrentHashMap<String, SerialDescriptor>()

    /** Registers [descriptor] under [name]; fails when another descriptor owns [name] or the registry is full. */
    fun register(
        name: String,
        descriptor: SerialDescriptor,
    ) {
        val owner = owners[name] ?: addOrGetOwner(name, descriptor)
        check(owner == null || owner == descriptor) {
            "Schema name '$name' is used by two descriptors: '${SchemaNames.shorten(owner?.serialName.orEmpty())}' and " +
                "'${SchemaNames.shorten(descriptor.serialName)}'"
        }
    }

    private fun addOrGetOwner(
        name: String,
        descriptor: SerialDescriptor,
    ): SerialDescriptor? {
        check(owners.size < maxDefinitions) {
            "${SchemaNames.shorten(descriptor.serialName)}: more than $maxDefinitions schema definitions are not supported"
        }
        return owners.putIfAbsent(name, descriptor)
    }
}
