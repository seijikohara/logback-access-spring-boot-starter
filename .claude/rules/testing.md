---
paths:
  - "**/src/test/**"
  - "examples/**"
---

# Testing Conventions

## Unit Tests (Core & Starter Modules)

- Framework: **Kotest** with **FunSpec** style
- Mocking: **MockK**
- Place in `src/test/kotlin/` mirroring the main source package structure
- Name test classes as `<ClassName>Spec` (e.g., `LogbackAccessPropertiesSpec`)

### Kotest FunSpec Pattern

```kotlin
class ExampleSpec : FunSpec({
    test("should do something") {
        // arrange
        // act
        // assert using Kotest matchers
    }
})
```

## Java Interop Tests (Core Module)

- Framework: **JUnit 5**
- Place in `src/test/java/`
- Verify that the Kotlin API is usable from Java

## Integration Tests (examples/)

- Framework: **JUnit 5** with **AssertJ** assertions
- Use `@SpringBootTest` with a random port
- Four example apps cover the matrix: Tomcat/Jetty x MVC/WebFlux
- TeeFilter tests are Tomcat-only (skipped on Jetty)
- Shared test utilities live in `examples/common`

## Running Tests

```bash
./gradlew test          # all tests
./gradlew :examples:tomcat-mvc:test   # specific module
```

## Verification

Always run `./gradlew clean build` before submitting changes. This executes Spotless, Detekt, and all tests.
