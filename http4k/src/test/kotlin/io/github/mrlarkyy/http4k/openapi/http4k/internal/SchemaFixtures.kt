package io.github.mrlarkyy.http4k.openapi.http4k.internal

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** `@Serializable` types for the schema tests; the schema name of each is its simple name. */
internal object SchemaFixtures {
    @Serializable
    data class Inner(
        val id: String,
    )

    @Serializable
    data class Item(
        val name: String,
        val count: Int,
        val big: Long,
        val price: Double,
        val ok: Boolean,
        val inner: Inner,
    )

    @Serializable
    data class Wrapper(
        val inner: Inner,
        val label: String,
    )

    @Serializable
    data object Marker

    @Serializable
    data class Tree(
        val label: String,
        val child: Tree?,
    )

    @Serializable
    data class Recursive(
        val label: String,
        val self: Recursive,
    )

    @Serializable
    data class Nullable(
        val value: String?,
    )

    @Serializable
    data class Optional(
        val value: String = "x",
    )

    @Serializable
    data class WithList(
        val values: List<String>,
    )

    @Serializable
    data class WithMap(
        val values: Map<String, String>,
    )

    @Serializable
    data class WithFloat(
        val value: Float,
    )

    enum class Color { RED }

    @Serializable
    data class WithEnum(
        val color: Color,
    )

    @Serializable
    @JvmInline
    value class Id(
        val value: String,
    )

    @Serializable
    data class WithInline(
        val id: Id,
    )

    @Serializable
    sealed interface Shape

    @Serializable
    data class WithSealed(
        val shape: Shape,
    )

    @Serializable
    @SerialName("shop.Order Item!")
    data class Renamed(
        val value: String,
    )

    @Serializable
    @SerialName("first.Dup")
    data class DupFirst(
        val value: String,
    )

    @Serializable
    @SerialName("second.Dup")
    data class DupSecond(
        val value: String,
    )

    @Serializable
    @SerialName("Same")
    data class SameV1(
        val id: String,
    )

    @Serializable
    @SerialName("Same")
    data class SameV2(
        val key: String,
    )

    @Serializable
    data class WrapsSame(
        val first: SameV1,
        val second: SameV2,
    )

    @Serializable
    @SerialName(LONG_NAME)
    data class LongNamed(
        val value: String?,
    )

    const val LONG_NAME =
        "p.0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789" +
            "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789" +
            "0123456789012345678901234567890123456789"

    data class NotSerializable(
        val value: String,
    )
}
