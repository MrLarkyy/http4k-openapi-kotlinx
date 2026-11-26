package io.github.mrlarkyy.http4k.openapi.tests.golden

import java.nio.file.Path
import java.nio.file.Paths

/**
 * Where golden files live and how a run treats them. Tests inject their own instance; [fromSystem] reads the
 * system properties set by the Gradle build and the `CI` environment variable once.
 */
data class GoldenSettings(
    val dir: Path,
    val receivedDir: Path,
    val entry: MatrixEntry,
    val mode: ApprovalMode,
) {
    companion object {
        const val DIR_PROPERTY = "h4oa.golden.dir"

        fun fromSystem(): GoldenSettings =
            GoldenSettings(
                dir = Paths.get(System.getProperty(DIR_PROPERTY) ?: "src/test/resources/golden").toAbsolutePath(),
                receivedDir = Paths.get("build/golden-received").toAbsolutePath(),
                entry = MatrixEntry.fromProperty(System.getProperty(MatrixEntry.PROPERTY)),
                mode = ApprovalMode.resolve(System.getProperty(ApprovalMode.PROPERTY), System.getenv("CI")),
            )
    }
}
