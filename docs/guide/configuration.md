# Configuration

This page describes all available configuration options for logback-access-spring-boot-starter.

## Application Properties

Configure the starter using Spring Boot properties in `application.yml` or `application.properties`:

```yaml
logback:
  access:
    enabled: true
    config-location: classpath:logback-access.xml
    local-port-strategy: server
    tomcat:
      request-attributes-enabled: true
    tee-filter:
      enabled: false
      include-hosts: localhost
      exclude-hosts: ""
    filter:
      include-url-patterns: []
      exclude-url-patterns:
        - /actuator/.*
        - /health
```

### Property Reference

| Property | Default | Description |
|----------|---------|-------------|
| `logback.access.enabled` | `true` | Enable or disable access logging |
| `logback.access.config-location` | Auto-detected | Path to logback-access.xml configuration file |
| `logback.access.local-port-strategy` | `server` | Port resolution strategy: `server` or `local` |
| `logback.access.tomcat.request-attributes-enabled` | `true` | Enable Tomcat request attribute logging |
| `logback.access.tee-filter.enabled` | `false` | Enable request/response body capture |
| `logback.access.tee-filter.include-hosts` | `null` | Comma-separated list of hosts to include |
| `logback.access.tee-filter.exclude-hosts` | `null` | Comma-separated list of hosts to exclude |
| `logback.access.filter.include-url-patterns` | `null` | URL patterns to include (regex) |
| `logback.access.filter.exclude-url-patterns` | `null` | URL patterns to exclude (regex) |

## Configuration File Resolution

The starter searches for configuration files in the following order:

1. Path specified in `logback.access.config-location`
2. `classpath:logback-access-test.xml` (for testing)
3. `classpath:logback-access.xml`
4. `classpath:logback-access-test-spring.xml` (for testing with Spring features)
5. `classpath:logback-access-spring.xml`
6. Built-in fallback configuration

## XML Configuration

### Basic Structure

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- Appenders define where logs go -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%h %l %u %t "%r" %s %b</pattern>
        </encoder>
    </appender>

    <!-- Reference appenders to activate them -->
    <appender-ref ref="CONSOLE"/>
</configuration>
```

### Using Spring Properties

Inject Spring properties into your configuration:

```xml
<configuration>
    <springProperty name="appName" source="spring.application.name"
                    defaultValue="app" scope="context"/>

    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>[${appName}] %h %l %u %t "%r" %s %b</pattern>
        </encoder>
    </appender>
    <appender-ref ref="CONSOLE"/>
</configuration>
```

### Using Spring Profiles

Configure different appenders for different environments:

```xml
<configuration>
    <springProfile name="dev">
        <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder>
                <pattern>%h %l %u %t "%r" %s %b %D</pattern>
            </encoder>
        </appender>
        <appender-ref ref="CONSOLE"/>
    </springProfile>

    <springProfile name="prod">
        <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
            <file>logs/access.log</file>
            <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
                <fileNamePattern>logs/access.%d{yyyy-MM-dd}.log</fileNamePattern>
                <maxHistory>30</maxHistory>
            </rollingPolicy>
            <encoder>
                <pattern>%h %l %u %t "%r" %s %b</pattern>
            </encoder>
        </appender>
        <appender-ref ref="FILE"/>
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
<appender name="FILE" class="ch.qos.logback.core.FileAppender">
    <file>logs/access.log</file>
    <encoder>
        <pattern>%h %l %u %t "%r" %s %b</pattern>
    </encoder>
</appender>
```

## Rolling File Appender

Rotate logs based on time or size:

```xml
<appender name="ROLLING" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>logs/access.log</file>
    <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
        <fileNamePattern>logs/access.%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
        <maxFileSize>100MB</maxFileSize>
        <maxHistory>30</maxHistory>
        <totalSizeCap>3GB</totalSizeCap>
    </rollingPolicy>
    <encoder>
        <pattern>%h %l %u %t "%r" %s %b</pattern>
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
