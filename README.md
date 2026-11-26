# http4k-openapi-kotlinx

Generates OpenAPI documents for [http4k](https://www.http4k.org/) contracts from
[kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization) descriptors.

## Status

Pre-alpha. `KotlinxApiRenderer` builds the schemas of simple `@Serializable` classes for http4k contracts; more
property kinds follow in later releases.

## Quick start

Pass the renderer to `OpenApi3` and give every body an example. http4k only asks for a schema of the example of a
body, so a body needs an example.

```kotlin
@Serializable
data class Inner(val id: String)

@Serializable
data class Item(val name: String, val count: Int, val inner: Inner)

val item = Item("pen", 3, Inner("i-1"))
val lens = Body.auto<Item>().toLens()   // org.http4k.format.KotlinxSerialization.auto

val handler =
    contract {
        renderer = OpenApi3(apiInfo, KotlinxSerialization, apiRenderer = KotlinxApiRenderer(KotlinxSerialization))
        descriptionPath = "/openapi.json"
        routes +=
            "/items" meta {
                receiving(lens to item)
                returning(OK, lens to item)
            } bindContract POST to { _: Request -> Response(OK) }
    }
```

The document then holds one schema for each type under `components/schemas`, named after the serial name without
its package (`Item` and `Inner` above), and the bodies refer to it with a `$ref`. A type used by several routes is
defined once. `required` lists the properties in declaration order.

### What is supported

- Classes and objects whose properties are non-null, non-optional `String`, `Boolean`, `Int`, `Long` or `Double`, or
  further such classes (rendered as `$ref`).
- An example of a subtype of a sealed or open hierarchy is rendered as that concrete class, without a discriminator.
- Examples that are JSON elements, strings, numbers, generic classes or anything else without a class or object
  descriptor are rendered by http4k itself, as without this renderer.

Everything else throws an `IllegalArgumentException` that names the serial name and the property: nullable and
optional properties (so a class that refers to itself fails too), enums, collections, maps, floats, polymorphic
properties, value classes (as a property or as the example) and contextual serializers. These are planned. A `Json`
with a naming strategy is rejected too. Two different schemas that would get one name, also one that http4k rendered,
throw an `IllegalStateException` instead of overwriting each other.

Use one `KotlinxApiRenderer` per contract: the instance remembers the schema names it has handed out.

## Supported versions

| Component               | Supported range                | Tested against            |
|-------------------------|--------------------------------|---------------------------|
| Kotlin (consumers)      | 2.2 and newer                  | 2.4.21 (build)            |
| JVM                     | 21                             | 21                        |
| http4k                  | 6.0.0.0 to 6.61.0.0            | lowest, current and newest |
| kotlinx.serialization   | 1.8.0 to 1.13.0                | lowest, current and newest |

The library is compiled against the lowest supported versions and every test suite also runs against the
current and newest ones.

http4k changed the `ApiInfo` constructor in 6.54.0.0, so code compiled against an earlier version does not link
against a later one. Construct `ApiInfo` in your own code against the http4k version you use; the library never
constructs it. A linkage test checks the http4k and kotlinx.serialization references of the `http4k` module
against the lowest, current and newest versions.

http4k 6.54.0.0 and newer pull in kotlin-stdlib 2.4.0 and kotlinx-serialization 1.11.0, so projects that stay on
Kotlin 2.2 need an older http4k.

## Modules

| Module  | Artifact                                                  | Contents                          |
|---------|-----------------------------------------------------------|-----------------------------------|
| `core`  | `io.github.mrlarkyy:http4k-openapi-kotlinx-core`          | descriptor handling, free of http4k |
| `http4k`| `io.github.mrlarkyy:http4k-openapi-kotlinx`               | http4k contract integration       |

The `tests` module holds cross-module tests and is not published.

## Build

```bash
./gradlew build          # compile, ktlint, tests on all version entries, API check (Kotlin ABI validation)
./gradlew ktlintFormat   # fix formatting
./gradlew testCurrent   # one matrix entry (testNewest likewise)
./gradlew apiCheck       # checkKotlinAbi: verify the public API against the committed dumps
./gradlew apiDump        # updateKotlinAbi: update the dumps, only for a deliberate API change
./gradlew :tests:check -PapproveGolden=true   # rewrite approved golden files (refused on CI)
```

## License

Apache License 2.0, see [LICENSE](LICENSE).
