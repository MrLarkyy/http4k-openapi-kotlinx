package io.github.mrlarkyy.http4k.openapi.tests.golden

import org.http4k.contract.Tag

/**
 * Creates http4k [Tag] values by reflection: `Tag` gained constructor parameters between http4k 6.0.0.0 and 6.61.0.0
 * (all strings, the ones after the description default to `null`), so no compiled call links against every version.
 */
object RouteTags {
    /** A tag named [name] with [description], which keeps http4k from rendering its empty default tag. */
    fun of(
        name: String,
        description: String,
    ): Tag {
        val constructor =
            Tag::class.java.constructors
                .filter { constructor -> constructor.parameterTypes.all { it == String::class.java } && constructor.parameterCount >= 2 }
                .minByOrNull { it.parameterCount }
        checkNotNull(constructor) { "Tag has no public constructor of strings" }
        val arguments = arrayOfNulls<Any>(constructor.parameterCount)
        arguments[0] = name
        arguments[1] = description
        return constructor.newInstance(*arguments) as Tag
    }
}
