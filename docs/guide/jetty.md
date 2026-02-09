# Jetty Integration

This page describes Jetty-specific configuration options and behavior.

## How It Works

When using Jetty as the embedded server, the starter registers a `JettyRequestLog` that captures HTTP request and response data.

```
HTTP Request → Jetty Server → JettyRequestLog → Your Application
                                          ↓
                                   LogbackAccessContext
                                          ↓
                                   Appenders (Console, File, etc.)
```

## Using Jetty

To use Jetty instead of Tomcat, exclude Tomcat and include Jetty:

::: code-group

```kotlin [Gradle (Kotlin)]
implementation("org.springframework.boot:spring-boot-starter-webmvc") {
    exclude(group = "org.springframework.boot", module = "spring-boot-starter-tomcat")
}
implementation("org.springframework.boot:spring-boot-starter-jetty")
implementation("io.github.seijikohara:logback-access-spring-boot-starter:VERSION")
```

```groovy [Gradle (Groovy)]
implementation('org.springframework.boot:spring-boot-starter-webmvc') {
    exclude group: 'org.springframework.boot', module: 'spring-boot-starter-tomcat'
}
implementation 'org.springframework.boot:spring-boot-starter-jetty'
implementation 'io.github.seijikohara:logback-access-spring-boot-starter:VERSION'
```

```xml [Maven]
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc</artifactId>
    <exclusions>
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-tomcat</artifactId>
        </exclusion>
    </exclusions>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jetty</artifactId>
</dependency>
<dependency>
    <groupId>io.github.seijikohara</groupId>
    <artifactId>logback-access-spring-boot-starter</artifactId>
    <version>VERSION</version>
</dependency>
```

:::

## Jetty 12 Compatibility

This library is compatible with Jetty 12 (the version bundled with Spring Boot 4).

## Pattern Variables

For standard pattern variables, see [Getting Started — Pattern Variables](/guide/getting-started#pattern-variables).

Jetty-specific notes:

- **Cookies** (`%{xxx}c`): The starter extracts cookies using `Request.getCookies()` from the Jetty native API.
- **Request attributes** (`%{xxx}r`): Standard servlet request attributes are available, but Tomcat-specific `AccessLog` attributes (e.g., `org.apache.catalina.AccessLog.RemoteAddr`) are not supported.
- **Remote host** (`%h`): Always returns the IP address (no reverse DNS lookup is performed).
- **Request parameters**: `requestParameterMap` returns an empty map to avoid consuming the request body.

## Known Limitations

### Remote Host Resolution

Jetty does not perform reverse DNS lookups by default. The `%h` variable will show the IP address, not the hostname. This is intentional for performance reasons.

### Request Parameters

For performance and compatibility reasons, `requestParameterMap` returns an empty map for all requests. This is intentional to avoid consuming the request body.

### TeeFilter

::: warning Not Supported on Jetty 12
TeeFilter is not supported on Jetty 12. The Jetty RequestLog API operates at the core server level, separate from the Servlet API. TeeFilter sets request attributes on the Servlet request, but these attributes are not visible to the RequestLog. See [Advanced Topics — TeeFilter](/guide/advanced#teefilter) for details on TeeFilter usage with Tomcat.
:::

## Local Port Strategy

Control which port is logged:

```yaml
logback:
  access:
    local-port-strategy: server
```

- `server`: Use the configured server port
- `local`: Use the local connection port

## Behind a Reverse Proxy

Configure Jetty to handle forwarded headers:

```yaml
server:
  forward-headers-strategy: native
```

Or for more control:

```yaml
server:
  forward-headers-strategy: framework
```

## Spring Security Integration

The starter captures authenticated usernames automatically in the `%u` variable when Spring Security is on the classpath.

## Example Configuration

Complete example for a production Jetty setup:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <appender name="file" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/access.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/access.%d{yyyy-MM-dd}.log.gz</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%h %l %u [%t] "%r" %s %b "%i{Referer}" "%i{User-Agent}" %D</pattern>
        </encoder>
    </appender>

    <appender-ref ref="file"/>
</configuration>
```

Application properties:

```yaml
logback:
  access:
    filter:
      exclude-url-patterns:
        - /actuator/.*
        - /health
```

## See Also

- [Configuration Reference](/guide/configuration) — Full property reference and XML configuration
- [Advanced Topics](/guide/advanced) — TeeFilter, URL filtering, JSON logging, and Spring Security
