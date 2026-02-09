# Contributing to logback-access-spring-boot-starter

Thank you for your interest in contributing! This guide will help you get started.

## Prerequisites

- **Java 21** or later (Temurin recommended)
- **Git**

No Kotlin or Gradle installation is required — the project includes the Gradle Wrapper and manages the Kotlin compiler internally.

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

The project uses a two-module architecture:

| Module | Artifact | Responsibility |
|--------|----------|----------------|
| `logback-access-spring-boot-starter-core` | Published to Maven Central | Public API, data models, Joran XML configuration extensions |
| `logback-access-spring-boot-starter` | Published to Maven Central | Spring Boot auto-configuration, Tomcat/Jetty integration |

When adding new public API types, add them to the **core** module. Server-specific integrations belong in the **starter** module.

## Code Style

This project enforces code style automatically:

- **Spotless** with ktlint — Kotlin formatting (max line length: 140)
- **Detekt** — Kotlin static analysis

Both tools run as part of `./gradlew build`. Fix any issues before submitting a pull request.

## Testing

- **Framework**: [Kotest](https://kotest.io/) with FunSpec style
- **Mocking**: [MockK](https://mockk.io/)

The project contains both unit tests and integration tests:

| Location | Type | Description |
|----------|------|-------------|
| `logback-access-spring-boot-starter-core/src/test/` | Unit tests | Core API, Joran extensions, Properties (Kotest) |
| `logback-access-spring-boot-starter/src/test/` | Unit tests | Body capture, Tomcat extractors, Security filter (Kotest) |
| `examples/` | Integration tests | Full Spring Boot tests for Tomcat/Jetty × MVC/WebFlux |

Run tests only:

```bash
./gradlew test
```

## Pull Request Process

1. **Create a branch** from `main`:

   ```bash
   git checkout -b feat/my-feature
   ```

2. **Make your changes** and ensure all checks pass:

   ```bash
   ./gradlew clean build
   ```

3. **Commit using [Conventional Commits](https://www.conventionalcommits.org/)**:

   ```bash
   git commit -m "feat: add support for custom access log patterns"
   ```

   Common prefixes:
   - `feat:` — new feature
   - `fix:` — bug fix
   - `docs:` — documentation changes
   - `chore:` — maintenance tasks
   - `refactor:` — code refactoring
   - `test:` — test additions or changes

4. **Open a pull request** against `main`

5. **CI must pass** — the PR will be reviewed after all checks are green

## Dependency Verification

This project uses [Gradle dependency verification](https://docs.gradle.org/current/userguide/dependency_verification.html) with SHA-256 checksums. When adding or updating dependencies, regenerate the verification metadata:

```bash
./gradlew --write-verification-metadata sha256 clean build
```

Commit the updated `gradle/verification-metadata.xml` alongside your dependency changes.

## Reporting Issues

- **Bugs**: Use the [Bug Report](https://github.com/seijikohara/logback-access-spring-boot-starter/issues/new?template=bug_report.yml) template
- **Features**: Use the [Feature Request](https://github.com/seijikohara/logback-access-spring-boot-starter/issues/new?template=feature_request.yml) template
- **Security**: See [SECURITY.md](SECURITY.md) for vulnerability reporting

## License

By contributing, you agree that your contributions will be licensed under the [Apache License 2.0](LICENSE).
