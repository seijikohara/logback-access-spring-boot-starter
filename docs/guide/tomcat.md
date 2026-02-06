# Tomcat Integration

This page describes Tomcat-specific configuration options and behavior.

## How It Works

When using Tomcat as the embedded server, the starter registers a `LogbackAccessTomcatValve` that intercepts all HTTP requests and responses.

```
HTTP Request → Tomcat Connector → LogbackAccessTomcatValve → Your Application
                                          ↓
                                   LogbackAccessContext
                                          ↓
                                   Appenders (Console, File, etc.)
```

## Tomcat-Specific Properties

```yaml
logback:
  access:
    tomcat:
      request-attributes-enabled: true
```

### Request Attributes

When `request-attributes-enabled` is `true`, the following Tomcat request attributes are available:

| Attribute | Description |
|-----------|-------------|
| `org.apache.catalina.AccessLog.RemoteAddr` | Client IP address |
| `org.apache.catalina.AccessLog.RemoteHost` | Client hostname |
| `org.apache.catalina.AccessLog.Protocol` | HTTP protocol version |
| `org.apache.catalina.AccessLog.ServerPort` | Server port |
| `org.apache.tomcat.remoteAddr` | Client IP address (alternative) |

These attributes are useful when behind a reverse proxy.

## Pattern Variables

Tomcat supports all standard pattern variables plus:

| Variable | Description |
|----------|-------------|
| `%a` | Remote IP address |
| `%A` | Local IP address |
| `%p` | Local port |
| `%{xxx}i` | Request header `xxx` |
| `%{xxx}o` | Response header `xxx` |
| `%{xxx}c` | Cookie value `xxx` |
| `%{xxx}r` | Request attribute `xxx` |

## Behind a Reverse Proxy

When running behind a proxy (nginx, Apache, load balancer), configure the `RemoteIpValve` to get the real client IP:

```yaml
server:
  tomcat:
    remoteip:
      remote-ip-header: X-Forwarded-For
      protocol-header: X-Forwarded-Proto
```

The access log will then show the real client IP instead of the proxy's IP.

## Local Port Strategy

Control which port is logged:

```yaml
logback:
  access:
    local-port-strategy: server  # or 'local'
```

- `server`: Use the server port (e.g., 8080)
- `local`: Use the local connection port

## Spring Security Integration

When Spring Security is on the classpath, authenticated usernames are automatically captured:

```xml
<pattern>%h %l %u %t "%r" %s %b</pattern>
```

The `%u` variable will show:
- The authenticated username for authenticated requests
- `-` for anonymous requests

## Example Configuration

Complete example for a production Tomcat setup:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <springProperty name="appName" source="spring.application.name"
                    defaultValue="app" scope="context"/>

    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/access.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/access.%d{yyyy-MM-dd}.log.gz</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%h %l %u %t "%r" %s %b "%i{Referer}" "%i{User-Agent}" %D</pattern>
        </encoder>
    </appender>

    <appender-ref ref="FILE"/>
</configuration>
```

Application properties:

```yaml
logback:
  access:
    tomcat:
      request-attributes-enabled: true
    filter:
      exclude-url-patterns:
        - /actuator/.*
        - /health
        - /favicon.ico
```
