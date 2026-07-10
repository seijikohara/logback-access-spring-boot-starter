---
paths:
  - "**/build.gradle.kts"
  - "gradle/libs.versions.toml"
  - "buildSrc/**"
---

# Dependency Management Rules

## Version Catalog

- Declare every dependency version in `gradle/libs.versions.toml` under `[versions]`.
- Never hardcode versions in `build.gradle.kts`. Always reference them via `libs.*`.
- Use BOM / platform dependencies to keep related artifacts aligned. The project uses the Spring Boot BOM (`libs.spring.boot.dependencies`) and the Kotest BOM (`libs.kotest.bom`).

## Adding a Dependency

1. Add the version to `[versions]` in `libs.versions.toml`.
2. Add the artifact to `[libraries]` with `version.ref` pointing at that version key.
3. Reference it from the module's `build.gradle.kts` as `libs.<library.name>`.

## Dependency Scopes in the Starter Module

| Scope | Use for |
|-------|---------|
| `api` | The core module (`api(project(":logback-access-spring-boot-starter-core"))`) and the Spring Boot platform BOM. |
| `implementation` | Required runtime dependencies (e.g., `spring-boot-starter`, `kotlin-logging`). |
| `compileOnly` | Optional server/framework dependencies (Tomcat, Jetty, Spring Security). |
| `testImplementation` | Full implementations of the `compileOnly` dependencies, plus Kotest, MockK, etc. |

The `compileOnly` scope is critical: it lets the starter compile against multiple optional servers without dragging them onto user classpaths.

## Automated Updates

- Renovate (`renovate.json`) opens dependency-update pull requests automatically.
- Review the PR, wait for CI, and merge once green.

## Build Plugins (`buildSrc`)

- `buildSrc/src/main/kotlin/maven-publish-conventions.gradle.kts` centralizes the publishing configuration shared by the two published modules.
- Apply it from a module's `build.gradle.kts` with `id("maven-publish-conventions")`.
