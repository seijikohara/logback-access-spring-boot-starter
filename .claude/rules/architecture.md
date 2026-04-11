---
paths:
  - "logback-access-spring-boot-starter-core/**"
  - "logback-access-spring-boot-starter/**"
---

# Architecture Rules

## Module Boundaries

### Core Module (`logback-access-spring-boot-starter-core`)

- Contains the public API contract: properties, data models, Joran extensions
- Must NOT depend on Tomcat, Jetty, Spring Security, or any server-specific library
- Uses `explicitApi()` — all public symbols require explicit visibility and types
- API changes are tracked by binary-compatibility-validator (`.api` file)

### Starter Module (`logback-access-spring-boot-starter`)

- Depends on the core module via `api(project(":logback-access-spring-boot-starter-core"))`
- Server libraries (Tomcat, Jetty, WebMVC, WebFlux, Security) are `compileOnly` dependencies
- Uses `@Conditional*` annotations for runtime classpath detection
- Auto-configuration entry point: `LogbackAccessAutoConfiguration`

## Conditional Configuration Pattern

Each server integration follows the same pattern:

1. A `*Configuration` class with `@Conditional*` annotations
2. Event source classes that bridge server-specific events to `LogbackAccessEvent`
3. Data extractor classes that pull request/response data from server objects

## Spring Boot Auto-Configuration

- Registration via `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Configuration properties prefix: `logback.access`
- Default config file search order: `logback-access-test.xml` → `logback-access.xml` → `logback-access-test-spring.xml` → `logback-access-spring.xml`

## API Compatibility

- Run `./gradlew apiCheck` to verify no unintended API changes
- Run `./gradlew apiDump` after intentional public API changes and commit the updated `.api` file
