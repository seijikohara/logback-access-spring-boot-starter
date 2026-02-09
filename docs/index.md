---
layout: home
title: Home
description: Spring Boot 4 auto-configuration for Logback Access HTTP logging

hero:
  name: logback-access-spring-boot-starter
  text: HTTP Access Logging for Spring Boot
  tagline: Auto-configuration for Logback Access with Tomcat and Jetty support
  image:
    src: /logo.svg
    alt: logback-access-spring-boot-starter
  actions:
    - theme: brand
      text: Get Started
      link: /guide/getting-started
    - theme: alt
      text: View on GitHub
      link: https://github.com/seijikohara/logback-access-spring-boot-starter

features:
  - icon: ⚙️
    title: Auto-Configuration
    details: Zero-configuration setup for Tomcat and Jetty embedded servers. Add the dependency and start logging.
  - icon: 🔐
    title: Spring Security Integration
    details: Automatically captures authenticated usernames in access logs via Spring Security.
  - icon: 📝
    title: Request/Response Body Capture
    details: Optional TeeFilter support for logging request and response body content.
  - icon: 🎯
    title: URL Filtering
    details: Include/exclude URL patterns to control which requests are logged.
  - icon: 🌱
    title: Spring Profiles Support
    details: Environment-specific logging configuration using Spring profiles.
  - icon: 📊
    title: JSON Logging
    details: JSON output via logstash-logback-encoder, compatible with Logstash and ELK stack.
---

## Architecture

```mermaid
flowchart TB
    subgraph starter["logback-access-spring-boot-starter"]
        direction TB
        A[HTTP Request] --> B{Embedded Server}
        B -->|Tomcat| C[TomcatValve]
        B -->|Jetty| D[JettyRequestLog]
    end

    subgraph core["logback-access-spring-boot-starter-core"]
        E[LogbackAccessContext]
    end

    C --> E
    D --> E
    E --> F[logback-access.xml]
    F --> G[Appenders]
    G -->|Console| H[Console Output]
    G -->|File| I[File Output]
    G -->|JSON| J[Logstash/ELK]

    subgraph optional["Optional Integrations"]
        K[Spring Security] -.->|Username| E
        L[TeeFilter] -.->|Body Capture| E
    end
```

## Quick Start

Add the dependency to your project:

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

Start your application and access logs will appear in the console.

## Requirements

| Component | Version |
|-----------|---------|
| Java | 21+ |
| Spring Boot | 4.0+ |

## License

This project is licensed under the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0).
