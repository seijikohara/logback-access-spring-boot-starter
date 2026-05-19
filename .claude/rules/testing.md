---
paths:
  - "**/src/test/**"
  - "examples/**"
---

# Testing Conventions

## Unit Tests (core and starter modules)

- **Framework**: [Kotest](https://kotest.io/) with the `FunSpec` style.
- **Mocking**: [MockK](https://mockk.io/).
- **Location**: `src/test/kotlin/`, mirroring the package layout of the main source.
- **Class name**: `<TypeUnderTest>Spec` (for example, `LogbackAccessPropertiesSpec`).

### Kotest FunSpec Pattern

```kotlin
class ExampleSpec : FunSpec({
    test("returns the resolved username when an authenticated principal is present") {
        // arrange
        // act
        // assert with Kotest matchers
    }
})
```

Prefer behavioral test names (`"returns ..."`, `"throws ... when ..."`) over generic names (`"test1"`).

## Java Interop Tests (core module)

- **Framework**: JUnit 5.
- **Location**: `src/test/java/` in `logback-access-spring-boot-starter-core`.
- **Purpose**: verify that the Kotlin public API is consumable from Java without `kotlin.Unit` / `kotlin.Pair` leaks, default-argument issues, or nullability surprises.

## Integration Tests (`examples/*`)

- **Framework**: JUnit 5 with AssertJ assertions.
- **Pattern**: `@SpringBootTest(webEnvironment = RANDOM_PORT)` against a real embedded server, plus the project's `HttpClientTestUtils` to issue requests.
- **Coverage matrix**: four runnable apps — `tomcat-mvc`, `tomcat-webflux`, `jetty-mvc`, `jetty-webflux` — exercise the cross product of server (Tomcat / Jetty) and stack (Servlet MVC / reactive WebFlux).
- **TeeFilter tests** are Tomcat-only. The Jetty equivalent (`JettyTeeFilterTest`) is annotated `@Disabled` because Jetty 12's `RequestLog` API runs below the Servlet layer and cannot see attributes that TeeFilter writes to the Servlet request.
- **Shared utilities** (`HttpClientTestUtils`, `AccessEventTestUtils`, abstract base classes) live in `examples/common`.

## Running Tests

```bash
./gradlew test                                  # All unit tests across modules
./gradlew :examples:tomcat-mvc:test             # One example app
./gradlew :examples:tomcat-mvc:test --tests "*BasicAccessLogTest*"  # One test class
```

## Verification before Submitting

Run `./gradlew clean build` before claiming work is complete. The task executes Spotless, Detekt, all unit tests, and the integration tests under `examples/*`.
