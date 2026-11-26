package io.github.mrlarkyy.http4k.openapi.tests.golden

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * Approved golden files of one matrix entry. `<name>.json` is the shared baseline; `<name>.current.json` and
 * `<name>.newest.json` override it for that entry only, where the rendering of a newer http4k differs.
 *
 * @param dir directory holding the approved files
 * @param receivedDir directory that receives the actual output of a failing run
 */
class GoldenFiles(
    private val dir: Path,
    private val receivedDir: Path,
    private val entry: MatrixEntry,
) {
    /** Paths looked up for [name], in order of precedence. */
    fun candidates(name: String): List<Path> {
        requireValidName(name)
        return entry.lookupSuffixes.map { suffix -> fileFor(name, suffix) }
    }

    /** The first existing approved file for [name], or `null` when there is none. */
    fun approved(name: String): ApprovedGolden? =
        candidates(name).firstOrNull { Files.isRegularFile(it) }?.let { ApprovedGolden(it, read(it)) }

    /**
     * Records [canonical] as the approved output of this entry: the own override file is written only when the output
     * differs from what the entries below it already approve, and a stale override is deleted.
     */
    fun approve(
        name: String,
        canonical: String,
    ) {
        requireValidName(name)
        val own = entry.lookupSuffixes.first()
        val inheritedFiles = entry.lookupSuffixes.drop(1).map { fileFor(name, it) }
        val inherited = inheritedFiles.firstOrNull { Files.isRegularFile(it) }?.let { read(it) }
        when {
            own == null -> write(fileFor(name, null), canonical)
            inherited == null -> write(fileFor(name, null), canonical)
            inherited == normalise(canonical) -> Files.deleteIfExists(fileFor(name, own))
            else -> write(fileFor(name, own), canonical)
        }
    }

    /** Writes the actual output of a failing run and returns its path. */
    fun writeReceived(
        name: String,
        canonical: String,
    ): Path {
        requireValidName(name)
        return write(receivedDir.resolve("$name.${entry.id}.json"), canonical)
    }

    private fun fileFor(
        name: String,
        suffix: String?,
    ): Path = dir.resolve(if (suffix == null) "$name.json" else "$name.$suffix.json")

    private fun read(path: Path): String = normalise(Files.readString(path, StandardCharsets.UTF_8))

    private fun write(
        target: Path,
        text: String,
    ): Path {
        Files.createDirectories(target.parent)
        val temporary = Files.createTempFile(target.parent, target.fileName.toString(), ".tmp")
        try {
            Files.writeString(temporary, normalise(text), StandardCharsets.UTF_8)
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } finally {
            Files.deleteIfExists(temporary)
        }
        return target
    }

    private fun normalise(text: String): String {
        val lf = text.replace("\r\n", "\n")
        return if (lf.endsWith("\n")) lf else lf + "\n"
    }

    private fun requireValidName(name: String) {
        require(NAME.matches(name) && ".." !in name) {
            "Invalid golden name '$name': must match ${NAME.pattern} and not contain '..'"
        }
    }

    private companion object {
        val NAME = Regex("^[a-z0-9][a-z0-9.-]*$")
    }
}
