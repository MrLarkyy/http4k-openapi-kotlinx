# http4k-openapi-kotlinx

Generates OpenAPI documents for [http4k](https://www.http4k.org/) contracts from
[kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization) descriptors.

## Status

Pre-alpha. The build, module layout and supported version ranges are in place; schema generation is not
implemented yet, so there is no quick start. The public API is empty and will be declared by later releases.

## Supported versions

| Component               | Supported range                | Tested against            |
|-------------------------|--------------------------------|---------------------------|
| Kotlin (consumers)      | 2.2 and newer                  | 2.4.21 (build)            |
| JVM                     | 21                             | 21                        |
| http4k                  | 6.0.0.0 to 6.61.0.0            | lowest, current and newest |
| kotlinx.serialization   | 1.8.0 to 1.13.0                | lowest, current and newest |

The library is compiled against the lowest supported versions and every test suite also runs against the
current and newest ones. Note that http4k API classes differ between 6.0 and 6.61; the library will adapt
internally.

## Modules

| Module  | Artifact                                                  | Contents                          |
|---------|-----------------------------------------------------------|-----------------------------------|
| `core`  | `io.github.mrlarkyy:http4k-openapi-kotlinx-core`          | descriptor handling, free of http4k |
| `http4k`| `io.github.mrlarkyy:http4k-openapi-kotlinx`               | http4k contract integration       |

The `tests` module holds cross-module tests and is not published.

## Build

```bash
./gradlew build          # compile, ktlint, tests on all version entries, API check
./gradlew ktlintFormat   # fix formatting
./gradlew testCurrent   # one matrix entry (testNewest likewise)
./gradlew apiCheck       # verify the public API against the committed dumps
./gradlew apiDump        # update the dumps, only for a deliberate API change
```

## License

Apache License 2.0, see [LICENSE](LICENSE).
