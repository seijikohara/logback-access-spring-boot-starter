# logback-access-spring-boot-starter

[![maven central badge]][maven central]
[![javadoc badge]][javadoc]
[![release badge]][release]
[![build badge]][build]
[![license badge]][license]

[maven central]: https://central.sonatype.com/artifact/io.github.seijikohara/logback-access-spring-boot-starter
[maven central badge]: https://img.shields.io/maven-central/v/io.github.seijikohara/logback-access-spring-boot-starter
[javadoc]: https://javadoc.io/doc/io.github.seijikohara/logback-access-spring-boot-starter
[javadoc badge]: https://javadoc.io/badge2/io.github.seijikohara/logback-access-spring-boot-starter/javadoc.svg
[release]: https://github.com/seijikohara/logback-access-spring-boot-starter/releases
[release badge]: https://img.shields.io/github/v/release/seijikohara/logback-access-spring-boot-starter?color=brightgreen&sort=semver
[build]: https://github.com/seijikohara/logback-access-spring-boot-starter/actions/workflows/build.yml
[build badge]: https://github.com/seijikohara/logback-access-spring-boot-starter/actions/workflows/build.yml/badge.svg
[license]: LICENSE.txt
[license badge]: https://img.shields.io/github/license/seijikohara/logback-access-spring-boot-starter?color=blue

[Spring Boot] Starter for [Logback-access].

> **Note**: This is a fork of [akkinoc/logback-access-spring-boot-starter](https://github.com/akkinoc/logback-access-spring-boot-starter) with Spring Boot 4.0 support.

[Spring Boot]: https://spring.io/projects/spring-boot
[Logback-access]: https://logback.qos.ch/access.html

## Quick Start

1. Add the dependency to your project
2. Create a `logback-access.xml` configuration file in the classpath
3. Start your application - access logging works automatically

```xml
<!-- logback-access.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <appender name="console" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>common</pattern>
        </encoder>
    </appender>
    <appender-ref ref="console"/>
</configuration>
```

## Features

* **Zero-configuration setup** - Auto-detects configuration files and auto-configures Logback-access
* **Classpath configuration** - Supports configuration files on the classpath (not just the filesystem)
* **Spring Boot extensions** - Provides `<springProfile>` and `<springProperty>` tags for configuration files
* **Forward header support** - Rewrites attributes using HTTP forward headers ("X-Forwarded-*")
* **Spring Security integration** - Provides remote user from Spring Security authentication
* **Distributed tracing** - Integrates with Micrometer Tracing for trace/span ID logging
* **Request/response body logging** - Configurable tee filter for capturing request and response bodies
* **Flexible exclusion patterns** - Exclude specific URI patterns from access logging

### Server Support Matrix

|          | Servlet Stack | Reactive Stack |
|:--------:|:-------------:|:--------------:|
|  Tomcat  |       ✅      |       ✅       |
|  Jetty   |       ✅      |       ✅       |
|  Netty   |       -       |   ✅ (Beta)    |

## Requirements

* Java 17 or later
* Kotlin 2.2 or later
* Spring Boot 4.0
* Logback-access 2.0

## Installation

### Maven

Add the starter dependency:

```xml
<dependency>
    <groupId>io.github.seijikohara</groupId>
    <artifactId>logback-access-spring-boot-starter</artifactId>
    <version>${logback-access-spring-boot-starter.version}</version>
</dependency>
```

### Server-Specific Dependencies

#### Tomcat

```xml
<dependency>
    <groupId>ch.qos.logback.access</groupId>
    <artifactId>logback-access-tomcat</artifactId>
    <version>${logback-access.version}</version>
</dependency>
```

#### Jetty

```xml
<dependency>
    <groupId>ch.qos.logback.access</groupId>
    <artifactId>logback-access-jetty12</artifactId>
    <version>${logback-access.version}</version>
</dependency>
```

#### Netty (Reactive Stack)

No additional dependencies required. Netty support is built into the starter.

## Configuration

### Configuration File

Create a `logback-access.xml` (or `logback-access-spring.xml`) file in the root of the classpath.

#### Auto-detection Priority

Configuration files are searched in the following order:

1. `classpath:logback-access-test.xml`
2. `classpath:logback-access.xml`
3. `classpath:logback-access-test-spring.xml`
4. `classpath:logback-access-spring.xml`
5. Built-in fallback (outputs to console with common pattern)

> **Tip**: Place `logback-access-test.xml` in `src/test/resources` for test-specific configuration. Maven ensures it won't be included in production artifacts.

#### Example Configuration

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- Console appender with combined pattern -->
    <appender name="console" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>combined</pattern>
        </encoder>
    </appender>

    <!-- File appender with custom pattern -->
    <appender name="file" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/access.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/access.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%h %l %u [%t] "%r" %s %b "%i{Referer}" "%i{User-Agent}" %D</pattern>
        </encoder>
    </appender>

    <appender-ref ref="console"/>
    <appender-ref ref="file"/>
</configuration>
```

### Configuration Properties

Configure via `application.yml` or `application.properties`:

```yaml
logback.access:
  # Enable/disable auto-configuration (default: true)
  enabled: true

  # Custom configuration file location
  # Supports "classpath:" and "file:" prefixes
  config: classpath:custom-logback-access.xml

  # Local port strategy: "local" or "server" (default: server)
  # - local: Returns the port on which the request was received
  # - server: Returns the port to which the request was sent
  local-port-strategy: server

  # Exclude patterns from access logging
  exclude-patterns:
    - /actuator/**
    - /health
    - /favicon.ico

  # Only exclude successful (2xx) responses (default: false)
  exclude-successful-only: false

  # Tomcat-specific settings
  tomcat:
    # Enable request attributes for RemoteIpValve (auto-detected)
    request-attributes-enabled: true

  # Tee filter for body logging
  tee-filter:
    enabled: false
    includes: localhost,dev-*
    excludes: prod-*
```

### Exclusion Patterns

Exclude specific URI patterns from access logging using Ant-style path patterns:

```yaml
logback.access:
  exclude-patterns:
    - /actuator/**      # All actuator endpoints
    - /health           # Health check endpoint
    - /static/**        # Static resources
    - /**/favicon.ico   # Favicon requests
```

Set `exclude-successful-only: true` to only exclude requests that return 2xx status codes (useful for logging errors even on excluded paths):

```yaml
logback.access:
  exclude-patterns:
    - /health
  exclude-successful-only: true  # Log errors on /health endpoint
```

### Request and Response Body Logging

Enable the tee filter to capture request and response bodies:

```yaml
logback.access:
  tee-filter:
    enabled: true
```

Then use the `%requestContent` and `%responseContent` patterns:

```xml
<pattern>%h %l %u [%t] "%r" %s %b [Request: %requestContent] [Response: %responseContent]</pattern>
```

#### Body Size Limits

By default, the tee filter captures up to 64KB of request and response bodies. Configure limits to prevent memory exhaustion with large payloads:

```yaml
logback.access:
  tee-filter:
    enabled: true
    max-request-body-size: 64KB   # Default: 64KB
    max-response-body-size: 128KB # Default: 64KB
```

Supported size formats: `64KB`, `1MB`, `1024B`, `1024` (bytes)

When a body exceeds the limit, it will be truncated and `... [TRUNCATED]` will be appended.

#### Body Capture Limitations

|                          | Servlet Stack (TeeFilter) | Reactive Stack |
|:-------------------------|:-------------------------:|:--------------:|
| `application/json`       |             ✅            |       ❌       |
| `text/plain`             |             ✅            |       ❌       |
| `application/xml`        |             ✅            |       ❌       |
| `application/x-www-form-urlencoded` |    ❌ (※)       |       ❌       |
| `multipart/form-data`    |             ❌            |       ❌       |

※ Form data cannot be captured because the Servlet container consumes the request body for parameter parsing. Use `%requestParameter{name}` instead.

> **Warning**: The tee filter buffers bodies in memory before truncation. Size limits reduce log storage but do not prevent initial memory allocation. Enable only in development or testing environments.

### Distributed Tracing Integration

When [Micrometer Tracing](https://micrometer.io/docs/tracing) is on the classpath and configured, trace context is automatically available as request attributes.

#### Setup

Add Micrometer Tracing to your project (e.g., with Zipkin):

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>
<dependency>
    <groupId>io.zipkin.reporter2</groupId>
    <artifactId>zipkin-reporter-brave</artifactId>
</dependency>
```

#### Usage

Use request attributes in your logback-access configuration:

```xml
<pattern>%h %l %u [%t] "%r" %s %b traceId=%reqAttribute{traceId} spanId=%reqAttribute{spanId}</pattern>
```

Available attributes:
- `%reqAttribute{traceId}` - The trace ID
- `%reqAttribute{spanId}` - The span ID
- `%reqAttribute{parentId}` - The parent span ID (if available)

> **Note**: For Reactive stack (Netty), tracing integration requires proper observation setup. Ensure Micrometer Observation is configured with your tracing backend (e.g., via Spring Boot Actuator's tracing autoconfiguration).

### Spring Boot Extensions

#### Profile-specific Configuration

Use `<springProfile>` to conditionally include configuration based on active Spring profiles:

```xml
<springProfile name="development">
    <appender name="console" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%h %l %u [%t] "%r" %s %b %D</pattern>
        </encoder>
    </appender>
    <appender-ref ref="console"/>
</springProfile>

<springProfile name="production">
    <appender name="file" class="ch.qos.logback.core.FileAppender">
        <file>/var/log/access.log</file>
        <encoder>
            <pattern>combined</pattern>
        </encoder>
    </appender>
    <appender-ref ref="file"/>
</springProfile>
```

Profile expressions:
- `name="staging"` - Active when "staging" profile is active
- `name="dev | staging"` - Active when "dev" OR "staging" is active
- `name="!production"` - Active when "production" is NOT active

#### Environment Properties

Use `<springProperty>` to expose Spring Environment properties:

```xml
<springProperty scope="context" name="appName" source="spring.application.name" defaultValue="app"/>
<springProperty scope="context" name="logPath" source="logging.file.path" defaultValue="/var/log"/>

<appender name="file" class="ch.qos.logback.core.FileAppender">
    <file>${logPath}/${appName}-access.log</file>
    <encoder>
        <pattern>combined</pattern>
    </encoder>
</appender>
```

## Server-Specific Notes

### Tomcat

Tomcat provides full feature support including:
- Request attributes for `RemoteIpValve` integration
- Forward header processing with `server.forward-headers-strategy=native`
- Full tee filter support for body logging

### Jetty

Jetty provides full feature support similar to Tomcat:
- Forward header processing
- Full tee filter support for body logging

### Netty (Beta)

Netty support for WebFlux reactive applications is available but with some limitations:

| Feature                  | Status      |
|:-------------------------|:-----------:|
| Basic access logging     | ✅          |
| Request/response headers | ✅          |
| Query parameters         | ✅          |
| Cookies                  | ✅          |
| Status code              | ✅          |
| Content length           | ✅          |
| Remote address           | ✅          |
| Protocol detection       | ✅ (HTTP/1.1, HTTP/2) |
| Exchange attributes      | ✅          |
| Tracing integration      | ✅ (requires observation setup) |
| `remoteUser`             | ❌ (always null) |
| `sessionID`              | ❌ (always null) |
| Request body capture     | ❌          |
| Response body capture    | ❌          |

> **Note**: These limitations are due to fundamental differences between Servlet and Reactive APIs. WebFlux does not have direct equivalents for servlet sessions or request attributes.

## Troubleshooting

### Request/Response Body Not Logged

**Problem**: `%requestContent` or `%responseContent` shows empty or "-"

**Solutions**:
1. Enable the tee filter:
   ```yaml
   logback.access:
     tee-filter:
       enabled: true
   ```
2. Check content type - form data (`application/x-www-form-urlencoded`) cannot be captured
3. For reactive stack (Netty), body capture is not supported

### Exclude Patterns Not Working

**Problem**: Requests matching exclude patterns are still logged

**Solutions**:
1. Verify Ant-style pattern syntax:
   - `*` matches zero or more characters within a path segment
   - `**` matches zero or more path segments
   - `?` matches exactly one character
2. Examples:
   - `/actuator/**` matches `/actuator/health`, `/actuator/info`, etc.
   - `/api/*/users` matches `/api/v1/users`, `/api/v2/users`, etc.

### Trace ID Not Appearing

**Problem**: `%reqAttribute{traceId}` shows empty or "-"

**Solutions**:
1. Verify Micrometer Tracing is on the classpath and configured
2. Ensure a `Tracer` bean is available in the application context
3. Check that requests are being traced (sampling may exclude some requests)
4. Note: Tracing is only available for Servlet stack, not Reactive

### Configuration File Not Found

**Problem**: Using fallback configuration instead of custom file

**Solutions**:
1. Check file location is on the classpath root
2. Verify file name matches expected patterns
3. Use explicit configuration:
   ```yaml
   logback.access:
     config: classpath:my-logback-access.xml
   ```

### Forward Headers Not Applied

**Problem**: Remote IP shows proxy address instead of client IP

**Solutions**:
1. For Tomcat, ensure `server.forward-headers-strategy=native` is set
2. Verify `X-Forwarded-For` header is being sent by your proxy
3. Check `tomcat.request-attributes-enabled` is true (auto-detected by default)

## API Reference

Please refer to the [Javadoc][javadoc].

## Release Notes

Please refer to the [Releases][release] page.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

Licensed under the [Apache License, Version 2.0][license].

## Acknowledgments

This project is a fork of [akkinoc/logback-access-spring-boot-starter](https://github.com/akkinoc/logback-access-spring-boot-starter).
Thanks to the original author for creating this useful library.
