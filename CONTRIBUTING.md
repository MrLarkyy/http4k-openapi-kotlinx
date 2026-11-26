# Contributing

## Build

Use the Gradle wrapper with JDK 21 (a toolchain is resolved automatically if missing):

```bash
./gradlew build
```

`build` compiles, runs ktlint, runs the tests against the lowest, current and newest http4k and
kotlinx.serialization versions, and checks the public API.

## Style

Formatting is enforced by ktlint (`ktlint_official`). Run `./gradlew ktlintFormat` before committing.

## Versions

All versions live in `gradle/libs.versions.toml`. Raising a `-lowest` entry is a breaking change for
consumers and needs a changelog entry.

## Public API

`apiCheck` must stay green. Run `./gradlew apiDump` only for a deliberate API change and commit the updated
dump together with a changelog entry.

## Tests

Every change needs tests. Run `./gradlew build` before opening a pull request.

## Golden files

Generated OpenAPI documents are checked against approved JSON files in `tests/src/test/resources/golden`. A
golden test renders a real http4k `contract { }`, validates the document against the bundled official OpenAPI
3.0 or 3.1 schema (nothing is fetched from the network), and compares its canonical form (sorted keys, 2-space
indent, LF) with the approved file.

If a document changes on purpose, rewrite the approved files locally and review the diff like code:

```bash
./gradlew :tests:check -PapproveGolden=true
```

Approving is refused when the `CI` environment variable is set, and it never writes a document that is invalid
or renders differently on a second run. Never edit an approved file by hand. A failing run writes the actual
output to `tests/build/golden-received`. When a version entry legitimately renders differently, approval
writes `<name>.current.json` or `<name>.newest.json` next to the shared file; such a file exists only while
that entry differs.
