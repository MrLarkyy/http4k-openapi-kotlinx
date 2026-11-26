# Bundled OpenAPI schemas

Official JSON Schemas published by the OpenAPI Initiative, used by the golden tests to validate generated documents
without network access. Each file keeps the path of its source IRI below `https://spec.openapis.org/`.
They are unmodified downloads, retrieved on 2026-11-26. Licence: Apache-2.0 (OpenAPI Initiative).

| File | Source IRI |
| --- | --- |
| `oas/3.0/schema/2024-10-18.json` | https://spec.openapis.org/oas/3.0/schema/2024-10-18 |
| `oas/3.1/schema-base/2026-11-05.json` | https://spec.openapis.org/oas/3.1/schema-base/2026-11-05 |
| `oas/3.1/schema/2026-11-05.json` | https://spec.openapis.org/oas/3.1/schema/2026-11-05 |
| `oas/3.1/dialect/2024-11-10.json` | https://spec.openapis.org/oas/3.1/dialect/2024-11-10 |
| `oas/3.1/meta/2024-11-10.json` | https://spec.openapis.org/oas/3.1/meta/2024-11-10 |

The set is closed: every `$ref` and `$schema` that is not a JSON Schema meta-schema (`json-schema.org`, shipped by the
validator) resolves to one of these files. The vocabulary identifier `https://spec.openapis.org/oas/3.1/vocab/base`
is only a name and has no document.

The 3.1 documents are validated against `schema-base`, so Schema Objects are checked against the OpenAPI 3.1 dialect.
The validator evaluates it offline; the vocabulary identifier needs no document.
