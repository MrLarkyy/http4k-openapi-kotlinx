package io.github.mrlarkyy.http4k.openapi.http4k.linkage

/**
 * A class, or a member of a class, that a compiled class refers to, in JVM internal naming.
 * A [Kind.CLASS] reference has an empty name and descriptor.
 */
internal data class MemberReference(
    val owner: String,
    val name: String,
    val descriptor: String,
    val kind: Kind,
) {
    enum class Kind { CLASS, FIELD, METHOD, CONSTRUCTOR }

    override fun toString(): String =
        when (kind) {
            Kind.CLASS -> "class $owner"
            Kind.FIELD -> "field $owner.$name:$descriptor"
            Kind.METHOD -> "method $owner.$name$descriptor"
            Kind.CONSTRUCTOR -> "constructor $owner.<init>$descriptor"
        }
}
