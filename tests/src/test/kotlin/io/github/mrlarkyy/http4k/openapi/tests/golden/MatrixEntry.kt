package io.github.mrlarkyy.http4k.openapi.tests.golden

/**
 * The dependency-matrix entry a test task runs against. [lookupSuffixes] lists the golden file suffixes to try in
 * order; `null` is the shared baseline file.
 */
enum class MatrixEntry(
    val id: String,
    val lookupSuffixes: List<String?>,
) {
    LOWEST("lowest", listOf(null)),
    CURRENT("current", listOf("current", null)),
    NEWEST("newest", listOf("newest", "current", null)),
    ;

    companion object {
        const val PROPERTY = "h4oa.matrix.entry"

        /** Maps the task property to an entry; a missing value is [LOWEST], an unknown one fails. */
        fun fromProperty(value: String?): MatrixEntry {
            if (value == null) return LOWEST
            return entries.firstOrNull { it.id == value }
                ?: throw IllegalArgumentException(
                    "Unknown matrix entry '$value' in $PROPERTY; expected one of ${entries.joinToString { it.id }}",
                )
        }
    }
}
