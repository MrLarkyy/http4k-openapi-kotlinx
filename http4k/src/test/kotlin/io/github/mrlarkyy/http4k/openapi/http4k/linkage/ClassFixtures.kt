package io.github.mrlarkyy.http4k.openapi.http4k.linkage

import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/** Builds small class files with ASM and loads them, so linkage checks need no real library. */
internal object ClassFixtures {
    /** A class file named [name]; [members] adds methods through [method] and [abstractMethod]. */
    fun define(
        name: String,
        access: Int = Opcodes.ACC_PUBLIC,
        superName: String = "java/lang/Object",
        interfaces: List<String> = emptyList(),
        members: ClassWriter.() -> Unit = {},
    ): ByteArray {
        val writer = ClassWriter(ClassWriter.COMPUTE_MAXS)
        writer.visit(Opcodes.V17, access, name, null, superName, interfaces.toTypedArray())
        writer.members()
        writer.visitEnd()
        return writer.toByteArray()
    }

    /** An interface file named [name]. */
    fun defineInterface(
        name: String,
        members: ClassWriter.() -> Unit = {},
    ): ByteArray = define(name, Opcodes.ACC_PUBLIC or Opcodes.ACC_INTERFACE or Opcodes.ACC_ABSTRACT, members = members)

    /** Adds an abstract method. */
    fun ClassWriter.abstractMethod(
        name: String,
        descriptor: String,
    ) {
        visitMethod(Opcodes.ACC_PUBLIC or Opcodes.ACC_ABSTRACT, name, descriptor, null, null).visitEnd()
    }

    /** Adds a method whose body [code] writes, ending with a return. */
    fun ClassWriter.method(
        name: String,
        descriptor: String = "()V",
        exceptions: List<String> = emptyList(),
        code: MethodVisitor.() -> Unit = {},
    ) {
        val visitor = visitMethod(Opcodes.ACC_PUBLIC, name, descriptor, null, exceptions.toTypedArray().ifEmpty { null })
        visitor.visitCode()
        visitor.code()
        visitor.visitInsn(Opcodes.RETURN)
        visitor.visitMaxs(0, 0)
        visitor.visitEnd()
    }

    /** A class loader that defines [classes] by internal name and delegates the rest to the test class loader. */
    fun loader(classes: Map<String, ByteArray>): ClassLoader =
        object : ClassLoader(ClassFixtures::class.java.classLoader) {
            override fun findClass(name: String): Class<*> {
                val bytes = classes[name.replace('.', '/')] ?: throw ClassNotFoundException(name)
                return defineClass(name, bytes, 0, bytes.size)
            }
        }
}
