# 設定

このページでは、logback-access-spring-boot-starterで使用可能なすべての設定オプションを説明します。

## アプリケーションプロパティ

`application.yml`または`application.properties`でSpring Bootプロパティを使用して設定します:

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
      # max-payload-size: 65536
      # allowed-content-types: text/*,application/json
    filter:
      # include-url-patterns:
      #   - /api/.*
      exclude-url-patterns:
        - /actuator/.*
        - /health
```

### プロパティリファレンス

| プロパティ | デフォルト | 説明 |
|-----------|----------|------|
| `logback.access.enabled` | `true` | アクセスロギングの有効/無効 |
| `logback.access.config-location` | 自動検出 | logback-access設定ファイルへのパス。`classpath:`および`file:` URLプレフィックスに対応 |
| `logback.access.local-port-strategy` | `server` | ポート解決戦略: `server`または`local` |
| `logback.access.tomcat.request-attributes-enabled` | `自動検出` | Tomcatリクエスト属性の有効化。未設定時、RemoteIpValveの存在から自動判定 |
| `logback.access.tee-filter.enabled` | `false` | リクエスト/レスポンスボディキャプチャの有効化 |
| `logback.access.tee-filter.include-hosts` | `null` | 含めるホストのカンマ区切りリスト |
| `logback.access.tee-filter.exclude-hosts` | `null` | 除外するホストのカンマ区切りリスト |
| `logback.access.tee-filter.max-payload-size` | `65536` | ログ出力する最大ペイロードサイズ（バイト） |
| `logback.access.tee-filter.allowed-content-types` | `null` | ボディキャプチャを許可するContent-Typeパターン（上書きモード） |
| `logback.access.filter.include-url-patterns` | `null` | 含めるURLパターン（正規表現） |
| `logback.access.filter.exclude-url-patterns` | `null` | 除外するURLパターン（正規表現） |

## 設定ファイルの解決

`logback.access.config-location`が設定されている場合、そのパスを直接使用します（フォールバックなし）。

未設定の場合、以下の順序で設定ファイルを検索します:

1. `classpath:logback-access-test.xml`（テスト用）
2. `classpath:logback-access.xml`
3. `classpath:logback-access-test-spring.xml`（Spring機能付きテスト用）
4. `classpath:logback-access-spring.xml`
5. 組み込みフォールバック設定

## XML設定

### 基本構造

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- Appenderはログの出力先を定義 -->
    <appender name="console" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%h %l %u [%t] "%r" %s %b</pattern>
        </encoder>
    </appender>

    <!-- Appenderを参照して有効化 -->
    <appender-ref ref="console"/>
</configuration>
```

### Springプロパティの使用

設定にSpringプロパティを注入:

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

::: warning デフォルトスコープ
`<springProperty>`のデフォルトスコープは`LOCAL`です。LOCALスコープのプロパティは、XML設定処理中の変数置換（`${varName}`など）でのみ使用可能です。`context.getProperty()`でプログラム的にアクセスするには、`scope="context"`を設定してください。
:::

### Springプロファイルの使用

環境別に異なるAppenderを設定:

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

### プロファイル式

Springプロファイル式は否定と複数プロファイルをサポート:

```xml
<!-- 本番環境以外でアクティブ -->
<springProfile name="!prod">
    ...
</springProfile>

<!-- devまたはstagingでアクティブ -->
<springProfile name="dev, staging">
    ...
</springProfile>
```

## File Appender

アクセスログをファイルに出力:

```xml
<appender name="file" class="ch.qos.logback.core.FileAppender">
    <file>logs/access.log</file>
    <encoder>
        <pattern>%h %l %u [%t] "%r" %s %b</pattern>
    </encoder>
</appender>
```

## Rolling File Appender

時間またはサイズに基づいてログをローテーション:

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

## アクセスロギングの無効化

プロパティを設定してアクセスロギングを無効化:

```yaml
logback:
  access:
    enabled: false
```

またはSpringプロファイルを使用:

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

## 関連ページ

- [Tomcat連携](/ja/guide/tomcat) — Tomcat固有のプロパティとリバースプロキシの設定
- [Jetty連携](/ja/guide/jetty) — Jetty固有の動作と既知の制限事項
- [高度な設定](/ja/guide/advanced) — TeeFilter、URLフィルタリング、JSONロギング、Spring Security
