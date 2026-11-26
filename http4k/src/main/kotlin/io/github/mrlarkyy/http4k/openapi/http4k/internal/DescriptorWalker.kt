package io.github.mrlarkyy.http4k.openapi.http4k.internal

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind

/**
 * Walks a root descriptor into [SchemaModel] definitions.
 *
 * Supports classes and objects whose elements are non-null, non-optional strings, booleans, ints, longs, doubles or
 * further classes and objects. Anything else fails with an [IllegalArgumentException] naming the serial name and the
 * element. A type met again while it is being walked becomes a reference, so recursive types terminate.
 */
@OptIn(ExperimentalSerializationApi::class)
internal class DescriptorWalker(
    private val maxDepth: Int = MAX_DEPTH,
    private val maxDefinitions: Int = MAX_DEFINITIONS,
) {
    /** Walks [root], which must be a class or an object but not a value class, whose JSON is its single property. */
    fun walk(root: SerialDescriptor): WalkedSchema {
        require(!root.isInline) { "${SchemaNames.shorten(root.serialName)}: inline ${root.kind} not supported yet" }
        require(isObjectLike(root.kind)) { "${SchemaNames.shorten(root.serialName)}: ${root.kind} not supported yet" }
        val run = Run()
        val rootName = run.visit(root, 0)
        return WalkedSchema(rootName, run.definitions.toSortedMap())
    }

    private inner class Run {
        val definitions = LinkedHashMap<String, WalkedSchema.Definition>()
        private val seen = HashMap<String, SerialDescriptor>()

        fun visit(
            descriptor: SerialDescriptor,
            depth: Int,
        ): String {
            val name = SchemaNames.of(descriptor)
            val known = seen[name]
            if (known != null) {
                check(known == descriptor && sameElementNames(known, descriptor)) {
                    "Schema name '${SchemaNames.shorten(name)}' is used by two descriptors: " +
                        "'${SchemaNames.shorten(known.serialName)}' and '${SchemaNames.shorten(descriptor.serialName)}'"
                }
                return name
            }
            require(depth <= maxDepth) {
                "${SchemaNames.shorten(descriptor.serialName)}: nesting deeper than $maxDepth levels is not supported"
            }
            require(seen.size < maxDefinitions) {
                "${SchemaNames.shorten(descriptor.serialName)}: more than $maxDefinitions definitions are not supported"
            }
            seen[name] = descriptor
            definitions[name] = WalkedSchema.Definition(descriptor, objectModel(descriptor, depth))
            return name
        }

        private fun objectModel(
            descriptor: SerialDescriptor,
            depth: Int,
        ): SchemaModel.Object {
            val properties = LinkedHashMap<String, SchemaModel>(descriptor.elementsCount * 2)
            val required = ArrayList<String>(descriptor.elementsCount)
            for (index in 0 until descriptor.elementsCount) {
                val element = descriptor.getElementName(index)
                val child = descriptor.getElementDescriptor(index)
                unsupportedReason(descriptor, index, child)?.let { reason ->
                    throw IllegalArgumentException(
                        "${SchemaNames.shorten(descriptor.serialName)}.${SchemaNames.shorten(element)}: $reason not supported yet",
                    )
                }
                properties[element] = elementModel(child, depth)
                required += element
            }
            return SchemaModel.Object(properties, required)
        }

        private fun elementModel(
            child: SerialDescriptor,
            depth: Int,
        ): SchemaModel =
            when (child.kind) {
                PrimitiveKind.STRING -> SchemaModel.Primitive("string")
                PrimitiveKind.BOOLEAN -> SchemaModel.Primitive("boolean")
                PrimitiveKind.INT, PrimitiveKind.LONG -> SchemaModel.Primitive("integer")
                PrimitiveKind.DOUBLE -> SchemaModel.Primitive("number")
                else -> SchemaModel.Ref(visit(child, depth + 1))
            }
    }

    private fun unsupportedReason(
        parent: SerialDescriptor,
        index: Int,
        child: SerialDescriptor,
    ): String? =
        when {
            parent.isElementOptional(index) -> "optional ${child.kind}"
            child.isNullable -> "nullable ${child.kind}"
            child.isInline -> "inline ${child.kind}"
            child.kind in supportedPrimitives || isObjectLike(child.kind) -> null
            else -> child.kind.toString()
        }

    // Descriptor equality ignores element names, so two types with one serial name would otherwise be taken as one.
    private fun sameElementNames(
        first: SerialDescriptor,
        second: SerialDescriptor,
    ): Boolean = (0 until first.elementsCount).all { first.getElementName(it) == second.getElementName(it) }

    private fun isObjectLike(kind: SerialKind): Boolean = kind == StructureKind.CLASS || kind == StructureKind.OBJECT

    internal companion object {
        /** Deepest nesting of distinct classes before a descriptor is rejected. */
        const val MAX_DEPTH = 32

        /** Most definitions one walk, and one renderer, accepts. */
        const val MAX_DEFINITIONS = 4096

        private val supportedPrimitives: Set<SerialKind> =
            setOf(PrimitiveKind.STRING, PrimitiveKind.BOOLEAN, PrimitiveKind.INT, PrimitiveKind.LONG, PrimitiveKind.DOUBLE)
    }
}
