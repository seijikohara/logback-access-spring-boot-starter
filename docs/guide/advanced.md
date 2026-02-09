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

| Property | Description |
|----------|-------------|
| `enabled` | Enable or disable body capture |
| `include-hosts` | Comma-separated list of hosts to include |
| `exclude-hosts` | Comma-separated list of hosts to exclude |

### Accessing Body Content

Use the `%requestContent` and `%responseContent` patterns:

```xml
<pattern>%h "%r" %s %requestContent %responseContent</pattern>
```

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

### Regular Expression Syntax

Patterns use Java regular expressions:

| Pattern | Matches |
|---------|---------|
| `/api/.*` | Any URL starting with `/api/` |
| `/users/[0-9]+` | `/users/123`, `/users/456` |
| `.*\\.json` | Any URL ending with `.json` |

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
  "@timestamp": "2026-01-01T12:00:00.000Z",
  "@version": 1,
  "method": "GET",
  "uri": "/api/users",
  "status": 200,
  "elapsed_time": 45,
  "remote_addr": "192.168.1.100",
  "user_agent": "Mozilla/5.0...",
  "service": "my-app",
  "environment": "production"
}
```

## Spring Security Integration

When Spring Security is on the classpath, authenticated usernames are automatically captured.

::: tip Servlet Applications Only
Automatic username capture requires a Servlet-based web application (Spring MVC). For reactive applications (Spring WebFlux), access logging still works but the `%u` variable will show `-`.
:::

### How It Works

The library checks the `SecurityContextHolder` for the authenticated principal:

1. Authenticated requests: Username is captured in `%u`
2. Anonymous requests: `-` is shown

### Custom Principal Extraction

The library uses `SecurityContextHolder.getContext().getAuthentication().getName()` by default.

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
