package io.github.mrlarkyy.http4k.openapi.tests.golden

import org.opentest4j.AssertionFailedError

/**
 * Golden-file check of a rendered OpenAPI document: renders twice and compares the raw output, validates it against
 * the official schema of [OpenApiSpec], and compares its canonical form with the approved file. In approval mode the
 * approved file is rewritten, but only for a deterministic, valid document.
 */
object GoldenOpenApi {
    private const val APPROVE_HINT = "./gradlew :tests:check -PapproveGolden=true"

    /**
     * Verifies (or approves) the document produced by [render] as golden file [name].
     *
     * @throws AssertionFailedError when the output is not deterministic, not valid for [spec], or differs from the
     * approved file; the actual output is then written below [GoldenSettings.receivedDir]
     */
    fun verify(
        name: String,
        spec: OpenApiSpec,
        render: () -> String,
        settings: GoldenSettings = GoldenSettings.fromSystem(),
    ) {
        val first = render()
        val second = render()
        if (first != second) {
            throw AssertionFailedError(
                "Rendering '$name' twice gave different output (not deterministic):\n" +
                    LineDiff.unified("first", "second", first, second),
                first,
                second,
            )
        }
        try {
            OpenApiValidator.validate(spec, first)
        } catch (e: AssertionFailedError) {
            throw AssertionFailedError("Golden '$name': ${e.message}", e)
        }
        val canonical = CanonicalJson.canonical(first, "golden '$name'")
        val files = GoldenFiles(settings.dir, settings.receivedDir, settings.entry)
        if (settings.mode == ApprovalMode.Approve) {
            files.approve(name, canonical)
            return
        }
        val approved = files.approved(name)
        if (approved == null) {
            val received = files.writeReceived(name, canonical)
            throw AssertionFailedError(
                "No approved file for '$name' (entry ${settings.entry.id}); looked for:\n" +
                    files.candidates(name).joinToString("\n") { "  $it" } +
                    "\nActual output written to $received\nApprove it locally with: $APPROVE_HINT",
            )
        }
        if (approved.text != canonical) {
            val received = files.writeReceived(name, canonical)
            throw AssertionFailedError(
                "Golden '$name' (entry ${settings.entry.id}) differs from ${approved.path}\n" +
                    "Actual output written to $received\n" +
                    LineDiff.unified(approved.path.toString(), received.toString(), approved.text, canonical),
                approved.text,
                canonical,
            )
        }
    }
}
