package io.github.mrlarkyy.http4k.openapi.tests.golden

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LineDiffTest {
    private fun numbered(count: Int) = (1..count).joinToString("\n", postfix = "\n") { "line $it" }

    @Test
    fun `one changed line gives a hunk with three lines of context`() {
        val expected = numbered(20)
        val actual = expected.replace("line 10\n", "changed 10\n")

        val diff = LineDiff.unified("expected.json", "actual.json", expected, actual)
        val lines = diff.lines()

        assertEquals("--- expected.json", lines[0])
        assertEquals("+++ actual.json", lines[1])
        assertTrue(lines[2].startsWith("@@"), diff)
        assertContains(lines, "-line 10")
        assertContains(lines, "+changed 10")
        assertEquals(listOf(" line 7", " line 8", " line 9"), lines.filter { it.startsWith(" line") }.take(3))
        assertEquals(6, lines.count { it.startsWith(" line") }, diff)
    }

    @Test
    fun `identical texts give an empty diff`() {
        assertEquals("", LineDiff.unified("a", "b", numbered(5), numbered(5)))
    }

    @Test
    fun `a long diff is capped with a count of the omitted lines`() {
        val expected = numbered(500)
        val actual = (1..500).joinToString("\n", postfix = "\n") { "other $it" }

        val diff = LineDiff.unified("a", "b", expected, actual)
        val lines = diff.lines()

        assertEquals(LineDiff.MAX_LINES + 1, lines.size)
        assertTrue(lines.last().matches(Regex("""\.\.\. \d+ more lines""")), lines.last())
    }
}
