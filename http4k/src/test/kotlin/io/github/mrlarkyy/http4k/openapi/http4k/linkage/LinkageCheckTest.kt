package io.github.mrlarkyy.http4k.openapi.http4k.linkage

import io.github.mrlarkyy.http4k.openapi.http4k.linkage.ClassFixtures.abstractMethod
import io.github.mrlarkyy.http4k.openapi.http4k.linkage.ClassFixtures.method
import io.github.mrlarkyy.http4k.openapi.http4k.linkage.MemberReference.Kind
import org.objectweb.asm.Handle
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LinkageCheckTest {
    private val classLoader = LinkageCheckTest::class.java.classLoader

    private fun missing(vararg references: MemberReference) = LinkageCheck.missing(references.toList(), classLoader)

    @Test
    fun `existing method resolves and absent method is reported`() {
        val present = MemberReference("java/lang/String", "length", "()I", Kind.METHOD)
        val absent = MemberReference("java/lang/String", "nope", "()V", Kind.METHOD)

        assertEquals(listOf(absent.toString()), missing(present, absent))
    }

    @Test
    fun `missing class is reported once for several members`() {
        val first = MemberReference("java/lang/Nope", "a", "()V", Kind.METHOD)
        val second = MemberReference("java/lang/Nope", "b", "()V", Kind.METHOD)

        assertEquals(listOf("class java/lang/Nope"), missing(first, second))
    }

    @Test
    fun `methods inherited from a superclass and an interface resolve`() {
        val fromSuperclass = MemberReference("java/util/ArrayList", "hashCode", "()I", Kind.METHOD)
        val fromInterface = MemberReference("java/util/ArrayList", "forEach", "(Ljava/util/function/Consumer;)V", Kind.METHOD)

        assertTrue(missing(fromSuperclass, fromInterface).isEmpty())
    }

    @Test
    fun `constructors resolve by exact descriptor`() {
        val present = MemberReference("java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", Kind.CONSTRUCTOR)
        val absent = MemberReference("java/lang/StringBuilder", "<init>", "(Ljava/util/UUID;)V", Kind.CONSTRUCTOR)

        assertEquals(listOf(absent.toString()), missing(present, absent))
    }

    @Test
    fun `fields resolve by name and descriptor`() {
        val present = MemberReference("java/lang/System", "out", "Ljava/io/PrintStream;", Kind.FIELD)
        val wrongDescriptor = MemberReference("java/lang/System", "out", "Ljava/lang/String;", Kind.FIELD)

        assertEquals(listOf(wrongDescriptor.toString()), missing(present, wrongDescriptor))
    }

    @Test
    fun `references keep only owners with a wanted prefix`() {
        val bytes =
            assertNotNull(
                classLoader.getResourceAsStream("io/github/mrlarkyy/http4k/openapi/http4k/linkage/LegacyApiInfoCaller.class"),
            ).use { it.readBytes() }

        val references = LinkageCheck.references(bytes, listOf("org/http4k/"))

        assertTrue(references.isNotEmpty())
        assertTrue(references.all { it.owner.startsWith("org/http4k/") })
        assertTrue(references.any { it.owner == "org/http4k/contract/openapi/ApiInfo" && it.kind == Kind.CONSTRUCTOR })
    }

    @Test
    fun `field instructions are collected`() {
        val bytes =
            ClassFixtures.define("t/Subject") {
                method("m") {
                    visitFieldInsn(Opcodes.GETSTATIC, "p/Lib", "f", "I")
                    visitFieldInsn(Opcodes.GETSTATIC, "q/Other", "g", "I")
                }
            }

        assertEquals(setOf(MemberReference("p/Lib", "f", "I", Kind.FIELD)), LinkageCheck.references(bytes, listOf("p/")))
    }

    @Test
    fun `invokedynamic handles are collected by kind`() {
        val bootstrap = Handle(Opcodes.H_INVOKESTATIC, "p/Boot", "bootstrap", "()V", false)
        val bytes =
            ClassFixtures.define("t/Subject") {
                method("m") {
                    visitInvokeDynamicInsn(
                        "run",
                        "()V",
                        bootstrap,
                        Handle(Opcodes.H_GETSTATIC, "p/Lib", "f", "I", false),
                        Handle(Opcodes.H_NEWINVOKESPECIAL, "p/Lib", "<init>", "()V", false),
                        Handle(Opcodes.H_INVOKEVIRTUAL, "p/Lib", "call", "()V", false),
                        Type.getMethodType("(Lp/Arg;)V"),
                    )
                }
            }

        val references = LinkageCheck.references(bytes, listOf("p/"))

        assertEquals(
            setOf(
                MemberReference("p/Boot", "bootstrap", "()V", Kind.METHOD),
                MemberReference("p/Lib", "f", "I", Kind.FIELD),
                MemberReference("p/Lib", "<init>", "()V", Kind.CONSTRUCTOR),
                MemberReference("p/Lib", "call", "()V", Kind.METHOD),
                MemberReference("p/Arg", "", "", Kind.CLASS),
            ),
            references,
        )
    }

    @Test
    fun `a lambda for a library interface records the interface method it implements`() {
        val metafactory =
            Handle(
                Opcodes.H_INVOKESTATIC,
                "java/lang/invoke/LambdaMetafactory",
                "metafactory",
                "(Ljava/lang/invoke/MethodHandles\$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;" +
                    "Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)" +
                    "Ljava/lang/invoke/CallSite;",
                false,
            )

        fun lambdaCaller(
            name: String,
            descriptor: String,
        ) = ClassFixtures.define("t/Subject") {
            method("m") {
                visitInvokeDynamicInsn(
                    name,
                    "()Ljava/lang/Runnable;",
                    metafactory,
                    Type.getMethodType(descriptor),
                    Handle(Opcodes.H_INVOKESTATIC, "t/Subject", "body", "()V", false),
                    Type.getMethodType(descriptor),
                )
            }
        }

        val present = LinkageCheck.references(lambdaCaller("run", "()V"), listOf("java/lang/Runnable"))
        val absent = LinkageCheck.references(lambdaCaller("nope", "()V"), listOf("java/lang/Runnable"))

        assertEquals(
            setOf(
                MemberReference("java/lang/Runnable", "", "", Kind.CLASS),
                MemberReference("java/lang/Runnable", "run", "()V", Kind.METHOD),
            ),
            present,
        )
        assertEquals(listOf("method java/lang/Runnable.nope()V"), missing(*absent.toTypedArray()))
        assertTrue(missing(*present.toTypedArray()).isEmpty())
    }

    @Test
    fun `class references from supertypes, signatures and instructions are collected`() {
        val bytes =
            ClassFixtures.define("t/Subject", superName = "p/Base", interfaces = listOf("p/Iface")) {
                visitField(Opcodes.ACC_PUBLIC, "field", "[Lp/FieldType;", null, null).visitEnd()
                method("m", "(Lp/Param;)Lp/Ret;", exceptions = listOf("p/Boom")) {
                    val start = Label()
                    val end = Label()
                    val handler = Label()
                    visitTryCatchBlock(start, end, handler, "p/Caught")
                    visitLabel(start)
                    visitTypeInsn(Opcodes.NEW, "p/New")
                    visitTypeInsn(Opcodes.ANEWARRAY, "p/Arr")
                    visitTypeInsn(Opcodes.CHECKCAST, "p/Cast")
                    visitTypeInsn(Opcodes.INSTANCEOF, "p/Inst")
                    visitLdcInsn(Type.getObjectType("p/Lit"))
                    visitMultiANewArrayInsn("[[Lp/Multi;", 2)
                    visitLabel(end)
                    visitLabel(handler)
                }
            }

        val references = LinkageCheck.references(bytes, listOf("p/"))

        assertTrue(references.all { it.kind == Kind.CLASS })
        assertEquals(
            setOf(
                "p/Base",
                "p/Iface",
                "p/FieldType",
                "p/Param",
                "p/Ret",
                "p/Boom",
                "p/Caught",
                "p/New",
                "p/Arr",
                "p/Cast",
                "p/Inst",
                "p/Lit",
                "p/Multi",
            ),
            references.map { it.owner }.toSet(),
        )
    }

    @Test
    fun `interface methods resolve against Object`() {
        val fromObject = MemberReference("java/lang/Runnable", "hashCode", "()I", Kind.METHOD)

        assertTrue(missing(fromObject).isEmpty())
    }

    @Test
    fun `a class that cannot be linked is reported as a missing class`() {
        val loader = ClassFixtures.loader(mapOf("t/Sub" to ClassFixtures.define("t/Sub", superName = "t/Gone")))

        val result = LinkageCheck.missing(listOf(MemberReference("t/Sub", "m", "()V", Kind.METHOD)), loader)

        assertEquals(listOf("class t/Sub"), result)
    }

    @Test
    fun `a member whose types cannot be resolved is reported as missing`() {
        val broken = ClassFixtures.define("t/Broken") { method("m", "(Lt/Gone;)V") }
        val loader = ClassFixtures.loader(mapOf("t/Broken" to broken))
        val reference = MemberReference("t/Broken", "m", "(Lt/Gone;)V", Kind.METHOD)

        assertEquals(listOf(reference.toString()), LinkageCheck.missing(listOf(reference), loader))
    }

    @Test
    fun `an abstract method of a library supertype that a class does not implement is reported`() {
        val api = ClassFixtures.defineInterface("p/Api") { abstractMethod("renamed", "()V") }
        val impl = ClassFixtures.define("t/Impl", interfaces = listOf("p/Api")) { method("run") }
        val loader = ClassFixtures.loader(mapOf("p/Api" to api, "t/Impl" to impl))

        assertEquals(
            listOf("abstract method p/Api.renamed()V is not implemented by t/Impl"),
            LinkageCheck.unimplemented("t/Impl", listOf("p/"), loader),
        )
    }

    @Test
    fun `an implemented abstract method, an abstract class and foreign supertypes are not reported`() {
        val api = ClassFixtures.defineInterface("p/Api") { abstractMethod("run", "()V") }
        val implemented = ClassFixtures.define("t/Impl", interfaces = listOf("p/Api")) { method("run") }
        val abstractImpl =
            ClassFixtures.define("t/Abs", Opcodes.ACC_PUBLIC or Opcodes.ACC_ABSTRACT, interfaces = listOf("p/Api"))
        val loader = ClassFixtures.loader(mapOf("p/Api" to api, "t/Impl" to implemented, "t/Abs" to abstractImpl))

        assertTrue(LinkageCheck.unimplemented("t/Impl", listOf("p/"), loader).isEmpty())
        assertTrue(LinkageCheck.unimplemented("t/Abs", listOf("p/"), loader).isEmpty())
        assertTrue(LinkageCheck.unimplemented("t/Impl", listOf("other/"), loader).isEmpty())
    }

    @Test
    fun `a class that cannot be loaded is reported by the abstract method check`() {
        val loader = ClassFixtures.loader(mapOf("t/Sub" to ClassFixtures.define("t/Sub", superName = "t/Gone")))

        val result = LinkageCheck.unimplemented("t/Sub", listOf("p/"), loader)

        assertEquals(1, result.size)
        assertTrue(result.single().startsWith("class t/Sub cannot be loaded"), result.single())
    }
}
