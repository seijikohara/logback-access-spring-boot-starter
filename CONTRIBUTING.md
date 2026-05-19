# Contributing to logback-access-spring-boot-starter

Thank you for your interest in contributing. This guide describes how to set up the project, the conventions the codebase follows, and the pull-request workflow.

## Prerequisites

- **Java 21** or later (Temurin is recommended).
- **Git**.

No separate Kotlin or Gradle installation is required — the project ships with the Gradle Wrapper, and the Kotlin compiler is managed by the Kotlin Gradle plugin.

## Development Setup

1. Clone the repository:

   ```bash
   git clone https://github.com/seijikohara/logback-access-spring-boot-starter.git
   cd logback-access-spring-boot-starter
   ```

2. Build and run all tests:

   ```bash
   ./gradlew clean build
   ```

   This command compiles the code, runs Spotless (formatting), Detekt (static analysis), and all tests.

3. Fix formatting issues automatically:

   ```bash
   ./gradlew spotlessApply
   ```

## Module Structure

The project is split into two published modules and a set of internal example apps:

| Module | Published | Responsibility |
|--------|:---------:|----------------|
| `logback-access-spring-boot-starter-core` | ✓ | Public API, data models, Joran XML configuration extensions (`<springProperty>` / `<springProfile>`). Must not depend on Tomcat, Jetty, or Spring Security. |
| `logback-access-spring-boot-starter` | ✓ | Spring Boot auto-configuration and the Tomcat / Jetty / Spring Security / TeeFilter integrations. |
| `examples/*` | — | Runnable Spring Boot apps that double as integration tests across Tomcat × Jetty × MVC × WebFlux. |

Place new public API types in **core**. Place server-specific integrations in **starter**.

## Code Style

Two tools enforce style automatically as part of `./gradlew build`:

- **Spotless with ktlint** — Kotlin formatting (max line length: 140).
- **Detekt** — Kotlin static analysis (config in `config/detekt/detekt.yml`).

Run `./gradlew spotlessApply` to fix formatting issues, and resolve any remaining warnings before opening a pull request.

The **core** module uses `kotlin { explicitApi() }`. Every public declaration requires an explicit visibility modifier and explicit return type. Detekt also enforces KDoc on every public declaration (test sources excluded).

## Testing

The project uses different test frameworks per layer:

| Location | Type | Framework | Description |
|----------|------|-----------|-------------|
| `logback-access-spring-boot-starter-core/src/test/` | Unit | [Kotest](https://kotest.io/) FunSpec + [MockK](https://mockk.io/) | Core API, Joran extensions, configuration properties. |
| `logback-access-spring-boot-starter/src/test/` | Unit | Kotest FunSpec + MockK | Body capture policy, Tomcat extractors, Spring Security filter. |
| `logback-access-spring-boot-starter-core/src/test/java/` | Java interop | JUnit 5 | Verifies the Kotlin public API remains usable from Java. |
| `examples/*` | Integration | JUnit 5 + AssertJ | `@SpringBootTest` apps that cover Tomcat × Jetty × MVC × WebFlux. |

Run all tests with:

```bash
./gradlew test
```

## Pull Request Process

1. **Branch from `main`**:

   ```bash
   git checkout -b feat/my-feature
   ```

2. **Make the change** and verify locally:

   ```bash
   ./gradlew clean build
   ```

3. **Commit using [Conventional Commits](https://www.conventionalcommits.org/)**:

   ```bash
   git commit -m "feat: add support for custom access log patterns"
   ```

   Common prefixes:

   | Prefix | Use for |
   |--------|---------|
   | `feat` | A new feature. |
   | `fix` | A bug fix. |
   | `docs` | Documentation-only changes. |
   | `refactor` | Code change that neither fixes a bug nor adds a feature. |
   | `perf` | Performance improvement. |
   | `test` | Adding or correcting tests. |
   | `build` | Build system or external dependency changes. |
   | `ci` | CI configuration or scripts. |
   | `chore` | Other maintenance tasks. |
   | `revert` | Reverts a previous commit. |

   For breaking changes, add `!` after the type (`feat!: ...`) or include a `BREAKING CHANGE:` footer.

4. **Open a pull request against `main`**. CI must pass before review.

## Dependency Verification

The project enables [Gradle dependency verification](https://docs.gradle.org/current/userguide/dependency_verification.html) with SHA-256 checksums. After adding or upgrading a dependency, regenerate the verification metadata:

```bash
./gradlew --write-verification-metadata sha256 clean build
```

Commit the updated `gradle/verification-metadata.xml` alongside the dependency change.

## API Compatibility

The core module's public API is tracked by `binary-compatibility-validator`:

- Run `./gradlew apiCheck` to detect unintended changes to the public API.
- After an intentional public API change, run `./gradlew apiDump` and commit the updated `.api` files.

## Reporting Issues

- **Bugs**: file a [bug report](https://github.com/seijikohara/logback-access-spring-boot-starter/issues/new?template=bug_report.yml).
- **Features**: file a [feature request](https://github.com/seijikohara/logback-access-spring-boot-starter/issues/new?template=feature_request.yml).
- **Security**: see [SECURITY.md](SECURITY.md) for the private disclosure process.

## License

By contributing, you agree that your contributions are licensed under the [Apache License 2.0](LICENSE).
