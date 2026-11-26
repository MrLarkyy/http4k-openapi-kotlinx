package io.github.mrlarkyy.http4k.openapi.tests.golden

import java.nio.file.Path

/** An approved golden file and its text with LF line endings. */
data class ApprovedGolden(
    val path: Path,
    val text: String,
)
