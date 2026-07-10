# Advanced Topics

This page covers TeeFilter body capture, URL filtering, JSON output, Spring Security integration, and troubleshooting.

## TeeFilter

TeeFilter is the Logback Access component that buffers request and response bodies so they can be referenced from the `%requestContent` and `%responseContent` pattern variables.

::: tip Tomcat Servlet Applications Only
The starter registers TeeFilter only when Tomcat is on the classpath and the application is Servlet-based (Spring MVC). It is not available on Jetty or on reactive applications (Spring WebFlux).
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
| `enabled` | Enable or disable body capture. | `false` |
| `include-hosts` | Comma-separated host names that activate the filter. | All hosts |
| `exclude-hosts` | Comma-separated host names that bypass the filter. | None |
| `max-payload-size` | Maximum payload size in bytes that appears in log output. Larger bodies are replaced with a sentinel. | `65536` |
| `allowed-content-types` | Content-Type patterns allowed for body capture. When set, this list completely replaces the built-in defaults. | See below |

::: tip Host matching
`include-hosts` / `exclude-hosts` are matched once at filter initialization against the server's own resolved local host name (not the request `Host` header), so they act as a global on/off switch rather than per-request filtering.
:::

### Accessing Body Content

Reference the captured bodies with the `%requestContent` and `%responseContent` pattern variables:

```xml
<pattern>%h "%r" %s %requestContent %responseContent</pattern>
```

### Body Capture Policy

Before captured bytes reach the log output, the starter evaluates a capture policy against the response's `Content-Type` and payload size. Binary content and oversized payloads are replaced with a sentinel. Empty payloads are never replaced with a sentinel.

**Default allowed content types:**

- `text/*` (text/plain, text/html, etc.)
- `application/json`
- `application/xml`
- `application/*+json` (application/vnd.api+json, etc.)
- `application/*+xml` (application/atom+xml, etc.)

`application/x-www-form-urlencoded` is deliberately not in the default list: login forms (for example Spring Security's `formLogin()`) post credentials with that content type. Add it to `allowed-content-types` explicitly when form bodies must be captured.

**Sentinel values:**

| Condition | Sentinel |
|-----------|----------|
| Image content (`image/*`) | `[IMAGE CONTENTS SUPPRESSED]` |
| Other binary or disallowed content | `[BINARY CONTENT SUPPRESSED]` |
| Missing `Content-Type` header (non-empty payload) | `[BINARY CONTENT SUPPRESSED]` |
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

Supplying `allowed-content-types` completely replaces the built-in list (override mode). Add every type that should be captured.

::: warning
`max-payload-size` controls only what reaches the log output. TeeFilter still buffers the full request and response bodies in memory regardless of this value. Restrict the capture scope with `include-hosts` / `exclude-hosts` in production.
:::

::: info
When `tee-filter.enabled` is `false` (the default), `%requestContent` and `%responseContent` always render as empty. This also suppresses the form-data reconstruction path for `application/x-www-form-urlencoded` requests. Even when TeeFilter is enabled, form bodies render as `[BINARY CONTENT SUPPRESSED]` unless `application/x-www-form-urlencoded` is explicitly added to `allowed-content-types`, so credentials submitted as form fields never leak into the access log by default.
:::

### Character Encoding

The starter decodes captured bytes using the charset declared in the `Content-Type` header. When the header omits a charset or specifies an unsupported one, the starter falls back to UTF-8.

Non-ASCII payloads (for example Shift_JIS or ISO-8859-1) decode correctly as long as the client or server sets the matching `charset` parameter on `Content-Type`.

### Performance Considerations

::: warning
Body capture buffers each request and response in memory. Limit the capture scope with `include-hosts` / `exclude-hosts` so the cost is bounded to the environments that truly need it.
:::

## URL Filtering

Choose which request URIs are logged using include and exclude patterns. Both lists accept Java regular expressions.

### Exclude Patterns

Drop health checks and the actuator from access logs:

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

1. When `include-url-patterns` is defined, the request URI must match at least one entry; otherwise the URI is dropped.
2. When `exclude-url-patterns` is defined, a matching URI is dropped.
3. If both lists are defined, exclude takes precedence — a URI is logged only when it matches an include pattern and matches no exclude pattern.

### Pattern Matching Behavior

Patterns use Java regular expressions with **partial matching**: a pattern matches if it is found anywhere in the request URI. Anchor with `^` and `$` for an exact match.

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

Emit access logs as JSON for downstream log-aggregation systems (Logstash, OpenSearch, etc.).

### Using the Logstash Encoder

Add `logstash-logback-encoder` as a dependency:

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

When Spring Security is on the classpath, the starter resolves the authenticated username from `SecurityContextHolder` and writes it to the `%u` log variable.

::: tip Servlet Applications Only
Username capture requires a Servlet-based web application (Spring MVC). On reactive applications (Spring WebFlux), access logging still operates but `%u` always renders as `-`.
:::

### How It Works

The starter registers an internal Servlet filter that runs after the Spring Security filter chain:

1. Authenticated request: the filter writes `Authentication.getName()` to a request attribute, and the access event sources copy it into the `%u` variable.
2. Anonymous request: no attribute is written and `%u` renders as `-`.

The filter consults an `AuthenticationTrustResolver` to distinguish anonymous tokens (such as `AnonymousAuthenticationToken`) from genuinely authenticated requests, so only authenticated users appear in the access log.

### Customizing Trust Resolution

The starter registers a default `AuthenticationTrustResolverImpl` bean when no other bean of type `AuthenticationTrustResolver` is present. Override it by declaring a bean of the same type:

```java
@Bean
public AuthenticationTrustResolver authenticationTrustResolver() {
    return new MyCustomTrustResolver();
}
```

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

- Use `RollingFileAppender` with size and history limits for production file logging.
- Enable [URL filtering](#url-filtering) to drop high-volume, low-value endpoints (health checks, metrics).
- When JSON output is required, `logstash-logback-encoder` provides its own asynchronous appenders.
- Leave TeeFilter disabled unless body content is actually needed in the log.

## Troubleshooting

### Logs Not Appearing

1. Confirm that `logback.access.enabled` is `true`.
2. Verify the configuration file exists on the classpath and is valid XML.
3. Check appender names in `<appender-ref>` for typos.

### Missing Username

1. Confirm Spring Security is on the classpath.
2. Verify the request is authenticated (and not an anonymous token).
3. Confirm the pattern contains `%u`.
4. Confirm the application is Servlet-based — reactive applications always render `%u` as `-`.

### Performance Issues

1. Wrap file appenders with `AsyncAppender` to decouple I/O from request threads.
2. Restrict the log volume with [URL filtering](#url-filtering).
3. Disable TeeFilter when body capture is not needed.
4. Apply size and history limits on `RollingFileAppender`.

## See Also

- [Tomcat Integration](/guide/tomcat) — Tomcat-specific properties and reverse-proxy setup.
- [Jetty Integration](/guide/jetty) — Jetty-specific behavior and known limitations.
- [Configuration Reference](/guide/configuration) — Full property reference and XML configuration.
