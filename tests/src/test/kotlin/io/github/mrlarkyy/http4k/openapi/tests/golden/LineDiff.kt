package io.github.mrlarkyy.http4k.openapi.tests.golden

import com.github.difflib.DiffUtils
import com.github.difflib.UnifiedDiffUtils

/** Unified line diff for failure messages. */
object LineDiff {
    const val CONTEXT_LINES = 3
    const val MAX_LINES = 200

    /** Returns the unified diff of [expected] and [actual] (capped at [MAX_LINES] lines), or `""` when they are equal. */
    fun unified(
        expectedName: String,
        actualName: String,
        expected: String,
        actual: String,
    ): String {
        val expectedLines = expected.lines()
        val patch = DiffUtils.diff(expectedLines, actual.lines())
        if (patch.deltas.isEmpty()) return ""
        val lines =
            UnifiedDiffUtils.generateUnifiedDiff(expectedName, actualName, expectedLines, patch, CONTEXT_LINES)
        if (lines.size <= MAX_LINES) return lines.joinToString("\n")
        return lines.take(MAX_LINES).joinToString("\n") + "\n... ${lines.size - MAX_LINES} more lines"
    }
}
