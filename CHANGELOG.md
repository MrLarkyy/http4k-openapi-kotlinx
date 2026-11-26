# Changelog

All notable changes to this project are documented in this file. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Added

- `KotlinxApiRenderer`, an http4k `ApiRenderer` that builds the `components/schemas` of `@Serializable` request and
  response examples from their kotlinx.serialization descriptors. It supports classes and objects with `String`,
  `Boolean`, `Int`, `Long` and `Double` properties and nested classes (as `$ref`); other constructs (nullable or
  optional properties, collections, enums, value classes and so on) fail with an `IllegalArgumentException` naming the
  serial name and the property, and two different schemas with one name fail with an `IllegalStateException` when at
  least one of them comes from this renderer (two that http4k rendered keep http4k's behaviour). Examples
  without a class or object descriptor, such as JSON elements, are rendered by http4k. Use one renderer per contract.
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
- Golden-file tests that render real http4k contracts, validate them against the OpenAPI 3.0 / 3.1 schemas and
  compare them with approved documents.
