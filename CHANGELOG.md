# Changelog

All notable changes to this project are documented in this file. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Added

- Gradle build skeleton with the `core` and `http4k` library modules and a `tests` module, published as
  `io.github.mrlarkyy:http4k-openapi-kotlinx-core` and `io.github.mrlarkyy:http4k-openapi-kotlinx` (0.1.0-SNAPSHOT).
- Supported ranges: Kotlin 2.2+, JVM 21, http4k 6.0.0.0 to 6.61.0.0, kotlinx.serialization 1.8.0 to 1.13.0,
  tested against the lowest, current and newest versions.
- Public API validation with the built-in ABI validation of the Kotlin Gradle plugin (`apiCheck` and `apiDump`,
  aliases of `checkKotlinAbi` and `updateKotlinAbi`), ktlint and a check that `core` stays free of http4k.
- Supported http4k range confirmed as 6.0.0.0 to 6.61.0.0 (the `ApiInfo` constructor changed in 6.54.0.0; the
  library never constructs it); a linkage test checks the http4k and kotlinx.serialization references of the
  `http4k` module against every tested version.
- Continuous integration on Linux and Windows.
