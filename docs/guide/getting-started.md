# Getting Started

This guide explains how to add HTTP access logging to your Spring Boot application.

## Prerequisites

- Java 17 or later
- Spring Boot 4.0 or later
- Tomcat or Jetty embedded server

## Installation

Add the dependency to your build file:

::: code-group

```kotlin [Gradle (Kotlin)]
implementation("io.github.seijikohara:logback-access-spring-boot-starter:1.0.0")
```

```groovy [Gradle (Groovy)]
implementation 'io.github.seijikohara:logback-access-spring-boot-starter:1.0.0'
```

```xml [Maven]
<dependency>
    <groupId>io.github.seijikohara</groupId>
    <artifactId>logback-access-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

:::

## Basic Configuration

Create a `logback-access.xml` file in `src/main/resources`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%h %l %u %t "%r" %s %b</pattern>
        </encoder>
    </appender>
    <appender-ref ref="CONSOLE"/>
</configuration>
```

This configuration outputs access logs in the Common Log Format (CLF).

## Pattern Variables

The following pattern variables are available:

| Variable | Description |
|----------|-------------|
| `%h` | Remote host (IP address) |
| `%l` | Remote log name (always `-`) |
| `%u` | Remote user (from authentication) |
| `%t` | Request timestamp |
| `%r` | Request line (method, URI, protocol) |
| `%s` | HTTP status code |
| `%b` | Response body size in bytes |
| `%D` | Request processing time in milliseconds |
| `%T` | Request processing time in seconds |
| `%I` | Thread name |

## Combined Log Format

For a more detailed output similar to Apache's Combined Log Format:

```xml
<pattern>%h %l %u %t "%r" %s %b "%i{Referer}" "%i{User-Agent}"</pattern>
```

## Verify Installation

1. Start your Spring Boot application
2. Make an HTTP request to any endpoint
3. Check the console output for access log entries

Example output:

```
127.0.0.1 - - [01/Jan/2024:12:00:00 +0000] "GET /api/users HTTP/1.1" 200 1234
```

## Next Steps

- [Configuration Reference](/guide/configuration) - Learn about all configuration options
- [Tomcat Integration](/guide/tomcat) - Tomcat-specific settings
- [Jetty Integration](/guide/jetty) - Jetty-specific settings
- [Advanced Topics](/guide/advanced) - TeeFilter, URL filtering, and more
