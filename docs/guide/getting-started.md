# Getting Started

This guide walks through adding HTTP access logging to a Spring Boot application.

## Prerequisites

- Java 21 or later
- Spring Boot 4.0 or later
- Tomcat or Jetty as the embedded server

## Module Structure

The library is published as two Maven artifacts:

| Artifact | Description |
|----------|-------------|
| `logback-access-spring-boot-starter` | Auto-configuration and server integrations (Tomcat, Jetty, Spring Security, TeeFilter). |
| `logback-access-spring-boot-starter-core` | Public API and data models. Pulled in transitively — declare it separately only when you write extensions against the API. |

In typical use, declare only the starter dependency; the core module follows transitively.

## Installation

Add the dependency to your build file:

> Replace `VERSION` with the [latest version from Maven Central](https://central.sonatype.com/artifact/io.github.seijikohara/logback-access-spring-boot-starter).

::: code-group

```kotlin [Gradle (Kotlin)]
implementation("io.github.seijikohara:logback-access-spring-boot-starter:VERSION")
```

```groovy [Gradle (Groovy)]
implementation 'io.github.seijikohara:logback-access-spring-boot-starter:VERSION'
```

```xml [Maven]
<dependency>
    <groupId>io.github.seijikohara</groupId>
    <artifactId>logback-access-spring-boot-starter</artifactId>
    <version>VERSION</version>
</dependency>
```

:::

## Basic Configuration

Create `src/main/resources/logback-access.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <appender name="console" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%h %l %u [%t] "%r" %s %b</pattern>
        </encoder>
    </appender>
    <appender-ref ref="console"/>
</configuration>
```

This pattern matches the NCSA Common Log Format (CLF). The starter loads this file automatically — see [Configuration File Resolution](/guide/configuration#configuration-file-resolution) for the full lookup order.

## Pattern Variables

The following conversion words are available in the pattern:

| Variable | Description |
|----------|-------------|
| `%h` | Remote host. On Jetty, always an IP address (no reverse DNS lookup). |
| `%a` | Remote IP address. |
| `%A` | Local IP address. |
| `%p` | Local port. See [local-port-strategy](/guide/configuration#property-reference) to choose between the addressed port and the local interface port. |
| `%l` | Remote log name. Always `-`. |
| `%u` | Authenticated user name, or `-` when anonymous. Requires Spring Security on Servlet applications. |
| `%t` | Request timestamp. |
| `%r` | Request line: method, URI (with query string), protocol. |
| `%s` | HTTP status code. |
| `%b` | Response body size in bytes. |
| `%D` | Request processing time in milliseconds. |
| `%T` | Request processing time in seconds. |
| `%I` | Thread name that processed the request. |
| `%{name}i` | Value of request header `name`. |
| `%{name}o` | Value of response header `name`. |
| `%{name}c` | Value of cookie `name`. |
| `%{name}r` | Value of request attribute `name`. |
| `%queryString` | Query string with leading `?`, or empty when none. |
| `%requestContent` | Request body. Empty unless TeeFilter is enabled (Tomcat only). |
| `%responseContent` | Response body. Empty unless TeeFilter is enabled (Tomcat only). |

::: tip Alternative Syntax
For header, response header, cookie, and attribute conversion words, both the `%{name}i` and `%i{name}` forms are accepted. This documentation uses the `%{name}i` form throughout.
:::

## Combined Log Format

For Apache's Combined Log Format, append the `Referer` and `User-Agent` headers:

```xml
<pattern>%h %l %u [%t] "%r" %s %b "%{Referer}i" "%{User-Agent}i"</pattern>
```

## Verify the Installation

1. Start the Spring Boot application.
2. Issue an HTTP request to any endpoint.
3. Confirm that an access-log entry appears on the console.

Example output:

```
127.0.0.1 - - [01/Jan/2026:12:00:00 +0000] "GET /api/users HTTP/1.1" 200 1234
```

## Next Steps

- [Configuration Reference](/guide/configuration) — All application properties and XML configuration options.
- [Tomcat Integration](/guide/tomcat) — Tomcat-specific behavior and reverse-proxy setup.
- [Jetty Integration](/guide/jetty) — Jetty-specific behavior and known limitations.
- [Advanced Topics](/guide/advanced) — TeeFilter, URL filtering, JSON logging, and Spring Security.
