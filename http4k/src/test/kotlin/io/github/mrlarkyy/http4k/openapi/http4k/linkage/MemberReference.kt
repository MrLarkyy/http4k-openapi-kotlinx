package io.github.mrlarkyy.http4k.openapi.http4k.linkage

/** A member of another class that a compiled class refers to, in JVM internal naming. */
internal data class MemberReference(
    val owner: String,
    val name: String,
    val descriptor: String,
    val kind: Kind,
) {
    enum class Kind { FIELD, METHOD, CONSTRUCTOR }

    override fun toString(): String =
        when (kind) {
            Kind.FIELD -> "field $owner.$name:$descriptor"
            Kind.METHOD -> "method $owner.$name$descriptor"
            Kind.CONSTRUCTOR -> "constructor $owner.<init>$descriptor"
        }
}
