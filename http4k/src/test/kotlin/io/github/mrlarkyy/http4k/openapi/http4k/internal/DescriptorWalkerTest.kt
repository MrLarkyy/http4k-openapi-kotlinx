package io.github.mrlarkyy.http4k.openapi.http4k.internal

import io.github.mrlarkyy.http4k.openapi.http4k.internal.SchemaFixtures.Item
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DescriptorWalkerTest {
    private val walker = DescriptorWalker()

    @Test
    fun `primitives and nested classes keep declaration order`() {
        val walked = walker.walk(Item.serializer().descriptor)

        assertEquals("Item", walked.root)
        val item = walked.definitions.getValue("Item").model
        assertEquals(listOf("name", "count", "big", "price", "ok", "inner"), item.properties.keys.toList())
        assertEquals(
            mapOf(
                "name" to SchemaModel.Primitive("string"),
                "count" to SchemaModel.Primitive("integer"),
                "big" to SchemaModel.Primitive("integer"),
                "price" to SchemaModel.Primitive("number"),
                "ok" to SchemaModel.Primitive("boolean"),
                "inner" to SchemaModel.Ref("Inner"),
            ),
            item.properties,
        )
        assertEquals(item.properties.keys.toList(), item.required)
        assertEquals(
            SchemaModel.Object(mapOf("id" to SchemaModel.Primitive("string")), listOf("id")),
            walked.definitions.getValue("Inner").model,
        )
    }

    @Test
    fun `definitions are sorted by name and a second walk is equal`() {
        val first = walker.walk(SchemaFixtures.Wrapper.serializer().descriptor)
        val second = walker.walk(SchemaFixtures.Wrapper.serializer().descriptor)

        assertEquals(listOf("Inner", "Wrapper"), first.definitions.keys.toList())
        assertEquals(first, second)
    }

    @Test
    fun `an object without properties is an empty object schema`() {
        val walked = walker.walk(SchemaFixtures.Marker.serializer().descriptor)

        assertEquals(SchemaModel.Object(), walked.definitions.getValue("Marker").model)
    }

    @Test
    fun `a self reference terminates with a reference`() {
        val walked = walker.walk(SchemaFixtures.Recursive.serializer().descriptor)

        assertEquals(listOf("Recursive"), walked.definitions.keys.toList())
        val model = walked.definitions.getValue("Recursive").model
        assertEquals(SchemaModel.Ref("Recursive"), model.properties["self"])
    }

    @Test
    fun `unsupported kinds fail naming the serial name and the property`() {
        val cases: List<Triple<KSerializer<*>, String, String>> =
            listOf(
                Triple(SchemaFixtures.Nullable.serializer(), "Nullable.value", "nullable STRING"),
                Triple(SchemaFixtures.Optional.serializer(), "Optional.value", "optional STRING"),
                Triple(SchemaFixtures.Tree.serializer(), "Tree.child", "nullable CLASS"),
                Triple(SchemaFixtures.WithList.serializer(), "WithList.values", "LIST"),
                Triple(SchemaFixtures.WithMap.serializer(), "WithMap.values", "MAP"),
                Triple(SchemaFixtures.WithFloat.serializer(), "WithFloat.value", "FLOAT"),
                Triple(SchemaFixtures.WithEnum.serializer(), "WithEnum.color", "ENUM"),
                Triple(SchemaFixtures.WithInline.serializer(), "WithInline.id", "inline CLASS"),
                Triple(SchemaFixtures.WithSealed.serializer(), "WithSealed.shape", "SEALED"),
            )

        for ((serializer, location, kind) in cases) {
            val failure = assertFailsWith<IllegalArgumentException>(location) { walker.walk(serializer.descriptor) }
            assertTrue(failure.message.orEmpty().contains("$location: $kind not supported yet"), failure.message)
        }
    }

    @Test
    fun `a root that is not a class or object fails naming it`() {
        val failure = assertFailsWith<IllegalArgumentException> { walker.walk(ListSerializer(String.serializer()).descriptor) }

        assertTrue(failure.message.orEmpty().contains("not supported yet"), failure.message)
    }

    @Test
    fun `a value class root fails because its JSON is a primitive`() {
        val failure = assertFailsWith<IllegalArgumentException> { walker.walk(SchemaFixtures.Id.serializer().descriptor) }

        assertTrue(failure.message.orEmpty().contains("inline CLASS not supported yet"), failure.message)
    }

    @Test
    fun `two classes with one serial name but different properties fail`() {
        val failure = assertFailsWith<IllegalStateException> { walker.walk(SchemaFixtures.WrapsSame.serializer().descriptor) }

        assertTrue(failure.message.orEmpty().contains("Schema name 'Same' is used by two descriptors"), failure.message)
    }

    @Test
    fun `a long serial name is shortened in the message`() {
        val failure = assertFailsWith<IllegalArgumentException> { walker.walk(SchemaFixtures.LongNamed.serializer().descriptor) }

        val message = failure.message.orEmpty()
        assertTrue(message.contains("...") && message.length < 300, message)
    }

    @Test
    fun `nesting deeper than 32 levels fails and 32 levels still walk`() {
        assertEquals(33, walker.walk(chain(33)).definitions.size)

        val failure = assertFailsWith<IllegalArgumentException> { walker.walk(chain(34)) }
        assertTrue(failure.message.orEmpty().contains("deeper than 32"), failure.message)
    }

    @Test
    fun `more definitions than the limit fail`() {
        val failure = assertFailsWith<IllegalArgumentException> { DescriptorWalker(maxDefinitions = 1).walk(Item.serializer().descriptor) }

        assertTrue(failure.message.orEmpty().contains("more than 1 definitions"), failure.message)
    }

    @Test
    fun `two classes with one schema name fail naming both serial names`() {
        val holder =
            buildClassSerialDescriptor("Holder") {
                element("first", SchemaFixtures.DupFirst.serializer().descriptor)
                element("second", SchemaFixtures.DupSecond.serializer().descriptor)
            }

        val failure = assertFailsWith<IllegalStateException> { walker.walk(holder) }

        val message = failure.message.orEmpty()
        assertTrue(message.contains("first.Dup") && message.contains("second.Dup"), message)
    }

    @Test
    fun `names are the serial name after the last dot with unsafe characters replaced`() {
        val walked = walker.walk(SchemaFixtures.Renamed.serializer().descriptor)

        assertEquals("Order_Item_", walked.root)
    }

    @Test
    fun `serial names in messages are shortened`() {
        val descriptor =
            buildClassSerialDescriptor("T".repeat(500)) {
                element("e".repeat(500), ListSerializer(String.serializer()).descriptor)
            }

        val failure = assertFailsWith<IllegalArgumentException> { walker.walk(descriptor) }

        assertTrue(failure.message.orEmpty().length < 600, failure.message)
    }

    private fun chain(length: Int): SerialDescriptor {
        var next: SerialDescriptor =
            buildClassSerialDescriptor("N${length - 1}") { element("value", String.serializer().descriptor) }
        for (index in length - 2 downTo 0) {
            val child = next
            next = buildClassSerialDescriptor("N$index") { element("next", child) }
        }
        return next
    }
}
