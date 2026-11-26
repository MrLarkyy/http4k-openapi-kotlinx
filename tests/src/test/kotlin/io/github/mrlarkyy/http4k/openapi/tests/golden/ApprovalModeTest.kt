package io.github.mrlarkyy.http4k.openapi.tests.golden

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ApprovalModeTest {
    @Test
    fun `without approval the mode is verify whatever CI says`() {
        assertEquals(ApprovalMode.Verify, ApprovalMode.resolve(null, null))
        assertEquals(ApprovalMode.Verify, ApprovalMode.resolve("false", null))
        assertEquals(ApprovalMode.Verify, ApprovalMode.resolve("false", "true"))
    }

    @Test
    fun `approval is allowed when CI is unset or false-like`() {
        for (ci in listOf(null, "", "false", "0")) {
            assertEquals(ApprovalMode.Approve, ApprovalMode.resolve("true", ci), "CI=$ci")
        }
    }

    @Test
    fun `approval is refused when CI is set`() {
        for (ci in listOf("true", "1", "yes")) {
            val failure = assertFailsWith<IllegalStateException> { ApprovalMode.resolve("true", ci) }
            assertContains(failure.message.orEmpty(), "refused on CI")
            assertContains(failure.message.orEmpty(), "CI=$ci")
        }
    }

    @Test
    fun `matrix entries have their lookup chains`() {
        assertEquals(listOf<String?>(null), MatrixEntry.LOWEST.lookupSuffixes)
        assertEquals(listOf("current", null), MatrixEntry.CURRENT.lookupSuffixes)
        assertEquals(listOf("newest", "current", null), MatrixEntry.NEWEST.lookupSuffixes)
    }

    @Test
    fun `matrix entry comes from the property`() {
        assertEquals(MatrixEntry.LOWEST, MatrixEntry.fromProperty(null))
        assertEquals(MatrixEntry.LOWEST, MatrixEntry.fromProperty("lowest"))
        assertEquals(MatrixEntry.CURRENT, MatrixEntry.fromProperty("current"))
        assertEquals(MatrixEntry.NEWEST, MatrixEntry.fromProperty("newest"))
    }

    @Test
    fun `an unknown matrix entry fails`() {
        val failure = assertFailsWith<IllegalArgumentException> { MatrixEntry.fromProperty("oldest") }

        assertContains(failure.message.orEmpty(), "oldest")
    }
}
