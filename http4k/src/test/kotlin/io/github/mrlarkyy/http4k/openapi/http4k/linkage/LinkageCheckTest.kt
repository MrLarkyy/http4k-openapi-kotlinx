package io.github.mrlarkyy.http4k.openapi.http4k.linkage

import io.github.mrlarkyy.http4k.openapi.http4k.linkage.MemberReference.Kind
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
}
