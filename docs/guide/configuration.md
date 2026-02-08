# Configuration

This page describes all available configuration options for logback-access-spring-boot-starter.

## Application Properties

Configure the starter using Spring Boot properties in `application.yml` or `application.properties`:

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
      # include-hosts: localhost
      # exclude-hosts: internal.example.com
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
| `logback.access.enabled` | `true` | Enable or disable access logging |
| `logback.access.config-location` | Auto-detected | Path to logback-access configuration file. Supports `classpath:` and `file:` URL prefixes |
| `logback.access.local-port-strategy` | `server` | Port resolution strategy: `server` or `local` |
| `logback.access.tomcat.request-attributes-enabled` | `Auto-detected` | Enable Tomcat request attributes. Auto-detected from RemoteIpValve when not set |
| `logback.access.tee-filter.enabled` | `false` | Enable request/response body capture |
| `logback.access.tee-filter.include-hosts` | `null` | Comma-separated list of hosts to include |
| `logback.access.tee-filter.exclude-hosts` | `null` | Comma-separated list of hosts to exclude |
| `logback.access.filter.include-url-patterns` | `null` | URL patterns to include (regex) |
| `logback.access.filter.exclude-url-patterns` | `null` | URL patterns to exclude (regex) |

## Configuration File Resolution

When `logback.access.config-location` is set, that path is used directly (no fallback).

When not set, the starter searches in the following order:

1. `classpath:logback-access-test.xml` (for testing)
2. `classpath:logback-access.xml`
3. `classpath:logback-access-test-spring.xml` (for testing with Spring features)
4. `classpath:logback-access-spring.xml`
5. Built-in fallback configuration

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

Inject Spring properties into your configuration:

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

### Using Spring Profiles

Configure different appenders for different environments:

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

Spring profile expressions support negation and multiple profiles:

```xml
<!-- Active when NOT in production -->
<springProfile name="!prod">
    ...
</springProfile>

<!-- Active in dev OR staging -->
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

Set the property to disable access logging:

```yaml
logback:
  access:
    enabled: false
```

Or use a Spring profile:

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
