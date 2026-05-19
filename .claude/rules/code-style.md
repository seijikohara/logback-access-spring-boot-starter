---
paths:
  - "**/*.kt"
  - "**/*.kts"
---

# Kotlin Code Style

## Formatting

- Spotless with ktlint enforces formatting on every `./gradlew build`. The max line length is 140.
- Run `./gradlew spotlessApply` to auto-fix violations.
- Do not override ktlint rules manually. If a rule is wrong for this project, change the configuration instead.

## Explicit API (core module)

`logback-access-spring-boot-starter-core` enables `kotlin { explicitApi() }`:

- Every public declaration needs an explicit visibility modifier.
- Every public function needs an explicit return type.
- Every public property needs an explicit type.

Internal helpers should be marked `internal` rather than left without a modifier.

## KDoc

Detekt enforces KDoc on every public class, function, and property in the core module. Test sources are exempt.

- Start each KDoc description with a verb in the imperative form, for example `Return the access event data.`
- Explain *why* (a constraint, invariant, or workaround), not *what* (which the code already shows).
- Reference related types with `[Type]` so that Dokka resolves the link.
- Keep KDoc to a few lines. Longer rationale belongs in commit messages or in the project documentation under `docs/`.

## Naming

- Standard Kotlin conventions: `camelCase` for functions and properties, `PascalCase` for classes and objects, `UPPER_SNAKE_CASE` for constants.
- Kotest spec class names end with `Spec` (for example, `LogbackAccessPropertiesSpec`).
- Java interop test class names follow JUnit 5 conventions and live under `src/test/java/`.

## Imports

- No wildcard imports.
- Organize imports alphabetically; ktlint enforces ordering.
