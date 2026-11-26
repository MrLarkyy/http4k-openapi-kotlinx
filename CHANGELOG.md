# Changelog

All notable changes to this project are documented in this file. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Added

- Gradle build skeleton with the `core` and `http4k` library modules and a `tests` module, published as
  `io.github.mrlarkyy:http4k-openapi-kotlinx-core` and `io.github.mrlarkyy:http4k-openapi-kotlinx` (0.1.0-SNAPSHOT).
- Supported ranges: Kotlin 2.2+, JVM 21, http4k 6.0.0.0 to 6.61.0.0, kotlinx.serialization 1.8.0 to 1.13.0,
  tested against the lowest, current and newest versions.
- Public API validation, ktlint and a check that `core` stays free of http4k.
- Continuous integration on Linux and Windows.
- Golden-file tests that render real http4k contracts, validate them against the OpenAPI 3.0 / 3.1 schemas and
  compare them with approved documents.
