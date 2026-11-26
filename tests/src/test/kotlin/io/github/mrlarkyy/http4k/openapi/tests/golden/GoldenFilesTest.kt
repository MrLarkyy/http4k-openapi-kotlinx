package io.github.mrlarkyy.http4k.openapi.tests.golden

import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GoldenFilesTest {
    @TempDir
    lateinit var root: Path

    private val dir get() = root.resolve("golden")
    private val received get() = root.resolve("received")

    private fun files(entry: MatrixEntry) = GoldenFiles(dir, received, entry)

    private fun put(
        file: String,
        text: String,
    ) {
        Files.createDirectories(dir)
        Files.writeString(dir.resolve(file), text)
    }

    @Test
    fun `lowest reads only the shared file`() {
        put("a.json", "base\n")
        put("a.current.json", "current\n")

        assertEquals("base\n", files(MatrixEntry.LOWEST).approved("a")?.text)
    }

    @Test
    fun `current prefers its own file and falls back to the shared one`() {
        put("a.json", "base\n")
        assertEquals("base\n", files(MatrixEntry.CURRENT).approved("a")?.text)

        put("a.current.json", "current\n")
        assertEquals("current\n", files(MatrixEntry.CURRENT).approved("a")?.text)
    }

    @Test
    fun `newest falls back to current then shared`() {
        put("a.json", "base\n")
        assertEquals("base\n", files(MatrixEntry.NEWEST).approved("a")?.text)

        put("a.current.json", "current\n")
        assertEquals("current\n", files(MatrixEntry.NEWEST).approved("a")?.text)

        put("a.newest.json", "newest\n")
        assertEquals("newest\n", files(MatrixEntry.NEWEST).approved("a")?.text)
    }

    @Test
    fun `a missing file is null and the candidates list the lookup order`() {
        val newest = files(MatrixEntry.NEWEST)

        assertNull(newest.approved("a"))
        assertEquals(
            listOf("a.newest.json", "a.current.json", "a.json"),
            newest.candidates("a").map { it.fileName.toString() },
        )
    }

    @Test
    fun `approving lowest writes the shared file`() {
        files(MatrixEntry.LOWEST).approve("a", "x\n")

        assertEquals("x\n", Files.readString(dir.resolve("a.json")))
    }

    @Test
    fun `approving current equal to the shared file deletes a stale override`() {
        put("a.json", "base\n")
        put("a.current.json", "old\n")

        files(MatrixEntry.CURRENT).approve("a", "base\n")

        assertFalse(Files.exists(dir.resolve("a.current.json")))
        assertTrue(Files.exists(dir.resolve("a.json")))
    }

    @Test
    fun `approving current that differs writes an override and keeps the shared file`() {
        put("a.json", "base\n")

        files(MatrixEntry.CURRENT).approve("a", "other\n")

        assertEquals("other\n", Files.readString(dir.resolve("a.current.json")))
        assertEquals("base\n", Files.readString(dir.resolve("a.json")))
    }

    @Test
    fun `approving newest compares with the current override`() {
        put("a.json", "base\n")
        put("a.current.json", "current\n")

        files(MatrixEntry.NEWEST).approve("a", "current\n")
        assertFalse(Files.exists(dir.resolve("a.newest.json")))

        files(MatrixEntry.NEWEST).approve("a", "newest\n")
        assertEquals("newest\n", Files.readString(dir.resolve("a.newest.json")))
    }

    @Test
    fun `bad names are rejected`() {
        val files = files(MatrixEntry.LOWEST)

        for (name in listOf("../x", "a/b", "a..b", "", "A", "-a", "a b")) {
            assertFailsWith<IllegalArgumentException>(name) { files.approved(name) }
            assertFailsWith<IllegalArgumentException>(name) { files.approve(name, "x\n") }
            assertFailsWith<IllegalArgumentException>(name) { files.writeReceived(name, "x\n") }
        }
    }

    @Test
    fun `files are written with LF and a trailing newline`() {
        files(MatrixEntry.LOWEST).approve("a", "one\r\ntwo")

        assertEquals("one\ntwo\n", Files.readString(dir.resolve("a.json")))
    }

    @Test
    fun `files read with CRLF are compared as LF`() {
        put("a.json", "one\r\ntwo\r\n")

        assertEquals("one\ntwo\n", files(MatrixEntry.LOWEST).approved("a")?.text)
    }

    @Test
    fun `received output goes to the received directory named by entry`() {
        val path = files(MatrixEntry.CURRENT).writeReceived("a", "x\n")

        assertEquals(received.resolve("a.current.json"), path)
        assertEquals("x\n", Files.readString(path))
    }

    @Test
    fun `no temporary files are left behind`() {
        files(MatrixEntry.LOWEST).approve("a", "x\n")

        Files.list(dir).use { listing -> assertEquals(listOf("a.json"), listing.map { it.fileName.toString() }.toList()) }
    }
}
