package io.github.mrlarkyy.http4k.openapi.http4k.internal

import kotlinx.serialization.descriptors.SerialDescriptor

/** Schema names derived from serial names, and the text limits for error messages. */
internal object SchemaNames {
    private const val MAX_MESSAGE_TEXT = 200
    private val unsafeCharacters = Regex("[^A-Za-z0-9._-]")

    /** The schema name of [descriptor]: the serial name after its last `.`, restricted to the characters a `$ref` allows. */
    fun of(descriptor: SerialDescriptor): String {
        val name = sanitize(descriptor.serialName.substringAfterLast('.'))
        require(name.isNotEmpty()) { "${shorten(descriptor.serialName)}: serial name has no schema name" }
        return name
    }

    /** Replaces every character outside `[A-Za-z0-9._-]` by `_`. */
    fun sanitize(name: String): String = unsafeCharacters.replace(name, "_")

    /** Limits [text] taken from a descriptor before it goes into an error message. */
    fun shorten(text: String): String = if (text.length <= MAX_MESSAGE_TEXT) text else text.take(MAX_MESSAGE_TEXT) + "..."
}
