---
paths:
  - "logback-access-spring-boot-starter-core/**"
  - "logback-access-spring-boot-starter/**"
---

# Architecture Rules

## Module Boundaries

### Core module (`logback-access-spring-boot-starter-core`)

- Holds the public API: configuration properties, data models (`AccessEventData`, `LogbackAccessEvent`, `LocalPortStrategy`), and Joran XML extensions (`<springProfile>`, `<springProperty>`).
- Must not depend on Tomcat, Jetty, Spring Security, or any other server-specific library. If a server type is required, the integration belongs in the starter module instead.
- Enforces `kotlin { explicitApi() }`. Every public declaration requires an explicit visibility modifier and an explicit return type.
- Public API changes are tracked by `binary-compatibility-validator`. The committed `.api` files must reflect the current source.

### Starter module (`logback-access-spring-boot-starter`)

- Depends on the core module via `api(project(":logback-access-spring-boot-starter-core"))`.
- Treats server libraries (Tomcat, Jetty, Spring Security) as `compileOnly` dependencies; runtime presence is detected with `@ConditionalOnClass`, and the Servlet/reactive split with `@ConditionalOnWebApplication`.
- Auto-configuration entry point: `LogbackAccessAutoConfiguration`. Per-server integrations are imported via `@Import`.

## Conditional Configuration Pattern

Every server integration follows the same three-part pattern:

1. **A `*Configuration` class** annotated with `@ConditionalOnClass(...)` and `@ConditionalOnWebApplication(...)` that registers the integration bean.
2. **An event source** (e.g., `TomcatEventSource`, `JettyEventSource`) that converts a server-specific request/response into an immutable `AccessEventData` snapshot, then emits a `LogbackAccessEvent` through `LogbackAccessContext.emit(...)`.
3. **Data extractor objects** that pull headers, cookies, attributes, parameters, and body content from server objects. Keep server-specific types contained inside the extractor — return primitive Kotlin types.

## Spring Boot Auto-Configuration

- Registration: `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
- Configuration properties prefix: `logback.access`.
- Default config file search order, applied when `logback.access.config-location` is unset:
  1. `classpath:logback-access-test.xml`
  2. `classpath:logback-access.xml`
  3. `classpath:logback-access-test-spring.xml`
  4. `classpath:logback-access-spring.xml`
  5. Built-in fallback bundled in `logback-access-spring-boot-starter-core/src/main/resources/...`.
- The `<springProfile>` / `<springProperty>` Joran extensions activate only when the file name matches the `*-spring.xml` (or `-test-spring.xml`) suffix.

## API Compatibility

- Run `./gradlew apiCheck` to detect unintended changes to the public API.
- After an intentional public API change, run `./gradlew apiDump` and commit the updated `.api` files in the same change set.
