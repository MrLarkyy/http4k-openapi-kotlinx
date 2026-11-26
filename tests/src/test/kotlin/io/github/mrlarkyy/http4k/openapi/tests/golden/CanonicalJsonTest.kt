package io.github.mrlarkyy.http4k.openapi.tests.golden

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class CanonicalJsonTest {
    @Test
    fun `keys are sorted recursively`() {
        val canonical = CanonicalJson.canonical("""{"b":{"z":1,"a":2},"a":3}""")

        assertEquals("{\n  \"a\": 3,\n  \"b\": {\n    \"a\": 2,\n    \"z\": 1\n  }\n}\n", canonical)
    }

    @Test
    fun `array order is kept and objects inside are sorted`() {
        val canonical = CanonicalJson.canonical("""{"l":[3,1,{"y":1,"x":2}]}""")

        assertEquals("{\n  \"l\": [\n    3,\n    1,\n    {\n      \"x\": 2,\n      \"y\": 1\n    }\n  ]\n}\n", canonical)
    }

    @Test
    fun `empty containers are printed inline`() {
        assertEquals("{}\n", CanonicalJson.canonical("{}"))
        assertEquals("[]\n", CanonicalJson.canonical("[]"))
        assertEquals("{\n  \"a\": {},\n  \"b\": []\n}\n", CanonicalJson.canonical("""{"b":[],"a":{}}"""))
    }

    @Test
    fun `output uses LF and ends with one newline`() {
        val canonical = CanonicalJson.canonical("""{"a":[1,2]}""")

        assertFalse('\r' in canonical)
        assertEquals("\n", canonical.takeLast(1))
        assertEquals("]\n}\n", canonical.takeLast(4))
    }

    @Test
    fun `numbers are kept verbatim`() {
        val canonical = CanonicalJson.canonical("""{"a":1.0,"b":10,"c":1e3,"d":-0.50}""")

        assertContains(canonical, "\"a\": 1.0")
        assertContains(canonical, "\"b\": 10,")
        assertContains(canonical, "\"c\": 1e3")
        assertContains(canonical, "\"d\": -0.50")
    }

    @Test
    fun `strings keep their escapes`() {
        val canonical = CanonicalJson.canonical("""{"a":"line\nbreak \"quoted\" \\ end","b":null,"c":true}""")

        assertContains(canonical, """"a": "line\nbreak \"quoted\" \\ end"""")
        assertContains(canonical, "\"b\": null")
        assertContains(canonical, "\"c\": true")
    }

    @Test
    fun `invalid json names the source`() {
        val failure = assertFailsWith<IllegalArgumentException> { CanonicalJson.canonical("{", "golden 'basic'") }

        assertContains(failure.message.orEmpty(), "golden 'basic'")
    }
}
