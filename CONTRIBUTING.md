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
consumers and needs a changelog entry. Library code may only use http4k and kotlinx.serialization API that is
present across the whole supported range; `Http4kLinkageTest` fails otherwise.

## Public API

The Kotlin Gradle plugin ABI validation guards the public API of `core` and `http4k`; the dumps live in
`<module>/api`. `apiCheck` (alias of `checkKotlinAbi`) must stay green and runs as part of `check`. Run
`./gradlew apiDump` (alias of `updateKotlinAbi`) only for a deliberate API change and commit the updated dump
together with a changelog entry.

## Tests

Every change needs tests. Run `./gradlew build` before opening a pull request.
