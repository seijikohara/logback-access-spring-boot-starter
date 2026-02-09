# Advanced Topics

This page covers advanced features and configurations.

## TeeFilter

The TeeFilter captures request and response body content for logging.

::: tip Servlet Applications Only
TeeFilter requires a Servlet-based web application (Spring MVC). It is not available for reactive applications (Spring WebFlux).
:::

### Enable TeeFilter

```yaml
logback:
  access:
    tee-filter:
      enabled: true
      include-hosts: localhost,example.com
      exclude-hosts: internal.example.com
```

### Configuration Options

| Property | Description | Default |
|----------|-------------|---------|
| `enabled` | Enable or disable body capture | `false` |
| `include-hosts` | Comma-separated list of hosts to include | all hosts |
| `exclude-hosts` | Comma-separated list of hosts to exclude | none |
| `max-payload-size` | Maximum payload size (bytes) to log before suppression | `65536` |
| `allowed-content-types` | Content-Type patterns allowed for body capture (override mode) | see below |

### Accessing Body Content

Use the `%requestContent` and `%responseContent` patterns:

```xml
<pattern>%h "%r" %s %requestContent %responseContent</pattern>
```

### Body Capture Policy

Body content is evaluated against a capture policy before being included in log output. Binary content types and oversized payloads are automatically suppressed and replaced with sentinel values.

**Default allowed content types:**

- `text/*` (text/plain, text/html, etc.)
- `application/json`
- `application/xml`
- `application/*+json` (application/vnd.api+json, etc.)
- `application/*+xml` (application/atom+xml, etc.)
- `application/x-www-form-urlencoded`

**Sentinel values:**

| Condition | Sentinel |
|-----------|----------|
| Image content (`image/*`) | `[IMAGE CONTENTS SUPPRESSED]` |
| Other binary content | `[BINARY CONTENT SUPPRESSED]` |
| Payload exceeds `max-payload-size` | `[CONTENT TOO LARGE]` |

**Custom content types:**

```yaml
logback:
  access:
    tee-filter:
      enabled: true
      max-payload-size: 131072
      allowed-content-types:
        - "text/*"
        - "application/json"
        - "application/pdf"
```

When `allowed-content-types` is specified, it completely replaces the defaults (override mode).

::: warning
The `max-payload-size` setting only controls whether captured content appears in log output. TeeFilter still buffers the full body in memory regardless of this limit. Use host filtering to limit capture scope in production.
:::

::: info
When `tee-filter.enabled` is `false` (the default), `%requestContent` and `%responseContent` produce empty output. This includes form data (`application/x-www-form-urlencoded`) that would otherwise be reconstructed from request parameters. Body capture is completely disabled unless TeeFilter is explicitly enabled.
:::

### Character Encoding

Body content captured by TeeFilter is converted from bytes to text using the character encoding specified in the request or response `Content-Type` header. When no encoding is specified or the encoding is unsupported, UTF-8 is used as the fallback.

This means non-ASCII content (such as Shift_JIS or ISO-8859-1) is decoded correctly when the appropriate `Content-Type` charset is set by the client or server.

### Performance Considerations

::: warning
Body capture increases memory usage and may impact performance. Use host filtering to limit capture to specific environments.
:::

## URL Filtering

Control which URLs are logged using include and exclude patterns.

### Exclude Patterns

Exclude health check and actuator endpoints:

```yaml
logback:
  access:
    filter:
      exclude-url-patterns:
        - /actuator/.*
        - /health
        - /ready
        - /favicon.ico
```

### Include Patterns

Log only API endpoints:

```yaml
logback:
  access:
    filter:
      include-url-patterns:
        - /api/.*
```

### Pattern Evaluation Order

1. If include patterns are defined, the URL must match at least one
2. If exclude patterns are defined, matching URLs are excluded
3. Exclude takes precedence when both match

### Pattern Matching Behavior

Patterns use Java regular expressions with **partial matching**. A pattern matches if it is found anywhere within the request URI. Use anchors (`^`, `$`) for exact matching.

| Pattern | Matches | Does NOT Match |
|---------|---------|----------------|
| `/api/.*` | `/api/users`, `/v2/api/data` | `/apiary` |
| `/health` | `/health`, `/api/health-check` | `/heal` |
| `^/health$` | `/health` | `/api/health`, `/health/check` |
| `/users/[0-9]+` | `/users/123`, `/users/456` | `/users/abc` |
| `.*\\.json` | `/data.json`, `/api/config.json` | `/json-data` |

::: tip
To match an exact path, use anchored patterns. For example, `^/actuator/health$` matches only `/actuator/health`, not `/actuator/health/liveness`.
:::

## JSON Logging

Output access logs in JSON format for log aggregation systems.

### Using Logstash Encoder

Add the dependency:

::: code-group

```kotlin [Gradle (Kotlin)]
implementation("net.logstash.logback:logstash-logback-encoder:9.0")
```

```xml [Maven]
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>9.0</version>
</dependency>
```

:::

Configure the encoder:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <appender name="json" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashAccessEncoder"/>
    </appender>
    <appender-ref ref="json"/>
</configuration>
```

### Custom JSON Fields

Add custom fields to JSON output:

```xml
<encoder class="net.logstash.logback.encoder.LogstashAccessEncoder">
    <customFields>{"service":"my-app","environment":"production"}</customFields>
</encoder>
```

### Example Output

```json
{
  "@timestamp": "2026-01-01T12:00:00.000+09:00",
  "@version": "1",
  "message": "GET /api/users HTTP/1.1",
  "method": "GET",
  "protocol": "HTTP/1.1",
  "status_code": 200,
  "requested_url": "GET /api/users HTTP/1.1",
  "requested_uri": "/api/users",
  "remote_host": "192.168.1.100",
  "remote_user": "-",
  "content_length": 1234,
  "elapsed_time": 45,
  "service": "my-app",
  "environment": "production"
}
```

## Spring Security Integration

The starter captures authenticated usernames automatically when Spring Security is on the classpath.

::: tip Servlet Applications Only
Automatic username capture requires a Servlet-based web application (Spring MVC). For reactive applications (Spring WebFlux), access logging still works but the `%u` variable will show `-`.
:::

### How It Works

The starter checks the `SecurityContextHolder` for the authenticated principal:

1. Authenticated requests: The starter captures the username in `%u`
2. Anonymous requests: The `%u` variable shows `-`

Anonymous authentication tokens (such as `AnonymousAuthenticationToken`) are excluded using an `AuthenticationTrustResolver`. Only genuinely authenticated users appear in the access log.

### Customizing Trust Resolution

The starter provides a default `AuthenticationTrustResolver` bean (`AuthenticationTrustResolverImpl`). You can override it by defining your own bean:

```java
@Bean
public AuthenticationTrustResolver authenticationTrustResolver() {
    return new MyCustomTrustResolver();
}
```

### Custom Principal Extraction

The starter uses `SecurityContextHolder.getContext().getAuthentication().getName()` by default.

## Multiple Appenders

Send logs to multiple destinations:

```xml
<configuration>
    <appender name="console" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%h %l %u [%t] "%r" %s %b</pattern>
        </encoder>
    </appender>

    <appender name="file" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/access.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/access.%d{yyyy-MM-dd}.log</fileNamePattern>
        </rollingPolicy>
        <encoder>
            <pattern>%h %l %u [%t] "%r" %s %b</pattern>
        </encoder>
    </appender>

    <appender name="json" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashAccessEncoder"/>
    </appender>

    <appender-ref ref="console"/>
    <appender-ref ref="file"/>
    <appender-ref ref="json"/>
</configuration>
```

## Performance Tips

For optimal access logging performance:

1. Use `RollingFileAppender` with size limits for production file logging
2. Enable [URL filtering](#url-filtering) to reduce log volume
3. For JSON logging, [logstash-logback-encoder](https://github.com/logfellow/logstash-logback-encoder) provides its own async capabilities
4. Disable TeeFilter when body capture is not needed

## Troubleshooting

### Logs Not Appearing

1. Check that `logback.access.enabled` is `true`
2. Verify the configuration file exists and is valid XML
3. Check for typos in appender names

### Missing Username

1. Ensure Spring Security is on the classpath
2. Verify the user is actually authenticated
3. Check that the `%u` pattern is in your log format

### Performance Issues

1. Use async appenders for file logging
2. Enable URL filtering to reduce log volume
3. Disable body capture (TeeFilter) if not needed
4. Use rolling file appenders with size limits

## See Also

- [Tomcat Integration](/guide/tomcat) — Tomcat-specific properties and reverse proxy configuration
- [Jetty Integration](/guide/jetty) — Jetty-specific behavior and known limitations
- [Configuration Reference](/guide/configuration) — Full property reference and XML configuration
