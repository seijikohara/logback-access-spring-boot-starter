---
paths:
  - "**/build.gradle.kts"
  - "gradle/libs.versions.toml"
  - "buildSrc/**"
---

# Dependency Management Rules

## Version Catalog

- All dependency versions are declared in `gradle/libs.versions.toml`
- Never hardcode versions in `build.gradle.kts` — always use `libs.*` references
- Use BOM/platform dependencies for version alignment (Spring Boot BOM, Kotest BOM)

## Adding Dependencies

1. Add the version to `[versions]` in `libs.versions.toml`
2. Add the library to `[libraries]` with a `version.ref`
3. Reference as `libs.<library.name>` in `build.gradle.kts`

## Dependency Scopes in Starter Module

- `api`: Core module and Spring Boot platform BOM
- `implementation`: Required runtime dependencies (spring-boot-starter, kotlin-logging)
- `compileOnly`: Optional server/framework dependencies (Tomcat, Jetty, WebMVC, WebFlux, Security)
- Test dependencies get full implementations of compile-only deps

## Automated Updates

- Renovate (`renovate.json`) manages automated dependency update PRs
- Review and merge dependency PRs after CI passes

## Build Plugins (buildSrc)

- `maven-publish-conventions.gradle.kts` in `buildSrc/` provides shared publishing configuration
- Applied via `id("maven-publish-conventions")` in module build files
