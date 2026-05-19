# Configuration

This page lists every configuration option for logback-access-spring-boot-starter.

## Application Properties

Configure the starter from `application.yml` or `application.properties`:

```yaml
logback:
  access:
    enabled: true
    # config-location: classpath:custom-access.xml  # Supports classpath: and file: prefixes
    local-port-strategy: server
    tomcat:
      # request-attributes-enabled: true  # Auto-detected from RemoteIpValve
    tee-filter:
      enabled: false
      # include-hosts: localhost,example.com
      # exclude-hosts: internal.example.com
      # max-payload-size: 65536
      # allowed-content-types:
      #   - "text/*"
      #   - "application/json"
    filter:
      # include-url-patterns:
      #   - /api/.*
      exclude-url-patterns:
        - /actuator/.*
        - /health
```

### Property Reference

| Property | Default | Description |
|----------|---------|-------------|
| `logback.access.enabled` | `true` | Enable or disable access logging. |
| `logback.access.config-location` | Auto-detected | Path to the logback-access configuration file. Supports `classpath:` and `file:` URL prefixes. |
| `logback.access.local-port-strategy` | `server` | Port to report in access logs: `server` (the port the client addressed; honors `X-Forwarded-Port` via `RemoteIpValve`) or `local` (the port of the local interface that accepted the connection). |
| `logback.access.tomcat.request-attributes-enabled` | Auto-detected | Honor `RemoteIpValve` access-log attributes. When unset, the starter enables this automatically if a `RemoteIpValve` is present in the pipeline. |
| `logback.access.tee-filter.enabled` | `false` | Enable request/response body capture (Tomcat servlet only). |
| `logback.access.tee-filter.include-hosts` | `null` (all hosts) | Comma-separated host names to include. |
| `logback.access.tee-filter.exclude-hosts` | `null` (none) | Comma-separated host names to exclude. |
| `logback.access.tee-filter.max-payload-size` | `65536` | Maximum payload size in bytes that appears in log output. Larger bodies are replaced with a sentinel. |
| `logback.access.tee-filter.allowed-content-types` | `null` | Content-Type patterns allowed for body capture. When set, completely replaces the built-in defaults (override mode). |
| `logback.access.filter.include-url-patterns` | `null` (all URLs) | Java regex patterns; the request URI must match at least one to be logged. Patterns use partial matching — use `^...$` for exact match. |
| `logback.access.filter.exclude-url-patterns` | `null` (none) | Java regex patterns; matching request URIs are dropped. Exclude takes precedence over include. |

## Configuration File Resolution

When `logback.access.config-location` is set, the starter loads that path directly and skips every fallback. If the file does not exist, the application fails to start.

When the property is unset, the starter searches the classpath in this order and uses the first existing resource:

1. `classpath:logback-access-test.xml` — picked up by tests only.
2. `classpath:logback-access.xml` — the primary configuration file.
3. `classpath:logback-access-test-spring.xml` — test variant with `<springProfile>` / `<springProperty>` support.
4. `classpath:logback-access-spring.xml` — production variant with `<springProfile>` / `<springProperty>` support.
5. A built-in fallback bundled with the starter, which logs requests in the `common` format to the console.

## XML Configuration

### Basic Structure

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- Appenders define where logs go -->
    <appender name="console" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%h %l %u [%t] "%r" %s %b</pattern>
        </encoder>
    </appender>

    <!-- Reference appenders to activate them -->
    <appender-ref ref="console"/>
</configuration>
```

### Using Spring Properties

Inject values from the Spring `Environment` with `<springProperty>` — the configuration file must be named `logback-access-spring.xml` (or `-test-spring.xml`) for the extension to be applied:

```xml
<configuration>
    <springProperty name="appName" source="spring.application.name"
                    defaultValue="app" scope="context"/>

    <appender name="console" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>[${appName}] %h %l %u [%t] "%r" %s %b</pattern>
        </encoder>
    </appender>
    <appender-ref ref="console"/>
</configuration>
```

::: warning Default Scope
`<springProperty>` defaults to `LOCAL` scope. `LOCAL` properties are only resolved during XML parsing for variable substitution (`${varName}`). To read the value programmatically via `context.getProperty()`, set `scope="context"`.
:::

### Using Spring Profiles

Activate different appenders per environment with `<springProfile>`:

```xml
<configuration>
    <springProfile name="dev">
        <appender name="console" class="ch.qos.logback.core.ConsoleAppender">
            <encoder>
                <pattern>%h %l %u [%t] "%r" %s %b %D</pattern>
            </encoder>
        </appender>
        <appender-ref ref="console"/>
    </springProfile>

    <springProfile name="prod">
        <appender name="file" class="ch.qos.logback.core.rolling.RollingFileAppender">
            <file>logs/access.log</file>
            <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
                <fileNamePattern>logs/access.%d{yyyy-MM-dd}.log</fileNamePattern>
                <maxHistory>30</maxHistory>
            </rollingPolicy>
            <encoder>
                <pattern>%h %l %u [%t] "%r" %s %b</pattern>
            </encoder>
        </appender>
        <appender-ref ref="file"/>
    </springProfile>
</configuration>
```

### Profile Expressions

`<springProfile>` supports negation and a comma-separated list of profiles:

```xml
<!-- Active when "prod" is NOT active -->
<springProfile name="!prod">
    ...
</springProfile>

<!-- Active when either "dev" or "staging" is active -->
<springProfile name="dev, staging">
    ...
</springProfile>
```

## File Appender

Write access logs to a file:

```xml
<appender name="file" class="ch.qos.logback.core.FileAppender">
    <file>logs/access.log</file>
    <encoder>
        <pattern>%h %l %u [%t] "%r" %s %b</pattern>
    </encoder>
</appender>
```

## Rolling File Appender

Rotate logs based on time or size:

```xml
<appender name="rolling" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>logs/access.log</file>
    <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
        <fileNamePattern>logs/access.%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
        <maxFileSize>100MB</maxFileSize>
        <maxHistory>30</maxHistory>
        <totalSizeCap>3GB</totalSizeCap>
    </rollingPolicy>
    <encoder>
        <pattern>%h %l %u [%t] "%r" %s %b</pattern>
    </encoder>
</appender>
```

## Disabling Access Logging

Disable the starter globally:

```yaml
logback:
  access:
    enabled: false
```

Or disable it only for a specific profile by combining `logback.access.enabled` with `spring.config.activate.on-profile`:

```yaml
spring:
  profiles:
    active: test

---
spring:
  config:
    activate:
      on-profile: test

logback:
  access:
    enabled: false
```

## See Also

- [Tomcat Integration](/guide/tomcat) — Tomcat-specific properties and reverse-proxy setup.
- [Jetty Integration](/guide/jetty) — Jetty-specific behavior and known limitations.
- [Advanced Topics](/guide/advanced) — TeeFilter, URL filtering, JSON logging, and Spring Security.
