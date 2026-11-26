package io.github.mrlarkyy.http4k.openapi.http4k.linkage

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Handle
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Checks that the members a compiled class refers to exist in the classes of the running classpath.
 * Resolution is by owner, name and exact descriptor, as the JVM links them; access flags are not checked.
 */
internal object LinkageCheck {
    private const val ASM_API = Opcodes.ASM9

    /** Member references in [classBytes] whose owner internal name starts with one of [ownerPrefixes]. */
    fun references(
        classBytes: ByteArray,
        ownerPrefixes: List<String>,
    ): Set<MemberReference> {
        val found = LinkedHashSet<MemberReference>()

        fun add(
            owner: String,
            name: String,
            descriptor: String,
            kind: MemberReference.Kind,
        ) {
            if (ownerPrefixes.any { owner.startsWith(it) }) {
                found += MemberReference(owner, name, descriptor, kind)
            }
        }

        fun addHandle(handle: Handle) {
            val kind =
                when (handle.tag) {
                    Opcodes.H_GETFIELD, Opcodes.H_GETSTATIC, Opcodes.H_PUTFIELD, Opcodes.H_PUTSTATIC -> MemberReference.Kind.FIELD
                    Opcodes.H_NEWINVOKESPECIAL -> MemberReference.Kind.CONSTRUCTOR
                    else -> MemberReference.Kind.METHOD
                }
            add(handle.owner, handle.name, handle.desc, kind)
        }

        val methodVisitor =
            object : MethodVisitor(ASM_API) {
                override fun visitFieldInsn(
                    opcode: Int,
                    owner: String,
                    name: String,
                    descriptor: String,
                ) = add(owner, name, descriptor, MemberReference.Kind.FIELD)

                override fun visitMethodInsn(
                    opcode: Int,
                    owner: String,
                    name: String,
                    descriptor: String,
                    isInterface: Boolean,
                ) = add(
                    owner,
                    name,
                    descriptor,
                    if (name == "<init>") MemberReference.Kind.CONSTRUCTOR else MemberReference.Kind.METHOD,
                )

                override fun visitInvokeDynamicInsn(
                    name: String,
                    descriptor: String,
                    bootstrapMethodHandle: Handle,
                    vararg bootstrapMethodArguments: Any?,
                ) {
                    addHandle(bootstrapMethodHandle)
                    bootstrapMethodArguments.filterIsInstance<Handle>().forEach(::addHandle)
                }
            }

        ClassReader(classBytes).accept(
            object : ClassVisitor(ASM_API) {
                override fun visitMethod(
                    access: Int,
                    name: String,
                    descriptor: String,
                    signature: String?,
                    exceptions: Array<out String>?,
                ): MethodVisitor = methodVisitor
            },
            0,
        )
        return found
    }

    /** The references in [references] that [classLoader] cannot resolve; a missing class is reported once. */
    fun missing(
        references: Collection<MemberReference>,
        classLoader: ClassLoader,
    ): List<String> {
        val result = ArrayList<String>()
        val reportedClasses = HashSet<String>()
        val loaded = HashMap<String, Class<*>?>()
        for (reference in references) {
            val type =
                loaded.getOrPut(reference.owner) {
                    try {
                        Class.forName(reference.owner.replace('/', '.'), false, classLoader)
                    } catch (_: ClassNotFoundException) {
                        null
                    } catch (_: LinkageError) {
                        null
                    }
                }
            when {
                type == null -> if (reportedClasses.add(reference.owner)) result += "class ${reference.owner}"
                !resolves(type, reference) -> result += reference.toString()
            }
        }
        return result
    }

    private fun resolves(
        type: Class<*>,
        reference: MemberReference,
    ): Boolean =
        when (reference.kind) {
            MemberReference.Kind.CONSTRUCTOR -> {
                type.declaredConstructors.any { methodDescriptor(it.parameterTypes, Void.TYPE) == reference.descriptor }
            }

            MemberReference.Kind.METHOD -> {
                hierarchy(type, interfacesFirst = false).any { owner ->
                    owner.declaredMethods.any {
                        it.name == reference.name && methodDescriptor(it.parameterTypes, it.returnType) == reference.descriptor
                    }
                }
            }

            MemberReference.Kind.FIELD -> {
                hierarchy(type, interfacesFirst = true).any { owner ->
                    owner.declaredFields.any { it.name == reference.name && it.type.descriptor() == reference.descriptor }
                }
            }
        }

    private fun hierarchy(
        type: Class<*>,
        interfacesFirst: Boolean,
    ): Sequence<Class<*>> =
        sequence {
            val classes = generateSequence(type) { it.superclass }.toList()
            val interfaces = LinkedHashSet<Class<*>>()

            fun collect(candidate: Class<*>) {
                for (parent in candidate.interfaces) {
                    if (interfaces.add(parent)) collect(parent)
                }
            }
            classes.forEach(::collect)
            val extra = if (type.isInterface) listOf<Class<*>>(Any::class.java) else emptyList()
            if (interfacesFirst) {
                yieldAll(interfaces)
                yieldAll(classes)
            } else {
                yieldAll(classes)
                yieldAll(interfaces)
            }
            yieldAll(extra)
        }

    private fun methodDescriptor(
        parameters: Array<Class<*>>,
        returnType: Class<*>,
    ): String = parameters.joinToString("", "(", ")") { it.descriptor() } + returnType.descriptor()

    private fun Class<*>.descriptor(): String =
        when {
            this == Void.TYPE -> "V"
            this == Integer.TYPE -> "I"
            this == java.lang.Long.TYPE -> "J"
            this == java.lang.Boolean.TYPE -> "Z"
            this == java.lang.Byte.TYPE -> "B"
            this == java.lang.Short.TYPE -> "S"
            this == java.lang.Character.TYPE -> "C"
            this == java.lang.Float.TYPE -> "F"
            this == java.lang.Double.TYPE -> "D"
            isArray -> name.replace('.', '/')
            else -> "L${name.replace('.', '/')};"
        }
}
