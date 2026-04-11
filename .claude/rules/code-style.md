---
paths:
  - "**/*.kt"
  - "**/*.kts"
---

# Kotlin Code Style

## Formatting

- Use Spotless with ktlint for all Kotlin files
- Run `./gradlew spotlessApply` to auto-fix formatting issues
- Do not manually override ktlint rules

## Explicit API (Core Module)

The `logback-access-spring-boot-starter-core` module enforces `explicitApi()`:

- All public declarations must have explicit visibility modifiers
- All public functions must have explicit return types
- All public properties must have explicit types

## KDoc

Detekt enforces documentation on public API:

- Every public class, function, and property must have KDoc
- Start KDoc descriptions with a verb in imperative form (e.g., "Return the access event data.")
- Explain *why*, not *what*, when the behavior is non-obvious
- Test sources are exempt from documentation requirements

## Naming

- Use standard Kotlin naming conventions (camelCase for functions/properties, PascalCase for classes)
- Test spec classes end with `Spec` (e.g., `LogbackAccessPropertiesSpec`)

## Imports

- No wildcard imports
- Organize imports alphabetically
